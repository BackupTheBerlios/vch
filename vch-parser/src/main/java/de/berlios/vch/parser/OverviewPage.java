package de.berlios.vch.parser;

import java.util.LinkedList;
import java.util.List;

public class OverviewPage extends WebPage implements IOverviewPage {

	private List<IWebPage> pages;
	
	@Override
	public List<IWebPage> getPages() {
		if (pages == null) {
			pages = new LinkedList<IWebPage>();
			
		}
		return pages;
	}
}
