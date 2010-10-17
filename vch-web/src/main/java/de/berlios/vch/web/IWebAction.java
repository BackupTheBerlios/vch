package de.berlios.vch.web;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import de.berlios.vch.parser.IWebPage;


public interface IWebAction  {
	public String getUri(IWebPage page) throws UnsupportedEncodingException, URISyntaxException;
	public String getTitle();
}
