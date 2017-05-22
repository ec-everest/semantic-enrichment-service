package org.everest.ro.enricher.core;

import java.io.Serializable;

public class EnrichmentOutput implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	String enrichmentRDF;
	String ro_uri;
	String callback;
	String nonce;	
	
	public String getEnrichmentRDF() {
		return enrichmentRDF;
	}
	public void setEnrichmentRDF(String enrichmentRDF) {
		this.enrichmentRDF = enrichmentRDF;
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
		return "EnrichmentResponse [enrichmentRDF=" + enrichmentRDF + ", ro_uri=" + ro_uri + ", callback=" + callback
				+ ", nonce=" + nonce + "]";
	}
	
	
}
