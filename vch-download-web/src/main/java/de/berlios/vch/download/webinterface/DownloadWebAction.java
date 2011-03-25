package de.berlios.vch.download.webinterface;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.web.IWebAction;

@Component
@Provides
public class DownloadWebAction implements IWebAction {

    @Requires(filter = "(instance.name=vch.web.download.servlet)")
    private ResourceBundleProvider rbp;

    @Override
    public String getUri(IWebPage page) throws UnsupportedEncodingException {
        String uri = DownloadsServlet.PATH;
        uri += "?action=add&vchuri=";
        uri += URLEncoder.encode(page.getVchUri().toString(), "UTF-8");
        return uri;
    }

    @Override
    public String getTitle() {
        return rbp.getResourceBundle().getString("I18N_DL_OSD_DOWNLOAD");
    }

}
