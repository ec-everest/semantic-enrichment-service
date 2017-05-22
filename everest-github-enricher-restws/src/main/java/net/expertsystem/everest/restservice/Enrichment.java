package net.expertsystem.everest.restservice;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.everest.ro.enricher.core.EnricherProperties;
import org.everest.ro.enricher.core.EnrichmentInput;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

@Path("/enrichment")
public class Enrichment {
	
	 static final Logger logger = LogManager.getLogger(Enrichment.class.getName());
	 
	 private final static String QUEUE_NAME = "request";
	/**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getIt() {
        return "Got it!";
    }
      
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response enrich(final EnrichmentInput enrichmentInput) throws Exception {
    	logger.debug("POST enrichments/ received");
    	if ( (enrichmentInput==null) || (enrichmentInput.getRo_uri()==null || enrichmentInput.getRo_uri().isEmpty()) ||
    		 (enrichmentInput.getCallback()==null || enrichmentInput.getCallback().isEmpty()) ||
    		 (enrichmentInput.getNonce()==null || enrichmentInput.getNonce().isEmpty()) )
    		return Response.status(400).entity("json document expected: {\"ro_uri\":\"http://..\". \"callback\":\"http://...\", \"nonce\":\"http://...\"}").build();
    	
    	logger.debug("RO to Enrich {}", enrichmentInput);
    	byte[] data = SerializationUtils.serialize(enrichmentInput);
    	
    	String rabbitHost=EnricherProperties.getInstance().getRabbitHost();
    	logger.info("rabbit host: {}",rabbitHost);
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(rabbitHost);
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();		
		boolean durable = true;		
	    channel.queueDeclare(QUEUE_NAME, durable, false, false, null);
	    Map<String, Object> messageHeaders = new HashMap<String, Object>();
    	messageHeaders.put("x-redelivered-count", new Integer(0));
    	messageHeaders.put("callback",enrichmentInput.getCallback());
    	messageHeaders.put("nonce",enrichmentInput.getNonce());
    	messageHeaders.put("ro",enrichmentInput.getRo_uri());
		channel.basicPublish("", QUEUE_NAME,  new AMQP.BasicProperties.Builder()
												  	 .contentType("text/plain")
												     .deliveryMode(2) // persistent
									                 .priority(0)
									                 .headers(messageHeaders)		
									                 .correlationId(enrichmentInput.getRo_uri())									                 
									                 .build(),
									                data);        
		logger.info("[x] Enrichment request sent to queue {} ", enrichmentInput.getRo_uri() );
		
		channel.close();
		connection.close();
    	return Response.status(200).entity(enrichmentInput.getRo_uri()).build();
    }
}
