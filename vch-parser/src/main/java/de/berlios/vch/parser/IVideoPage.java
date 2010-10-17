package de.berlios.vch.parser;

import java.io.Serializable;
import java.net.URI;
import java.util.Calendar;

/**
 * A video page represents a web page with an embedded video
 * and some additional information for the video like
 * description, duration etc.
 * @author henni
 *
 */
public interface IVideoPage extends IWebPage, Cloneable, Serializable {
	public String getDescription();
	
	public void setDescription(String description);
	
	public Calendar getPublishDate();
	
	public void setPublishDate(Calendar publishDate);
	
	public URI getThumbnail();
	
	public void setThumbnail(URI uri);
	
    /**
     * URI of the video file
     * 
     * @return the URI of the video file
     */
    public URI getVideoUri();

    /**
     * Sets the URI of the video file
     */
    public void setVideoUri(URI uri);

	/**
	 * Duration of the video
	 * @return duration of the video in seconds
	 */
	public long getDuration();
	
	/**
     * Sets the duration of the video
     * @param duration in secods
     */
	public void setDuration(long duration);
	
	public Object clone() throws CloneNotSupportedException;
}
