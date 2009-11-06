package de.berlios.vch.parser;

import java.net.URI;
import java.util.Calendar;

/**
 * A video page represents a web page with an embedded video
 * and some additional information for the video like
 * description, duration etc.
 * @author henni
 *
 */
public interface IVideoPage extends IWebPage {
	public String getDescription();
	
	public Calendar getPublishDate();
	
	public URI getThumbnail();
	
	public void setVideoUri(URI uri);
	
	public URI getVideoUri();
	
	/**
	 * Duration of the video in seconds
	 * @return duration of the video in seconds
	 */
	public long getDuration();
}
