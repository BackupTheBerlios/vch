package de.berlios.vch.web.menu;

import java.util.SortedSet;

/**
 * A menu entry for the menu of the web interface. Can be registered as an osgi service and will be automatically added
 * to the menu by vch-web.
 */
public interface IWebMenuEntry extends Comparable<IWebMenuEntry> {
    /**
     * Returns the displayed title of the menu entry. This value is unique for each level of the menu.
     * 
     * @return the displayed title of the menu entry
     */
    public String getTitle();
    public void setTitle(String title);

    /**
     * Returns the preferred position in the menu
     * 
     * @return the preferred position in the menu
     */
    public int getPreferredPosition();
    public void setPreferredPosition(int pos);

    /**
     * Returns the children this menu entry.
     * 
     * @return the children this menu entry.
     */
    public SortedSet<IWebMenuEntry> getChilds();
    public void setChilds(SortedSet<IWebMenuEntry> childs);

    /**
     * Returns the URI, which will be opened, when the menu entry is selected.
     * 
     * @return the URI, which will be opened, when the menu entry is selected.
     */
    public String getLinkUri();
    public void setLinkUri(String linkUri);
    
    public IWebMenuEntry clone();
}
