package de.berlios.vch.parser;

public interface IWebParser {
    public String getTitle();
    
	public IOverviewPage getRoot() throws Exception;
	
	public IWebPage parse(IWebPage page) throws Exception;
	
	public String getId();
}
