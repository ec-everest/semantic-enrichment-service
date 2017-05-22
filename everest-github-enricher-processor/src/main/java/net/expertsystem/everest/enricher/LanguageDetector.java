package net.expertsystem.everest.enricher;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.optimaize.langdetect.DetectedLanguage;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.cybozu.util.LangProfile;
import com.optimaize.langdetect.frma.LangProfileReader;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.OldLangProfileConverter;

public class LanguageDetector {


	public static String detect(String text) throws IOException{
		com.optimaize.langdetect.LanguageDetector detect= makeNewDetector();		 
	     List<DetectedLanguage> result = detect.getProbabilities(text);	  
	     if (!result.isEmpty()){
	    	 DetectedLanguage best = result.get(0);
	     	return best.getLocale().getLanguage();
	     }
	     else
	    	return "en";
		
	}

    private static com.optimaize.langdetect.LanguageDetector makeNewDetector() throws IOException {
        LanguageDetectorBuilder builder = LanguageDetectorBuilder.create(NgramExtractors.standard())
            .shortTextAlgorithm(50)
            .prefixFactor(1.5)
            .suffixFactor(2.0);

        LangProfileReader langProfileReader = new LangProfileReader();
        for (String language : ImmutableList.of("en", "it", "es", "de")) {
            @SuppressWarnings("deprecation")
			LangProfile langProfile = langProfileReader.read(LanguageDetector.class.getResourceAsStream("/languages/" + language));
            LanguageProfile languageProfile = OldLangProfileConverter.convert(langProfile);
            builder.withProfile(languageProfile);
        }

        return builder.build();
    }
	
}
