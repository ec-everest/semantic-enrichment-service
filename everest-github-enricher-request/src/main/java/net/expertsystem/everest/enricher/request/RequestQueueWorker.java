package net.expertsystem.everest.enricher.request;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import net.expertsystem.everest.enricher.ResearchObject;
import net.expertsystem.lab.sensigrafo.SensigrafoDAO;

import org.apache.logging.log4j.Logger;
import org.everest.ro.enricher.core.EnricherProperties;
import org.everest.ro.enricher.core.EnrichmentInput;
import org.everest.ro.enricher.core.EnrichmentOutput;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;

public class RequestQueueWorker {
	
	final static Logger logger = LogManager.getLogger(RequestQueueWorker.class.getName());
			 
	private final static String QUEUE_NAME_RESP = "response";	
	private final static String QUEUE_NAME_REQ = "request";
	private final static String QUEUE_NAME_REQ_FAILED = "request-failed";
	private final static String QUEUE_NAME_TO_INDEX = "to-index";
	static SensigrafoDAO sensei_en = new SensigrafoDAO(System.getenv("SENSIGRAFO"));
	static SensigrafoDAO sensei_it = new SensigrafoDAO(System.getenv("SENSIGRAFO_ITA"));
	static SensigrafoDAO sensei_es = new SensigrafoDAO(System.getenv("SENSIGRAFO_SPA"));
	final static boolean durable = true;
	
	
	public static void main(String[] argv) throws Exception {
		String rabbitHost=EnricherProperties.getInstance().getRabbitHost();
		ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost(rabbitHost);	    	   
	    ExecutorService es = Executors.newFixedThreadPool(1);
	    Connection connection = factory.newConnection(es);
	    final Channel channelReq = connection.createChannel();	    
	    channelReq.queueDeclare(QUEUE_NAME_REQ, durable, false, false, null);	    	       	
	    logger.info(" [*] Waiting for messages. To exit press CTRL+C");
	    boolean autoAck = false; 
	    channelReq.basicQos(1); //// accept only one unack-ed message at a time
	    channelReq.basicConsume(QUEUE_NAME_REQ, autoAck, "myConsumerTag", new DefaultConsumer(channelReq){
	    	  @Override
	    	  public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
	    			  throws IOException {
	    	    //String roUri = new String(body, "UTF-8");
	    		EnrichmentInput enrichmentRequest =  (EnrichmentInput)SerializationUtils.deserialize(body);
	    	    logger.info(" [x] Received {} ", enrichmentRequest );	    	    
	    	    try{
	    	    	ResearchObject ro=net.expertsystem.everest.enricher.mainclass.enricher(enrichmentRequest.getRo_uri(),sensei_en,sensei_it,sensei_es);
	    	    	String enrichmentRDF=ro.getEnrichment_ann();	    	    	
	    	    	EnrichmentOutput output= new EnrichmentOutput();
	    	    	output.setEnrichmentRDF(enrichmentRDF);
	    	    	output.setRo_uri(enrichmentRequest.getRo_uri());
	    	    	output.setCallback(enrichmentRequest.getCallback());
	    	    	output.setNonce(enrichmentRequest.getNonce());
	    	    	byte[] data = SerializationUtils.serialize(output);
	    	    		    	    	    	    		    	    	
	    	    	if (!enrichmentRDF.isEmpty()){
	    	    		sendToResponseQueue(enrichmentRequest, output, data);	    	    		
		    	    	//enqueue the rdf message in the response queue
	    	    		sendToIndexEnrichmentQueue(ro);		    	    	
	    	    	}
	    	    	else{
	    	    		logger.info("Enrichment for ro {} is empty. END OF processing", enrichmentRequest.getRo_uri());
	    	    	}
	    			
	    	    	logger.debug("Enrichment for ro {} is {}", enrichmentRequest.getRo_uri(), enrichmentRDF);	    	    				    	    
	    	    	
	    	    } catch (Exception e) {
	    	    	logger.error("Error enriching the research object:"+enrichmentRequest.getRo_uri()+" ", e);	    	    	
	    	    	Integer reDeliveryCount=(Integer)properties.getHeaders().get("x-redelivered-count");
	    	    	properties.getHeaders().put("callback",enrichmentRequest.getCallback());
	    			properties.getHeaders().put("nonce",enrichmentRequest.getNonce());
	    			properties.getHeaders().put("ro",enrichmentRequest.getRo_uri());	 
	    			//if the message has failed less than 5 times requeue to try again 
	    	    	if (reDeliveryCount.intValue() <0){ 	    	    	
	    	    		logger.info("requeuing the message for {} ",enrichmentRequest.getRo_uri());
	    	    		try{	    	    			
	    	    			reDeliveryCount=new Integer(reDeliveryCount.intValue()+1);	    	    			
	    	    			properties.getHeaders().put("x-redelivered-count",reDeliveryCount);	    	    			   	    			 
	    	    			requeueEnrichmentRequest(channelReq, properties, enrichmentRequest, body);   
			    			logger.debug("[x] Response re-sent to queue request for ro {} ", enrichmentRequest.getRo_uri());
	    	    		}
		    	    	catch (Exception e1) {
	    	    			throw new IOException(e1);
	    	    	    }	
	    	    	}
	    	    	else{ //move the message to the failed messages.
	    	    		logger.info("message moved to the request-failed queue {} ",enrichmentRequest.getRo_uri());	    
	    	    		try{
	    	    			sendToFailedRequestQueue(properties, enrichmentRequest, body);
	    	    		}
	    	    		catch (IOException e2) {
	    	    			throw e2;
	    	    	    }
	    	    	}	    	    	    	    	
				} finally {		
					channelReq.basicAck(envelope.getDeliveryTag(), false);
				}	    	  
	    	  }
	    } );
	}

