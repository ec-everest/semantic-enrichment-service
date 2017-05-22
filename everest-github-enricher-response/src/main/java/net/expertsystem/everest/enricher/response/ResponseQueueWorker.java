package net.expertsystem.everest.enricher.response;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.everest.ro.enricher.core.EnricherProperties;
import org.everest.ro.enricher.core.EnrichmentOutput;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.logging.LoggingFeature;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;


public class ResponseQueueWorker {
	
	private final static String QUEUE_NAME_RESP = "response";
	private final static String QUEUE_NAME_RESP_FAILED = "response-failed";
	static boolean durable = true;
	private final static Logger logger = LogManager.getLogger(ResponseQueueWorker.class.getName());
	
	public static void main(String[] argv) throws Exception {
		System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");	
		String rabbitHost=EnricherProperties.getInstance().getRabbitHost();
		final ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost(rabbitHost);
	    Connection connection = factory.newConnection();
	    final Channel channelResp = connection.createChannel();
	    
	    channelResp.queueDeclare(QUEUE_NAME_RESP, durable, false, false, null);	    	   
    	
	    logger.info(" [+] Waiting for messages in RESPONSE queue. To exit press CTRL+C");
	    boolean autoAck = false; //
	    channelResp.basicQos(1);  //// accept only one unack-ed message at a time
	    channelResp.basicConsume(QUEUE_NAME_RESP, autoAck, "ResponseQueueWorker", new DefaultConsumer(channelResp) {
	    	  @Override
	    	  public void handleDelivery(String consumerTag, Envelope envelope, 
	    			  					 AMQP.BasicProperties properties, byte[] body) throws IOException {
	    		
	    	    
	    		String roUri=properties.getCorrelationId();
	    		logger.info(" [x] sending annotations of ro {} ",roUri );	    			    		
	    		EnrichmentOutput enrichmentOutput =  (EnrichmentOutput)SerializationUtils.deserialize(body);	    		
	    	    	    	    	
	    	    //logger.debug(" sending Response content {} ",enrichmentOutput);
	    	    
	    	    try{	    	    	    	    	   	   
	    	    	ClientConfig clientConfig = new ClientConfig();
	    	    	clientConfig.property(LoggingFeature.LOGGING_FEATURE_VERBOSITY_CLIENT, LoggingFeature.Verbosity.PAYLOAD_ANY);	    	    	
	    	    	Client client = ClientBuilder.newClient(clientConfig);
	    	    	
	    	    	WebTarget webTarget = client.target(enrichmentOutput.getCallback()).queryParam("nonce", enrichmentOutput.getNonce());	    	    	    	 
	    	    	Invocation.Builder invocationBuilder =  webTarget.request(MediaType.TEXT_PLAIN);
	    	    	invocationBuilder.header("Authorization", "Bearer ZW5yaWNobWVudDpIODZjMzV3eWdDRHZ1Zw==");	    	   
	    	    	Response response = invocationBuilder.post(Entity.entity(enrichmentOutput.getEnrichmentRDF(), "text/plain"));
	    	    	
	    	    	//If the callback failed then:
	    	    	if ((response.getStatus()!=Response.Status.OK.getStatusCode() && 
	    	    		 response.getStatus()!=Response.Status.ACCEPTED.getStatusCode())	){	    	    	
		    	    	logger.info("callback failed with {}", response.getStatus());
		    	    	throw new Exception("callback failed");
	    	    	}
	    	    } catch (Exception e) {
	    	    	logger.error("Error enriching the research object:"+enrichmentOutput.getRo_uri()+" ", e);	 
	    	    	//Get the message redelivered count
	    	    	logger.error("Error processing the response for the research object:"+enrichmentOutput.getRo_uri()+" ", e);
	    	    	Integer reDeliveryCount=(Integer)properties.getHeaders().get("x-redelivered-count");
	    	    	properties.getHeaders().put("callback",enrichmentOutput.getCallback());
	    			properties.getHeaders().put("nonce",enrichmentOutput.getNonce());
	    			properties.getHeaders().put("ro",enrichmentOutput.getRo_uri());	 
	    	    	//See if the message has been shoveled from the response-failed queue
	    	    	if (reDeliveryCount.intValue() <5){ 	    	    	
	    	    		logger.info(" requeuing the message for {} ",enrichmentOutput.getRo_uri());	    	    		
	    	    		reDeliveryCount=new Integer(reDeliveryCount.intValue()+1);
	    	    		properties.getHeaders().put("x-redelivered-count",reDeliveryCount);	  
	    	    		requeueResponseRequest(channelResp,properties,enrichmentOutput.getRo_uri(), body);
		    	    		
	    	    	}
	    	    	else{ //move the message to the failed messages.
	    	    		logger.info("message moved to the failed queue {} ",enrichmentOutput.getRo_uri());
	    	    		sendToFailedResponseQueue(properties,enrichmentOutput.getRo_uri(),body);	    	    		  	   
	    	    	}	    	    					
				}finally{
					channelResp.basicAck(envelope.getDeliveryTag(), false);
				}
	    	 }
	    	} );	    
	}
	
	protected static void sendToFailedResponseQueue(BasicProperties properties, String roUri, byte[] body) throws IOException {
		Connection connection=null; 
		try{
			String rabbitHost=EnricherProperties.getInstance().getRabbitHost();
			ConnectionFactory factory = new ConnectionFactory();
		    factory.setHost(rabbitHost);			
    		connection = factory.newConnection();
    		final Channel channelRespFailed = connection.createChannel();
    	    boolean durable = true;
    	    properties.getHeaders().put("x-redelivered-count",0);
    	    channelRespFailed.queueDeclare(QUEUE_NAME_RESP_FAILED, durable, false, false, null);
    	    channelRespFailed.basicPublish("", QUEUE_NAME_RESP_FAILED, new AMQP.BasicProperties.Builder()
	                 .contentType("text/plain")
	                 .deliveryMode(2) // persistent
	                 .priority(0)
	                 .headers(properties.getHeaders())
	                 .correlationId(roUri)												     
	                 .build(),
	                  body);
    		} catch (Exception e2) {
    			throw new IOException(e2);
    	    }
    	   finally {
				if (connection!=null)
					connection.close();					
			} 
	}

	private static void requeueResponseRequest(Channel channelResp, BasicProperties properties, String roUri, byte[] body) 
			throws IOException {
		// TODO Auto-generated method stub
		try{
		channelResp.queueDeclare(QUEUE_NAME_RESP, durable, false, false, null);    	    		
    	channelResp.basicPublish("", QUEUE_NAME_RESP, new AMQP.BasicProperties.Builder()
									                 .contentType("text/plain")
									                 .deliveryMode(2) // persistent
									                 .priority(0)
									                 .headers(properties.getHeaders())
									                 .correlationId(roUri)												     
									                 .build(),
									                  body);
		logger.debug("[x] Response sent to queue for ro {} ", roUri);
		}
		catch (Exception e1) {
			throw new IOException(e1);
	    }	
	}
}
