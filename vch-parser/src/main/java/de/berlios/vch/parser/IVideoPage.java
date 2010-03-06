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
	
	public void setDescription(String description);
	
	public Calendar getPublishDate();
	
	public void setPublishDate(Calendar publishDate);
	
	public URI getThumbnail();
	
	public void setThumbnail(URI uri);
	
	public URI getVideoUri();
	
	public void setVideoUri(URI uri);
	
	/**
	 * Duration of the video in seconds
	 * @return duration of the video in seconds
	 */
	public long getDuration();
	
	public void setDuration(long duration);
}
