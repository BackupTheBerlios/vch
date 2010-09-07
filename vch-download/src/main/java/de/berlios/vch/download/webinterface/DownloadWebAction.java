package de.berlios.vch.download.webinterface;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.web.IWebAction;

public class DownloadWebAction implements IWebAction {

    private Messages i18n;

    public DownloadWebAction(Messages i18n) {
        this.i18n = i18n;
    }
    
    @Override
    public String getUri(IWebPage page) throws UnsupportedEncodingException {
        String uri = DownloadsServlet.PATH;
        uri += "?action=add&vchuri=";
        uri += URLEncoder.encode(page.getVchUri().toString(), "UTF-8");
        return uri;
    }

    @Override
    public String getTitle() {
        return i18n.translate("I18N_DL_OSD_DOWNLOAD");
    }

}
