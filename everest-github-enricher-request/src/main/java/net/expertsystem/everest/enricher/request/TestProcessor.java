package net.expertsystem.everest.enricher.request;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.xmlbeans.XmlException;

import net.expertsystem.everest.enricher.mainclass;
import net.expertsystem.lab.sensigrafo.SensigrafoDAO;

public class TestProcessor {
	
	public static void main(String[] args) throws Exception{
		SensigrafoDAO sensei_en = new SensigrafoDAO(System.getenv("SENSIGRAFO"));
		SensigrafoDAO sensei_it = new SensigrafoDAO(System.getenv("SENSIGRAFO_ITA"));
		SensigrafoDAO sensei_es = new SensigrafoDAO(System.getenv("SENSIGRAFO_SPA"));
		String url = "http://sandbox.rohub.org/rodl/ROs/HD_chromatin_analysis/";
		url="http://sandbox.rohub.org/rodl/ROs/Reports/";
		//String url = "http://sandbox.rohub.org/rodl/ROs/delta-22/";
		System.out.println(mainclass.enricher(url,sensei_en,sensei_it,sensei_es));
	}
	
}
