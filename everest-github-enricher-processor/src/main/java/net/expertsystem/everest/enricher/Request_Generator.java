package net.expertsystem.everest.enricher;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.POITextExtractor;
import org.apache.poi.extractor.ExtractorFactory;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xslf.extractor.XSLFPowerPointExtractor;
import org.apache.poi.xslf.usermodel.XSLFSlideShow;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.xmlbeans.XmlException;


public class Request_Generator {
	private static final Logger logger = LogManager.getLogger("Request_Generator");

	public static List <String> sensible_files(ResearchObject ro) throws Exception{
		List <String>listRDF = getlistRDF(ro);
		List <String> res = new ArrayList <String> ();
		for (String rdf : listRDF) {
			if(getFileExtension(rdf.split(ro.getId())[1]).equals("rdf") || getFileExtension(rdf.split(ro.getId())[1]).equals("")){
				String fichero = RDFReader(rdf, ro);
				if (fichero != ""){
					res.add(fichero);
				}
			}
		}
		return res;
	}

	public static List <String> getlistRDF(ResearchObject ro){
		List <String> listRDF = new ArrayList <String> ();
		String sparqlEndpoint = "http://sandbox.rohub.org/rodl/sparql";
		String sparqlQuery = "SELECT ?ab " +
				"WHERE{<"+ro.getId()+"> <http://www.openarchives.org/ore/terms/aggregates> ?res. " +
				"?res a <http://purl.org/ao/Annotation> . " +
				"?res <http://purl.org/ao/body> ?ab. " +
				"?res <http://purl.org/wf4ever/ro#annotatesAggregatedResource> ?agg. " +
				"FILTER (?agg != <"+ro.getId()+">) " +
				"FILTER regex(str(?ab),\"^"+ro.getId()+".*\")}";
		Query query = QueryFactory.create(sparqlQuery, Syntax.syntaxSPARQL) ;
		QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint,query); 
		ResultSet results = qexec.execSelect();

		// print out the predicate, subject and object of each statement
		while (results.hasNext()) {
			QuerySolution sol = results.next();
			RDFNode object = sol.get("ab");
			String res = object.asResource().getURI();

			listRDF.add(res);
		}

		qexec.close();

		return listRDF;
	}
	public static String getFileExtension(String f) {  
		String ext = "";  
		int i = f.lastIndexOf('.');  
		if (i > 0 &&  i < f.length() - 1) {  
			ext = f.substring(i + 1).toLowerCase();  
		}  
		return ext;  
	} 

	public static String RDFReader(String url_RDF, ResearchObject ro) throws Exception{
		logger.info("Analyzing resource: {}", url_RDF);
		HttpClient httpclient = HttpClients.createDefault();
		URIBuilder builder = new URIBuilder(url_RDF);
		URI uri = builder.build();		
		HttpGet request = new HttpGet(uri);
		HttpResponse response = httpclient.execute(request);
		String resultado="";
		if(response.getStatusLine().getStatusCode() != 200){
			throw new Exception("No resource found.  Annotation Body: "+ url_RDF);
		}
		HttpEntity entity = response.getEntity();
		InputStream input = entity.getContent();

		Model model = ModelFactory.createDefaultModel();
		model.read(input,"RDF/XML");

		StmtIterator it = model.listStatements();
		String Document = "http://purl.org/wf4ever/wf4ever#Document";
		String BibliographicResource = "http://purl.org/dc/terms/BibliographicResource";
		String Conclusions = "http://purl.org/wf4ever/roterms#Conclusions";
		String Hypothesis = "http://purl.org/wf4ever/roterms#Hypothesis";
		String ResearchQuestion ="http://purl.org/wf4ever/roterms#ResearchQuestion";
		String Paper = "http://purl.org/wf4ever/roterms#Paper";
		String Sketch = "http://purl.org/wf4ever/roterms#Sketch";
		while(it.hasNext()){
			Statement statement = it.next();
			RDFNode res = statement.getObject();
			if (res.isResource()){
				if((res.asResource().toString().equals(Document))||
						(res.asResource().toString().equals(BibliographicResource))||
						(res.asResource().toString().equals(Conclusions))||
						(res.asResource().toString().equals(Hypothesis))||
						(res.asResource().toString().equals(ResearchQuestion))||
						(res.asResource().toString().equals(Paper))){
					resultado = statement.getSubject().getURI().toString();	
				}
				if(res.asResource().toString().equals(Sketch)){
					resultado = statement.getSubject().getURI().toString();	
					ro.setSketch(resultado);
				}
			}
		}
		model.close();
		return resultado;
	}

	public static String text_extractor(List <String> files, ResearchObject ro) throws IOException, OpenXML4JException, XmlException, URISyntaxException{
		String request = "";
		for (String file : files) {
			HttpClient httpclient = HttpClients.createDefault();
			URIBuilder builder = new URIBuilder(file);
			URI uri = builder.build();
			HttpGet httprequest = new HttpGet(uri);
			logger.info("FILE request: {}", uri);			
			HttpResponse response = httpclient.execute(httprequest);
			HttpEntity entity = response.getEntity();
			InputStream input = entity.getContent();

			if (input!=null){
				if(file.toLowerCase().endsWith(".docx")||(file.toLowerCase().endsWith(".docx"))) {
					request = request + doc_extractor(input,file) + " ";
				}
				if(file.toLowerCase().endsWith(".pptx")) {
					request = request + pptx_extractor(input) + " ";
				}
				if(file.toLowerCase().endsWith(".pdf")) {
					request = request + pdf_extractor(input) + " ";
				}
				if(file.toLowerCase().endsWith(".txt")) {
					request = request + text_extractor(input) + " ";
				}
			}
		}
		return request;
	}

	private static String text_extractor(InputStream input) throws IOException {
		try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
			return buffer.lines().collect(Collectors.joining("\n"));
		}
	}

	public static String doc_extractor(InputStream fis, String url) throws IOException, OpenXML4JException, XmlException{
		POITextExtractor extractor;
		String extractedText = "";
		// if docx
		if (url.toLowerCase().endsWith(".docx")) {
			XWPFDocument doc = new XWPFDocument(fis);
			extractor = new XWPFWordExtractor(doc);
			extractedText = extractor.getText();
		} else {
			// if doc
			POIFSFileSystem fileSystem = new POIFSFileSystem(fis);
			extractor = ExtractorFactory.createExtractor(fileSystem);
			extractedText= extractor.getText();
		}
		return extractedText;
	}
	public static String pptx_extractor(InputStream input) throws IOException, OpenXML4JException, XmlException{
		OPCPackage pkg = OPCPackage.open(input);
		XSLFSlideShow show = new XSLFSlideShow(pkg);
		XSLFPowerPointExtractor extractor = new XSLFPowerPointExtractor(show);
		String text = extractor.getText();
		extractor.close();
		return text;
	}
	public static String pdf_extractor(InputStream input) throws IOException{
		PDDocument pdDoc = PDDocument.load(input);
		PDFTextStripper pdfStripper = new PDFTextStripper();
		pdfStripper.setParagraphStart("/t");
		pdfStripper.setStartPage(1);
		pdfStripper.setEndPage(10);
		String text= pdfStripper.getText(pdDoc);
		String res ="";
		for (String paragraph: text.split(pdfStripper.getParagraphStart()))
		{
			res = res + paragraph.replaceAll("\n", "") + "\n";
		}

		res = res.replaceAll("[^A-Za-z().,:¡!¿?&@;$€\\/\n]", " ");

		pdDoc.close();

		return res;
	}
}
