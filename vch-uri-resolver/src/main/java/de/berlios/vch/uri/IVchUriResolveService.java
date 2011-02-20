package de.berlios.vch.uri;

import java.net.URI;

import de.berlios.vch.parser.IWebPage;

public interface IVchUriResolveService {
	public IWebPage resolve(URI vchuri) throws Exception;
}
