package de.berlios.vch.web.servlets;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.web.TemplateLoader;

@Component
public class WelcomeServlet extends VchHttpServlet {

    public static final String PATH = "/vch";

    @Requires(filter="(instance.name=vch.web)")
    private ResourceBundleProvider rbp;
    
    @Requires
    private TemplateLoader templateLoader;
    
    @Requires
    private HttpService httpService;
    
    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("TITLE", rbp.getResourceBundle().getString("I18N_WELCOME"));
        String page = templateLoader.loadTemplate("welcome.ftl", params);
        resp.getWriter().print(page);
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        get(req, resp);
    }
    
    @Validate
    public void start() throws ServletException, NamespaceException {
        // register the servlet
        httpService.registerServlet(PATH, this, null, null);
    }

    @Invalidate
    public void stop() {
        // unregister the servlet
        httpService.unregister(PATH);
    }

}