		private static void sendToIndexEnrichmentQueue(ResearchObject ro) throws IOException, TimeoutException {
			Connection connectionResp=null;
		    Channel channelResp;
		    try{
		    	byte[] data = SerializationUtils.serialize(ro); 
		    	String rabbitHost=EnricherProperties.getInstance().getRabbitHost();
				ConnectionFactory factoryResp = new ConnectionFactory();
			    factoryResp.setHost(rabbitHost);
		    	connectionResp = factoryResp.newConnection();
		    	channelResp = connectionResp.createChannel();		    	
		    	Map<String, Object> messageHeaders = new HashMap<String, Object>();		    			    			    	
		    	messageHeaders.put("ro",ro.getId());
		    	channelResp.queueDeclare(QUEUE_NAME_TO_INDEX, durable, false, false, null);
		    		
		    	channelResp.basicPublish("", QUEUE_NAME_TO_INDEX, new AMQP.BasicProperties.Builder()
											                 .contentType("text/plain")
											                 .deliveryMode(2) // persistent
											                 .priority(0)
											                 .headers(messageHeaders)											                 												    
											                 .build(),
											                  data);
				logger.info("[x] Response sent to queue for INDEXING ro {} ", ro.getId());
		    }
		    finally {
				if (connectionResp!=null)
					connectionResp.close();	
		    }	  		
		}	    

			private static void sendToFailedRequestQueue(BasicProperties properties, EnrichmentInput enrichmentRequest,
					byte[] body) throws IOException {
					Connection connection=null;
				try{
					String rabbitHost=EnricherProperties.getInstance().getRabbitHost();
					ConnectionFactory factory = new ConnectionFactory();
				    factory.setHost(rabbitHost);
					connection = factory.newConnection();
		    		final Channel channelRespFailed = connection.createChannel();
		    	    boolean durable = true;
		    	    properties.getHeaders().put("x-redelivered-count",0);		    	    	    
		    	    channelRespFailed.queueDeclare(QUEUE_NAME_REQ_FAILED, durable, false, false, null);
		    	    channelRespFailed.basicPublish("", QUEUE_NAME_REQ_FAILED, new AMQP.BasicProperties.Builder()
			                 .contentType("text/plain")
			                 .deliveryMode(2) // persistent
			                 .priority(0)
			                 .headers(properties.getHeaders())
			                 .correlationId(enrichmentRequest.getRo_uri())												     
			                 .build(),
			                  body);
				}catch (Exception e2) {		    	    			
	    			throw new IOException(e2);								
	    	    }
	    	   finally {
					if (connection!=null)
						connection.close();					
				}   	    
				
			}

			private static void requeueEnrichmentRequest(Channel channelReq, BasicProperties properties, EnrichmentInput enrichmentRequest, byte[] body) throws IOException {
				channelReq.queueDeclare(QUEUE_NAME_REQ, durable, false, false, null);    	    		
    			channelReq.basicPublish("", QUEUE_NAME_REQ,  new AMQP.BasicProperties.Builder()
					  	 .contentType("text/plain")
					     .deliveryMode(2) // persistent
		                 .priority(0)
		                 .headers(properties.getHeaders())
		                 .correlationId(enrichmentRequest.getRo_uri())	
		                 .build(),
		                body);     
			}

			private static void sendToResponseQueue(EnrichmentInput enrichmentTequest, EnrichmentOutput output, byte[] data) 
					throws IOException, TimeoutException {
				Connection connectionResp=null;
	    	    Channel channelResp;
	    	    try{
	    	    	String rabbitHost=EnricherProperties.getInstance().getRabbitHost();
					ConnectionFactory factoryResp = new ConnectionFactory();
	    		    factoryResp.setHost(rabbitHost);
	    	    	connectionResp = factoryResp.newConnection();
	    	    	channelResp = connectionResp.createChannel();		    	
	    	    	Map<String, Object> messageHeaders = new HashMap<String, Object>();
	    	    	messageHeaders.put("x-redelivered-count", new Integer(0));
	    	    	messageHeaders.put("callback",enrichmentTequest.getCallback());
	    	    	messageHeaders.put("nonce",enrichmentTequest.getNonce());
	    	    	messageHeaders.put("ro",enrichmentTequest.getRo_uri());
	    	    	channelResp.queueDeclare(QUEUE_NAME_RESP, durable, false, false, null);
	    	    		
	    	    	//channelResp.basicPublish("", QUEUE_NAME_RESP, null, enrichmentInput.getRo_uri().getBytes("UTF-8"));
	    	    	channelResp.basicPublish("", QUEUE_NAME_RESP, new AMQP.BasicProperties.Builder()
												                 .contentType("text/plain")
												                 .deliveryMode(2) // persistent
												                 .priority(0)
												                 .headers(messageHeaders)
												                 .correlationId(enrichmentTequest.getRo_uri())												     
												                 .build(),
												                  data);
	    			logger.info("[x] Response sent to queue for ro {} ", enrichmentTequest.getRo_uri());
	    	    }
	    	    finally {
					if (connectionResp!=null)
						connectionResp.close();	
	    	    }	   	    	 		    	   
			}
}
