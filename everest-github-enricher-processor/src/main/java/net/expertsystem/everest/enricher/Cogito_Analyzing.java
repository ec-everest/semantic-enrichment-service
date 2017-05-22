package net.expertsystem.everest.enricher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.expertsystem.gslnsdk.GSLRequest;
import net.expertsystem.gslnsdk.GSLRequester;
import net.expertsystem.gslnsdk.GSLResponse;
import net.expertsystem.gslnsdk.GSLRequest.Synthesis;
import net.expertsystem.gslnsdk.GSLRequest.Synthesis.SynthEntities;
import net.expertsystem.gslnsdk.GSLRequest.Synthesis.SynthRelevant.RelevantValue;
import net.expertsystem.gslnsdk.GSLResponse.Synthesis.Domain;
import net.expertsystem.gslnsdk.GSLResponse.Synthesis.MainGroup;
import net.expertsystem.gslnsdk.GSLResponse.Synthesis.MainSyncon;
import net.expertsystem.gslnsdk.GSLResponse.Synthesis.Organization;
import net.expertsystem.gslnsdk.GSLResponse.Synthesis.People;
import net.expertsystem.gslnsdk.GSLResponse.Synthesis.Place;
import net.expertsystem.lab.sensigrafo.SensigrafoDAO;
import net.expertsystem.sdk.CogitoLanguage;

