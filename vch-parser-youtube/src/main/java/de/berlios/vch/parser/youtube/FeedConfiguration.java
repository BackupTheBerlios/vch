package de.berlios.vch.parser.youtube;

import java.util.List;

public interface FeedConfiguration {
    public void addFeed(String title, String uri);

    public List<Feed> getFeeds();

    public void removeFeed(String id);
}
