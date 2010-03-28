package de.berlios.vch.parser;

import java.net.URI;
import java.util.Set;

public interface IParserService {
    public IWebPage parse(URI vchUri) throws Exception;
    
    public IOverviewPage getParserOverview() throws Exception;
    
    public Set<IWebParser> getParsers();
    
    public IWebParser getParser(String id);
}
