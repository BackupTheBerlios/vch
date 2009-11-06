package de.berlios.vch.parser.dmax.pages;

import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.OverviewPage;;

public class ProgramListing extends OverviewPage {
    public ProgramListing() {
        getUserData().put("type", IOverviewPage.class.getSimpleName());
        getUserData().put("dmax.type", ProgramListing.class.getSimpleName());
    }
}
