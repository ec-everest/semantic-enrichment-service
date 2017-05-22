package net.expertsystem.everest.enricher;
import java.util.Comparator;

import net.expertsystem.gslnsdk.GSLResponse.Synthesis.Place;

public class CustomComparatorPlaces implements Comparator<Place> {
	    public int compare(Place o1, Place o2) {
	        return Integer.compare(o2.positions.size(),o1.positions.size());
	    }
	}
