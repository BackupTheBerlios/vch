package de.berlios.vch.parser;

import java.net.URI;
import java.util.Set;

// TODO thread pool zum parsen bereitstellen, damit nicht jeder parser einen anlegen muss?
public interface IParserService {
    public IWebPage parse(URI vchUri) throws Exception;
    
    public IOverviewPage getParserOverview() throws Exception;
    
    public Set<IWebParser> getParsers();
    
    public IWebParser getParser(String id);
}
