package org.everest.ro.enricher.core;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class EnrichmentInput implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String ro_uri;	
	private String callback;
	private String nonce;
	
	public EnrichmentInput() {}
	
    public EnrichmentInput (String ro_uri) {
	      this.ro_uri = ro_uri;	        
	}

	public String getRo_uri() {
		return ro_uri;
	}

	public void setRo_uri(String ro_uri) {
		this.ro_uri = ro_uri;
	}		

	public String getCallback() {
		return callback;
	}

	public void setCallback(String callback) {
		this.callback = callback;
	}

	public String getNonce() {
		return nonce;
	}

	public void setNonce(String nonce) {
		this.nonce = nonce;
	}

	@Override
	public String toString() {
		return "EnrichmentInput [ro_uri=" + ro_uri + ", calback=" + callback + ", nonce=" + nonce + "]";
	}

	
	
	

	    

    
	
}
