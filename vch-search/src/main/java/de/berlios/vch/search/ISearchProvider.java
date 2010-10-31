package de.berlios.vch.search;

import de.berlios.vch.parser.IOverviewPage;

public interface ISearchProvider {
	public String getId();
	
	public String getName();
	
    public IOverviewPage search(String query) throws Exception;
}
