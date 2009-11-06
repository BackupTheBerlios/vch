package de.berlios.vch.parser;

import java.util.List;

/**
 * An overview page consists of references to other web pages
 * @author henni
 *
 */
public interface IOverviewPage extends IWebPage {
	/**
	 * @return a list of pages, which are listed on the overview page
	 */
	public List<IWebPage> getPages() throws Exception;
}
