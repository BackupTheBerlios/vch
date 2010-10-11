package de.berlios.vch.search;

import de.berlios.vch.parser.IOverviewPage;

public interface ISearchService {
    
	public IOverviewPage search(String query);
}
