package net.expertsystem.everest.enricher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class RO_Harvester {
	
	private static final Logger logger = LogManager.getLogger("RO_Harvester");
	
	public static String sparql(ResearchObject ro) throws FileNotFoundException {
		String sparqlEndpoint = "http://sandbox.rohub.org/rodl/sparql";
		String uri_author = "http://xmlns.com/foaf/0.1/name";
		String creator = "http://purl.org/dc/terms/creator";
		String sparqlQuery = "" +
				"CONSTRUCT {\n "
				+ "<" + ro.getId() +"> ?p ?o .\n "
				+ "<" + ro.getId() +"> <" + uri_author + "> ?personName . }\n"
				+ "WHERE {\n"
				+ "{<" + ro.getId() + "> ?p ?o }\n"
				+ "union {<" + ro.getId() + "> <" + uri_author + "> ?person . ?person <" + uri_author + "> ?personName .} }";
		Query query = QueryFactory.create(sparqlQuery, Syntax.syntaxSPARQL) ;
		logger.info("query: {}", query);
		QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint,query); 
		Model results = qexec.execConstruct();

		String title = "http://purl.org/dc/terms/title";
		String description = "http://purl.org/dc/terms/description";
		String created = "http://purl.org/dc/terms/created";
		String archiveof = "http://purl.org/wf4ever/roevo#isArchiveOf";
		String snapshotof = "http://purl.org/wf4ever/roevo#isSnapshotOf";
		
		String title_res="";
		String description_res="";
		String creator_res="";
		String created_res="";
		String author_res="";
		String source_ro=ro.getId();


		// list the statements in the Model
		StmtIterator iter = results.listStatements();

		// print out the predicate, subject and object of each statement
		while (iter.hasNext()) {
			Statement stmt      = iter.nextStatement();  // get next statement
			RDFNode   object    = stmt.getObject();      // get the object
			Property  predicate = stmt.getPredicate();   // get the predicate

			if (!(object instanceof Resource)) {
				if (predicate.toString().equals(title)){
					title_res = object.toString();
				}
				if (predicate.toString().equals(description)){
					description_res = object.toString();
				}
				if (predicate.toString().equals(uri_author)){
					author_res = object.toString();
				}
				if (predicate.toString().equals(created)){
					created_res = object.toString();
				}
			}
			if (predicate.toString().equals(creator)){
				if(!object.isLiteral()){
					creator_res = object.asResource().getURI();
				}
			}
			if (predicate.toString().equals(archiveof)||predicate.toString().equals(snapshotof)){
				if(!object.isLiteral()){
					source_ro = object.toString();
				}
			}
		}
		ro.setTitle(title_res);
		ro.setDescription(description_res);
		ro.setCreator(creator_res);
		ro.setCreated(created_res);
		ro.setAuthor(author_res);
		ro.setSource_ro(source_ro);
		String request = "";
		if (title_res != ""){
			request = request + title_res + " ";
		}
		if (description_res != ""){
			request = request + description_res + " ";
		}
		
		qexec.close();
		return request;
	}
}