public class Cogito_Analyzing {
	private static final Logger logger = LogManager.getLogger("Cogito_Analyzing");
	public static String analyzeXML(String request) throws IOException{    	
		String res = "";
		if (request != null){
			GSLRequester testObj = new GSLRequester(CogitoLanguage.ENGLISH);
			GSLRequest req1 = getNERRequest001(request);
			GSLResponse resp1 = testObj.run(req1);
			res = resp1.printXMLResponse();
		}
		return res;
	}
	public static void analyzeRO(String request, ResearchObject ro, String lang, SensigrafoDAO sensei_en, SensigrafoDAO sensei_it, 
			SensigrafoDAO sensei_es) throws IOException, URISyntaxException{    	
		if (!request.isEmpty()){
			GSLRequester testObj = null;
			SensigrafoDAO sensei = null;
			switch (lang) {
				case "en": 
							testObj = new GSLRequester(CogitoLanguage.ENGLISH);
							sensei = sensei_en;
							break;
				case "it": 	
							testObj = new GSLRequester(CogitoLanguage.ITALIAN);
							sensei = sensei_it;
							break;
				case "es": 
							testObj = new GSLRequester(CogitoLanguage.SPANISH);
							sensei = sensei_es;
							break;
//				case "de": 
//							testObj = new GSLRequester(CogitoLanguage.GERMAN);
//							sensei = new SensigrafoDAO(System.getenv("SENSIGRAFO_DEU"));
//							break;
			}
			
			GSLRequest req1 = getNERRequest001(request);
			GSLResponse resp1 = testObj.run(req1);
			ro.setPeople(list_people(resp1.people()));
			ro.setOrg(list_org(resp1.organizations()));
			ro.setPlaces(list_place(resp1.places()));
			ro.setExpressions(list_group(resp1.mainGroups()));
			ro.setConcepts(list_syncon(resp1.mainSyncons(),sensei));
			ro.setDomains(list_domain(resp1.domains()));
		}
	}
	public static List <String> forbidden_words(String lista) throws IOException, URISyntaxException{
		BufferedReader br;
		List<String> data = new ArrayList<String>();
		logger.debug("loading forbbiden word list: {}",lista);				
		InputStream in=Cogito_Analyzing.class.getClassLoader().getResourceAsStream(lista);
		
		br = new BufferedReader(new InputStreamReader(in));
		String x;
		while ( (x = br.readLine()) != null) {
			data.add(x.trim());
		}
		br.close();

		return data;
	}
	public static List<String>list_people(List<People> list) throws IOException, URISyntaxException{
		List<String>res = new ArrayList<String>();
		List<People> modifiableListPeople = new ArrayList<People>(list);
		Collections.sort(modifiableListPeople, new CustomComparatorPeople());
		Iterator<People> it = modifiableListPeople.iterator();
		int numero_variables = 5;
		for (int i = 1;i<numero_variables+1;i++){
			boolean palabra_erronea = false;
			if (!it.hasNext()){
				break;
			}
			People aux = it.next();
			String nombre = aux.name;
			for (String fw : forbidden_words("no_people.txt")){
				if (fw.equals(nombre)){
					palabra_erronea = true;
					break;
				}
			}
			if(palabra_erronea){
				i=i-1;
			}
			else{
				res.add(nombre);
			}
		}
		return res;
	}
	public static List<String>list_org(List<Organization> list) throws IOException, URISyntaxException{
		List<String>res = new ArrayList<String>();
		List<Organization> modifiableListPeople = new ArrayList<Organization>(list);
		Collections.sort(modifiableListPeople, new CustomComparatorOrg());
		Iterator<Organization> it = modifiableListPeople.iterator();
		int numero_variables = 5;
		for (int i = 1;i<numero_variables+1;i++){
			boolean palabra_erronea = false;
			if (!it.hasNext()){
				break;
			}
			Organization aux = it.next();
			String nombre = aux.name;
			for (String fw : forbidden_words("no_org.txt")){
				if (fw.equals(nombre)){
					palabra_erronea = true;
					break;
				}
			}
			if(palabra_erronea){
				i=i-1;
			}
			else{
				res.add(nombre);
			}
		}
		return res;
	}
	public static List<String>list_place(List<Place> list) throws IOException, URISyntaxException{
		List<String>res = new ArrayList<String>();
		List<Place> modifiableListPlace = new ArrayList<Place>(list);
		Collections.sort(modifiableListPlace, new CustomComparatorPlaces());
		Iterator<Place> it = modifiableListPlace.iterator();
		int numero_variables = 5;
		for (int i = 1;i<numero_variables+1;i++){
			boolean palabra_erronea = false;
			if (!it.hasNext()){
				break;
			}
			Place aux = it.next();
			String nombre = aux.name;
			//int valor = aux.positions.size();
			for (String fw : forbidden_words("no_places.txt")){
				if (fw.equals(nombre)){
					palabra_erronea = true;
					break;
				}
			}
			if(palabra_erronea){
				i=i-1;
			}
			else{
				res.add(nombre);
			}
		}
		return res;
	}
	public static List<String>list_group(List<MainGroup> list) throws IOException, URISyntaxException{
		List<String>res = new ArrayList<String>();
		Iterator<MainGroup> it = list.iterator();
		while(it.hasNext()){
			boolean palabra_erronea = false;
			MainGroup aux = it.next();
			String nombre = aux.group;
			//Optional<Double> valor = aux.score;
			for (String fw : forbidden_words("no_groups.txt")){
				if (fw.equals(nombre)){
					palabra_erronea = true;
					break;
				}
			}
			if(!palabra_erronea){
				res.add(nombre);
			}
		}
		return res;
	}
	public static List<String>list_syncon(List<MainSyncon> list, SensigrafoDAO sensei) throws IOException, URISyntaxException{
		List<String>res = new ArrayList<String>();
		Iterator<MainSyncon> it = list.iterator();
		while(it.hasNext()){
			boolean palabra_erronea = false;
			MainSyncon aux = it.next();
			int valor = aux.syncon;
			List <String>lemmas_syncon = sensei.mostFreqSynconLemmas(valor);
			String nombre = lemmas_syncon.get(0);
			for (String fw : forbidden_words("no_syncons.txt")){
				if (fw.equals(nombre)){
					palabra_erronea = true;
					break;
				}
			}
			if(!palabra_erronea){
				res.add(nombre);
			}
		}
		return res;
	}
	public static List<String>list_domain(List<Domain> list) throws IOException, URISyntaxException{
		List<String>res = new ArrayList<String>();
		Iterator<Domain> it = list.iterator();
		int numero_variables = 5;
		for (int i = 1;i<numero_variables+1;i++){
			boolean palabra_erronea = false;
			if (!it.hasNext()){
				break;
			}
			Domain aux = it.next();
			String nombre = aux.name;
			//Double valor = aux.score;
			for (String fw : forbidden_words("no_domains.txt")){
				if (fw.equals(nombre)){
					palabra_erronea = true;
					break;
				}
			}
			if(palabra_erronea){
				i=i-1;
			}
			else{
				res.add(nombre);
			}
		}
		return res;
	}
		public static GSLRequest getNERRequest001(final String inputText) {
			return GSLRequest.newBuilder().withContent(inputText).
					withAnalysis(nerSynthesis01()).
					build();
		}
		private static Synthesis nerSynthesis01() {
			Synthesis synthesis = Synthesis.builder().
					withEntities(SynthEntities.People, SynthEntities.KnownConcept, SynthEntities.Organization, SynthEntities.Place).
					withRelevants(RelevantValue.MAINLEMMAS, RelevantValue.MAINSYNCONS, RelevantValue.MAINGROUPS).
					withAllDomains().
					withPositions().
					withDetails().
					build();
			return synthesis;
		}
	}
