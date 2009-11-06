package de.berlios.vch.osdserver.osd.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.osdserver.Activator;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.IEventListener;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.menu.ItemDetailsMenu;
import de.berlios.vch.osdserver.osd.menu.Menu;
import de.berlios.vch.osdserver.osd.menu.OverviewMenu;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.exceptions.NoSupportedVideoFoundException;


public class OverviewClickListener implements IEventListener {

    private static transient Logger logger = LoggerFactory.getLogger(OverviewClickListener.class);
    
    private Osd osd = Osd.getInstance();
    
    private Messages i18n;
    
    public OverviewClickListener(Messages i18n) {
        this.i18n = i18n;
    }

    @Override
    public void eventHappened(Event event) {
        if(event.getType().equals(Event.KEY_OK)) {
            OsdItem item = (OsdItem) event.getSource();
            IWebPage page = (IWebPage) item.getUserData();
            try {
                osd.showMessage(new OsdMessage(i18n.translate("loading"), OsdMessage.STATUS));
                
                IWebParser parser = getParser(page.getParser());
                if(parser == null) {
                    osd.showMessage(new OsdMessage(i18n.translate("error_parser_missing"), OsdMessage.ERROR));
                    return;
                }
                
                if (page.getUri() != null && "vchpage://root".equals(page.getUri().toString())) {
                    page = parser.getRoot();
                } else {
                    page = parser.parse(page);
                }
                
                if(page instanceof IOverviewPage) {
                    try {
                        Menu siteMenu = new OverviewMenu((IOverviewPage) page, i18n);
                        osd.createMenu(siteMenu);
                        // TODO activate downloads
//                        try {
//                            osd.setColorKeyText(siteMenu, "Downloads", Event.KEY_BLUE);
//                            siteMenu.registerEvent(new Event(siteMenu.getId(), Event.KEY_BLUE, null));
//                        } catch (Exception e) {
//                            logger.error("Couldn't create color button", e);
//                        }
                        osd.appendToFocus(siteMenu);
                        osd.showMessage(new OsdMessage("", OsdMessage.STATUSCLEAR));
                        osd.show(siteMenu);
                    } catch (Exception e) {
                        logger.error("Couldn't create osd menu", e);
                    }
                } else if(page instanceof IVideoPage) {
                    osd.showMessage(new OsdMessage(i18n.translate("loading"), OsdMessage.STATUS));
                    IVideoPage vpage = (IVideoPage) page;
              
                    Menu itemDetailsMenu = new ItemDetailsMenu(vpage);
                    osd.createMenu(itemDetailsMenu);
                    if(vpage.getVideoUri() != null && !vpage.getVideoUri().toString().isEmpty()) {
                        osd.setColorKeyText(itemDetailsMenu, i18n.translate("play"), Event.KEY_GREEN);
                    }
                    // TODO activate downloads
                    //osd.setColorKeyText(itemDetailsMenu, i18n.translate("download"), Event.KEY_BLUE);
                    osd.appendToFocus(itemDetailsMenu);
                    osd.showMessage(new OsdMessage("", OsdMessage.STATUSCLEAR));
                    osd.show(itemDetailsMenu);
                }
            } catch(NoSupportedVideoFoundException e) {
                logger.warn("No supported video format found on page {}", page.getUri().toString());
                try {
                    osd.showMessage(new OsdMessage(i18n.translate("no_supported_video_format"), OsdMessage.ERROR));
                } catch (Exception e1) {
                    logger.error("Couldn't send error msg to osd", e1);
                }
            } catch(Exception e) {
                logger.error("Couldn't parse web page", e); 
                try {
                    osd.showMessage(new OsdMessage(i18n.translate("error_load_item") + " " +e.getLocalizedMessage(), OsdMessage.ERROR));
                } catch (Exception e1) {
                    logger.error("Couldn't send error msg to osd", e1);
                }
            }
        }
    }
    
    public IWebParser getParser(String id) {
        Object[] parsers = Activator.parserTracker.getServices();
        for (Object o : parsers) {
            IWebParser parser = (IWebParser) o;
            if(id.equals(parser.getId())) {
                return parser;
            }
        }
        
        return null;
    }
}
