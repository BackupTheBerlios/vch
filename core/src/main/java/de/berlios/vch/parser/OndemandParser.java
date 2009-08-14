package de.berlios.vch.parser;

public interface OndemandParser {
    /**
     * Parses a webpage on demand and returns the video URI
     * @param webpage
     * @return the URI to the video
     */
    public String parseOnDemand(String webpage);
}
