package net.expertsystem.everest.enricher;
import java.util.Comparator;

import net.expertsystem.gslnsdk.GSLResponse.Synthesis.Organization;

public class CustomComparatorOrg implements Comparator<Organization> {
	    public int compare(Organization o1, Organization o2) {
	        return Integer.compare(o2.positions.size(),o1.positions.size());
	    }
	}
