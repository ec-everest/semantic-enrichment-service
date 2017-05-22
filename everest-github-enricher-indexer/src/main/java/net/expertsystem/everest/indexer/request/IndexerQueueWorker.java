package net.expertsystem.everest.indexer.request;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
import org.everest.ro.enricher.core.*;

import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.common.SolrInputDocument;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;

public class IndexerQueueWorker {
	
	final static Logger logger = LogManager.getLogger(IndexerQueueWorker.class.getName());
			 

	private final static String QUEUE_NAME_TO_INDEX = "to-index";
	private final static String QUEUE_NAME_TO_INDEX_FAILED = "to-index-failed";
	final static boolean durable = true;

	public static void main(String[] argv) throws Exception {
		String rabbitHost=EnricherProperties.getInstance().getRabbitHost();
		ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost(rabbitHost);
		Connection connection = factory.newConnection();
		final Channel channelReq = connection.createChannel();	    
		channelReq.queueDeclare(QUEUE_NAME_TO_INDEX, durable, false, false, null);	    	       	
		logger.info(" [*] Waiting for messages. To exit press CTRL+C");
		boolean autoAck = false; 
		channelReq.basicQos(1); //// accept only one unack-ed message at a time
		channelReq.basicConsume(QUEUE_NAME_TO_INDEX, autoAck, "myConsumerTag", new DefaultConsumer(channelReq){
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
					throws IOException {
				//String roUri = new String(body, "UTF-8");
				logger.info("Entering handle delivery");
				ResearchObject ro =  (ResearchObject)SerializationUtils.deserialize(body);
				logger.info(" [x] Received {} ", ro);
				try {
					roToIndex(ro);
				} catch (SolrServerException e) {
					// TODO Auto-generated catch block
					logger.fatal("Error indexing the research object.", e);	    	    	     	    	
	    	    	logger.info("message moved to the request-failed queue {} ",ro.getId());	    	    			    	    		
	    	    	sendToFailedRequestQueue(properties, ro, body);	    	    					    	    				    	    	     	    	    	    	
				} finally {		
					channelReq.basicAck(envelope.getDeliveryTag(), false);
			}}});
	}
	private static void roToIndex(ResearchObject ro) throws SolrServerException, IOException{
		logger.info("solr document for {}",ro.getId());
		String urlString = EnricherProperties.getInstance().getSolrURL();
		SolrClient solr = new HttpSolrClient.Builder(urlString).build();
		SolrInputDocument document = new SolrInputDocument();
		((HttpSolrClient) solr).setParser(new XMLResponseParser());
		String content = "";
		document.addField("id", ro.getId().trim());
		document.addField("title", ro.getTitle());
		document.addField("autocomplete", ro.getTitle());
		content = content + ro.getTitle() + " ";
		document.addField("description", ro.getDescription());
		content = content + ro.getDescription() + " ";
		document.addField("creator", ro.getCreator());
		document.addField("author", ro.getAuthor());
		content = content + ro.getAuthor() + " ";
		document.addField("autocomplete", ro.getAuthor());
		document.addField("created", dateParser(ro.getCreated()));
		document.addField("source_ro", ro.getSource_ro());
		if (ro.getSketch() != ""){
			document.addField("sketch", ro.getSketch());
		}
		if (!ro.getPeople().isEmpty()){
			for(String people: ro.getPeople()){
				document.addField("people", people);
				document.addField("autocomplete",people);
				content = content + people + " ";
			}
		}
		if (!ro.getOrg().isEmpty()){
			for(String org: ro.getOrg()){
				document.addField("organization", org);
				document.addField("autocomplete",org);
				content = content + org + " ";
			}
		}
		if (!ro.getPlaces().isEmpty()){
			for(String place: ro.getPlaces()){
				document.addField("place", place);
				document.addField("autocomplete",place);
				content = content + place + " ";
			}
		}
		if (!ro.getExpressions().isEmpty()){
			for(String exp: ro.getExpressions()){
				document.addField("compound terms", exp);
				document.addField("autocomplete",exp);
				content = content + exp + " ";
			}
		}
		if (!ro.getConcepts().isEmpty()){
			for(String concept: ro.getConcepts()){
				document.addField("concepts", concept);
				document.addField("autocomplete",concept);
				content = content + concept + " ";
			}
		}
		if (!ro.getDomains().isEmpty()){
			for(String domain: ro.getDomains()){
				document.addField("domains", domain);
				document.addField("autocomplete",domain);
				content = content + domain + " ";
			}
		}
		document.addField("content", content);

		solr.add(document);

		// Remember to commit your changes!

		solr.commit();		

		logger.info("document added to solr");
	}
	public static String dateParser (String date_without_parsing){
		String aux = date_without_parsing.replaceAll("Z\\^\\^http://www.w3.org/2001/XMLSchema#dateTime", "");
		String date = aux.replaceAll("T", " ");
		return date;
	}
	private static void sendToFailedRequestQueue(BasicProperties properties, ResearchObject ro, byte[] body) throws IOException {
		Connection connection=null;
		try{
			String rabbitHost=EnricherProperties.getInstance().getRabbitHost();
			ConnectionFactory factory = new ConnectionFactory();
		    factory.setHost(rabbitHost);
			connection = factory.newConnection();
    		final Channel channelRespFailed = connection.createChannel();
    	    boolean durable = true;	    	    	    
    	    channelRespFailed.queueDeclare(QUEUE_NAME_TO_INDEX_FAILED, durable, false, false, null);
    	    channelRespFailed.basicPublish("", QUEUE_NAME_TO_INDEX_FAILED, new AMQP.BasicProperties.Builder()
	                 .contentType("text/plain")
	                 .deliveryMode(2) // persistent
	                 .priority(0)
	                 .headers(properties.getHeaders())
	                 .correlationId(ro.getId())												     
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
}
