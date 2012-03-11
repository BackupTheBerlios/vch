package de.berlios.vch.parser.sf;

/**
 * Container class for the different video types and qualities. The URI is split into uriPart1 and uriPart2. This is needed for rtmp streams.
 */
public class VideoType implements Comparable<VideoType> {
    private String uri;
    private int width;
    private int bitrate;

    public VideoType(String uri, int width, int bitrate) {
        super();
        this.uri = uri;
        this.width = width;
        this.bitrate = bitrate;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    /*
     * First compare the video resolution. If that is equal, compare the video bitrates
     */
    @Override
    public int compareTo(VideoType other) {
        if (this.getWidth() > other.getWidth()) {
            return 1;
        } else if (this.getWidth() < other.getWidth()) {
            return -1;
        } else if (this.getBitrate() > other.getBitrate()) {
            return 1;
        } else if (this.getBitrate() < other.getBitrate()) {
            return -1;
        }
        return 0;
    }

}
