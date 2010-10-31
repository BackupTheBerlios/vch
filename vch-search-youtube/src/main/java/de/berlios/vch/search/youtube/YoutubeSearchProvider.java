package de.berlios.vch.search.youtube;

import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.search.ISearchProvider;

@Component
@Provides
public class YoutubeSearchProvider implements ISearchProvider {

	@Requires(filter="(instance.name=VCH Youtube Parser)")
	private IWebParser youtubeParser;
	
	@Override
	public String getName() {
		return youtubeParser.getTitle();
	}
	
	@Override
	public IOverviewPage search(String query) throws Exception {
		String uri = "http://gdata.youtube.com/feeds/base/videos?q="
			+ URLEncoder.encode(query, "UTF-8") 
			+ "&client=ytapi-youtube-search&alt=rss&v=2";
		IOverviewPage opage = new OverviewPage();
		opage.setParser(youtubeParser.getId());
		opage.setUri(new URI(uri));
		opage = (IOverviewPage)youtubeParser.parse(opage);
		
		// remove related videos and keep only the IVideoPages
		List<IVideoPage> videos = new ArrayList<IVideoPage>(opage.getPages().size());
		for (IWebPage _subpage : opage.getPages()) {
			IOverviewPage subpage = (IOverviewPage) _subpage;
			videos.add((IVideoPage) subpage.getPages().get(0));
		}
		opage.getPages().clear();
		opage.getPages().addAll(videos);
		return opage;
	}

	@Override
	public String getId() {
		return YoutubeSearchProvider.class.getName();
	} 
}
