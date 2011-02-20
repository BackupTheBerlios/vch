package de.berlios.vch.uri;

import java.net.URI;

import de.berlios.vch.parser.IWebPage;

public interface IVchUriResolver {
	
    public boolean accept(URI vchuri);
    
    public IWebPage resolve(URI vchuri) throws Exception;
}
