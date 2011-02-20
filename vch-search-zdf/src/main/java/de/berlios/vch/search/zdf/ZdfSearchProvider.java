package de.berlios.vch.search.zdf;

import java.net.URI;
import java.net.URLEncoder;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.ServiceException;

import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.zdf.ZDFMediathekParser;
import de.berlios.vch.search.ISearchProvider;

@Component
@Provides
public class ZdfSearchProvider implements ISearchProvider {

	@Requires(filter="(instance.name=VCH ZDFMediathek Parser)")
	private IWebParser parser;
	
	@Override
	public String getName() {
		return "ZDFmediathek";
	}
	
	@Override
	public IOverviewPage search(String query) throws Exception {
		if(parser == null) {
			throw new ServiceException("ZDFMediathek Parser is not available");
		}
		
		String uri = "http://www.zdf.de/ZDFmediathek/suche?flash=off&sucheText="
			+ URLEncoder.encode(query, "UTF-8");
		IOverviewPage opage = new OverviewPage();
		opage.setParser(getId());
		opage.setUri(new URI(uri));
		opage = (IOverviewPage)parser.parse(opage);
		return opage;
	}

	@Override
	public String getId() {
		return ZDFMediathekParser.class.getName();
	}

	@Override
	public IWebPage parse(IWebPage page) throws Exception {
		if(parser == null) {
			throw new ServiceException("ZDFMediathek Parser is not available");
		}
		
		return parser.parse(page);
	}
}
