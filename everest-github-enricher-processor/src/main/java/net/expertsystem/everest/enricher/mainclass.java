package net.expertsystem.everest.enricher;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.apache.logging.log4j.LogManager;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.xmlbeans.XmlException;

import net.expertsystem.gslnsdk.GSLRequester;
import net.expertsystem.lab.sensigrafo.SensigrafoDAO;
import net.expertsystem.sdk.CogitoLanguage;


public class mainclass {
	public static void main(String[] args) throws Exception{
		SensigrafoDAO sensei_en = new SensigrafoDAO(System.getenv("SENSIGRAFO"));
		SensigrafoDAO sensei_it = new SensigrafoDAO(System.getenv("SENSIGRAFO_ITA"));
		SensigrafoDAO sensei_es = new SensigrafoDAO(System.getenv("SENSIGRAFO_SPA"));
		
		List <String> list = new ArrayList<String>();
		String url = "http://sandbox.rohub.org/rodl/ROs/cnr-The%2Bimplementation%2Bof%2Bthe%2BEur-2/";
		System.out.println(enricher(url, sensei_en, sensei_it, sensei_es).getEnrichment_ann());
	}
	
	private static final Logger logger = LogManager.getLogger("mainclass");
	
	public static ResearchObject enricher(String uri, SensigrafoDAO sensei_en, SensigrafoDAO sensei_it, SensigrafoDAO sensei_es) throws Exception{		
		String request = "";		//text to analyze in cogito. 
		ResearchObject ro = new ResearchObject (uri);
		
		long startTime = System.currentTimeMillis();
		logger.info("\n\nDESCARGA DE METADATA");
		request = request + RO_Harvester.sparql(ro);
		long endTime = System.currentTimeMillis();
		long duration = (endTime - startTime);
		logger.debug("DESCARGA DEL RESEARCH OBJECT Y METADATA FINALIZADA EN " + duration/1000 + " segundos");
		
		startTime = System.currentTimeMillis();
		logger.info("\n\nANALISIS DE RDF's Y EXTRACCION DE ARCHIVOS SENSIBLES PARA COGITO");
		List<String>list_files = Request_Generator.sensible_files(ro);
		endTime = System.currentTimeMillis();
		duration = (endTime - startTime);
		logger.debug("ANALISIS DE RDF's Y EXTRACCION DE ARCHIVOS SENSIBLES PARA COGITO FINALIZADO EN " + duration/1000 + " segundos");
		
		startTime = System.currentTimeMillis();
		logger.info("\n\nCONVERSION DE DOCUMENTOS A STRING");
		request = request + Request_Generator.text_extractor(list_files,ro);
		endTime = System.currentTimeMillis();
		duration = (endTime - startTime);
		logger.debug("CONVERSION DE DOCUMENTOS A STRING FINALIZADA EN " + duration/1000 + " segundos");

		startTime = System.currentTimeMillis();
		logger.info("\n\nDETECT TEXT LANGUAGE");
		endTime = System.currentTimeMillis();
		String lang = LanguageDetector.detect(request);
		duration = (endTime - startTime);
		logger.info("LANG {} DETECTED IN {} sg ", lang,duration/1000);

		startTime = System.currentTimeMillis();
		logger.info("\n\nANALISIS EN COGITO");
	    Cogito_Analyzing.analyzeRO(request,ro,lang,sensei_en,sensei_it,sensei_es);
		endTime = System.currentTimeMillis();
		duration = (endTime - startTime);
		logger.debug("ANALISIS EN COGITO FINALIZADO EN " + duration/1000 + " segundos");
	    
		startTime = System.currentTimeMillis();
		logger.debug("\n\nGENERACION DE ANOTACIONES");
	    if (!request.isEmpty())
	    	ro.setEnrichment_ann(annotation_generator(ro)); 
		endTime = System.currentTimeMillis();
		duration = (endTime - startTime);
		logger.debug("GENERACION DE ANOTACIONES FINALIZADA EN " + duration/1000 + " segundos");
		
	    return ro;
		}
	
		public static String annotation_generator(ResearchObject ro){
			List <Resource> resourceList = new ArrayList<Resource>();
			String res = "";
				String cdesc= "https://w3id.org/contentdesc#";
				String prefixNewResource="subject/";

				String DomainClass= cdesc + "Domain";
				String ConceptClass= cdesc + "Concept";
				String ExpressionClass= cdesc + "Expression";
				String PersonClass = cdesc + "Person";
				String OrganizationClass = cdesc + "Organization";
				String PlaceClass = cdesc + "Place";

				Model model = ModelFactory.createDefaultModel();

				//String skos = "http://www.w3.org/2004/02/skos/core#";
				//model.setNsPrefix("skos", skos);
				model.setNsPrefix("skos", SKOS.getURI());
				model.setNsPrefix("foaf", FOAF.getURI());
				model.setNsPrefix("dc", DCTerms.getURI());
				model.setNsPrefix("cdesc", cdesc);


				for (String domain:ro.getDomains()){
					resourceList.add(model.createResource(prefixNewResource+domain.hashCode()).addProperty(RDF.type, DomainClass).addProperty(SKOS.prefLabel, domain));
				}
				for (String concept:ro.getConcepts()){
					resourceList.add(model.createResource(prefixNewResource+concept.hashCode()).addProperty(RDF.type, ConceptClass).addProperty(SKOS.prefLabel, concept));
				}
				for (String expression:ro.getExpressions()){
					resourceList.add(model.createResource(prefixNewResource+expression.hashCode()).addProperty(RDF.type, ExpressionClass).addProperty(SKOS.prefLabel, expression));
				}
				for (String person:ro.getPeople()){
					resourceList.add(model.createResource(prefixNewResource+person.hashCode()).addProperty(RDF.type, PersonClass).addProperty(FOAF.name, person));
				}
				for (String organization:ro.getOrg()){
					resourceList.add(model.createResource(prefixNewResource+organization.hashCode()).addProperty(RDF.type, OrganizationClass).addProperty(FOAF.name, organization));
				}
				for (String place:ro.getPlaces()){
					resourceList.add(model.createResource(prefixNewResource+place.hashCode()).addProperty(RDF.type, PlaceClass).addProperty(SKOS.prefLabel, place));
				}

				if (!resourceList.isEmpty()){
					for (Resource resource:resourceList){
						model.createResource(ro.getId()).addProperty(DCTerms.subject, resource);
					}
				}
				StringWriter out = new StringWriter();
				
				
				model.write(out,"TURTLE");
				res = out.toString();
			if (model.isEmpty())
				return "";
			else
				return res;
		}
		
		public static String roName(ResearchObject ro){
			String url = ro.getId();
			String res = url.split("/")[url.split("/").length-1];
			return res;
		}
	}

