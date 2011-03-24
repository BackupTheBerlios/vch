package de.berlios.vch.search.osd;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.ResourceBundle;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.service.log.LogService;

import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.osdserver.ID;
import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdObject;
import de.berlios.vch.osdserver.osd.StringEditOsdItem;
import de.berlios.vch.osdserver.osd.menu.Menu;
import de.berlios.vch.osdserver.osd.menu.actions.IOsdAction;
import de.berlios.vch.osdserver.osd.menu.actions.OverviewAction;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.WebPageTitleComparator;
import de.berlios.vch.search.ISearchService;

@Component
@Provides
public class OpenSearchMenu implements OverviewAction {

    @Requires(filter = "(instance.name=vch.osd.search)")
    private ResourceBundleProvider rbp;

    @Requires
    private LogService logger;

    @Requires
    private ISearchService searchService;

    @Override
    public String getName() {
        return rbp.getResourceBundle().getString("I18N_SEARCH");
    }

    @Override
    public String getEvent() {
        return Event.KEY_RED;
    }

    @Override
    public String getModifier() {
        return null;
    }

    @Override
    public void execute(OsdSession session, OsdObject oo) throws Exception {
        Osd osd = session.getOsd();
        final ResourceBundle rb = rbp.getResourceBundle();
        Menu menu = new Menu(ID.randomId(), rb.getString("I18N_SEARCH"));

        String id = ID.randomId();
        final StringEditOsdItem item = new StringEditOsdItem(id, rb.getString("I18N_QUERY"), "android", session);
        item.registerAction(new IOsdAction() {
            @Override
            public String getName() {
                return "OK";
            }

            @Override
            public String getModifier() {
                return null;
            }

            @Override
            public String getEvent() {
                return Event.KEY_OK;
            }

            @Override
            public void execute(OsdSession session, OsdObject oo) throws Exception {
                search(session, item.getText());
            }
        });

        menu.addOsdItem(item);
        menu.registerAction(new OverviewAction() {
            @Override
            public String getName() {
                return rb.getString("I18N_SEARCH");
            }

            @Override
            public String getModifier() {
                return null;
            }

            @Override
            public String getEvent() {
                return Event.KEY_GREEN;
            }

            @Override
            public void execute(OsdSession session, OsdObject oo) throws Exception {
                search(session, item.getText());
            }
        });

        osd.createMenu(menu);
        osd.appendToFocus(menu);
        osd.show(menu);
    }

    private void search(OsdSession session, String query) throws Exception {
        // print out status message
        String status = rbp.getResourceBundle().getString("I18N_SEARCHING_FOR");
        status = MessageFormat.format(status, query);
        Osd osd = session.getOsd();
        osd.showMessage(new OsdMessage(status, OsdMessage.INFO, 1));

        // execute search and sort the result
        IOverviewPage results = searchService.search(query);
        try {
            Collections.sort(results.getPages(), new WebPageTitleComparator());
        } catch (Exception e1) {
            logger.log(LogService.LOG_WARNING, "Couldn't sort providers by name", e1);
        }

        // open the results in a new menu
        Menu menu = new SearchResultMenu(session, results, searchService);
        osd.createMenu(menu);
        osd.appendToFocus(menu);
        osd.show(menu);
    }
}
