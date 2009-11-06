package de.berlios.vch.parser;

import java.net.URI;
import java.util.Map;

public interface IWebPage {
	/**
	 * The title of the web page
	 * @return the title of the web page
	 */
	public String getTitle();
	public void setTitle(String title);
	
	/**
	 * The uri of the web page
	 * @return the uri of the web page
	 */
	public URI getUri();
	public void setUri(URI uri);
	
	/**
	 * Maybe for some pages additional data is needed.
	 * These information can be stored in here.
	 * @return
	 */
	public Map<String, Object> getUserData();
	
	public String getParser();
	public void setParser(String parser);
}
