package org.everest.ro.enricher.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EnricherProperties {
	
	private static EnricherProperties instance = null;
	private Properties prop;
	static final Logger logger = LogManager.getLogger(EnricherProperties.class.getName());
	
	protected EnricherProperties() throws IOException{		
		prop = new Properties();
		InputStream input = null;
		try {
			input = getClass().getClassLoader().getResourceAsStream("config.properties");			 
			prop.load(input);
			prop.forEach((k,v)->logger.info("Key: {} value: {} ",k ,v));			
		}catch (IOException ex) {
			throw ex;
		} finally {
			if (input != null) 				
				input.close();
			
		}
	}

	public static EnricherProperties getInstance() throws IOException {
	      if(instance == null) {
	         instance = new EnricherProperties();
	      }
	      return instance;
	}
	
	public String getRabbitHost(){
		return prop.getProperty("rabbitHost");
	}

	public String getSolrURL() {
		return prop.getProperty("SolrURL");
	}

	
}
