package de.berlios.vch.search.zdf;

import java.net.URI;
import java.net.URLEncoder;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;

import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.zdf.ZDFMediathekParser;
import de.berlios.vch.search.ISearchProvider;

@Component
@Provides
public class ZdfSearchProvider implements ISearchProvider {
	// TODO retrieve from bundle context
	private ZDFMediathekParser parser = new ZDFMediathekParser();
	
	@Override
	public String getName() {
		return "ZDFmediathek";
	}
	
	@Override
	public IOverviewPage search(String query) throws Exception {
		String uri = "http://www.zdf.de/ZDFmediathek/suche?flash=off&sucheText="
			+ URLEncoder.encode(query, "UTF-8");
		IOverviewPage opage = new OverviewPage();
		opage.setParser(parser.getId());
		opage.setUri(new URI(uri));
		opage = (IOverviewPage)parser.parse(opage);
		return opage;
	}

	@Override
	public String getId() {
		return ZdfSearchProvider.class.getName();
	} 
}
