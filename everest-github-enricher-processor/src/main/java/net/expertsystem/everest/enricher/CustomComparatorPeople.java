package net.expertsystem.everest.enricher;
import java.util.Comparator;

import net.expertsystem.gslnsdk.GSLResponse.Synthesis.People;

public class CustomComparatorPeople implements Comparator<People> {
	    public int compare(People o1, People o2) {
	        return Integer.compare(o2.positions.size(),o1.positions.size());
	    }
	}
