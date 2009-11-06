package de.berlios.vch.parser.dmax.pages;

import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.OverviewPage;

public class EpisodePage extends OverviewPage {
    public EpisodePage() {
        getUserData().put("type", IOverviewPage.class.getSimpleName());
        getUserData().put("dmax.type", EpisodePage.class.getSimpleName());
    }
}
