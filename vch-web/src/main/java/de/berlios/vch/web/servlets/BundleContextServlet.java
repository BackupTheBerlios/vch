package de.berlios.vch.web.servlets;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.web.NotifyMessage;
import de.berlios.vch.web.TemplateLoader;

public abstract class BundleContextServlet extends HttpServlet {
    
    protected Dictionary<?, ?> properties;
    
    protected BundleContext bundleContext;
    
    protected Messages i18n;
    
    protected TemplateLoader templateLoader;
    
    protected LogService logger;
    
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
    
    @Override
    final protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html;charset=UTF-8"); // TODO config param
        get(req, resp);
    }

    @Override
    final protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html;charset=UTF-8"); // TODO config param
        post(req, resp);
    }
    
    protected abstract void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;
 
    
    protected abstract void post(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException;
    
    protected void error(HttpServletResponse res, int code, String msg) throws IOException {
        error(res, code, msg, null, false);
    }
    
    protected void error(HttpServletResponse res, int code, String msg, boolean isAjaxRequest) throws IOException {
        error(res, code, msg, null, isAjaxRequest);
    }
    
    protected void error(HttpServletResponse res, int code, String msg, Throwable t) throws IOException {
        error(res, code, msg, t, false);
    }
    
    protected void error(HttpServletResponse res, int code, String msg, Throwable t, boolean isAjaxRequest) throws IOException {
        if(isAjaxRequest) {
            res.setHeader("Content-Type", "text/plain; charset=utf-8");
            res.setStatus(code);
            res.getWriter().println(msg + "\n");
            if(t!= null) {
                t.printStackTrace(res.getWriter());
            }
        } else {
            Map<String, Object> tplParams = new HashMap<String, Object>();
            tplParams.put("TITLE", i18n.translate("I18N_ERROR"));
            tplParams.put("MESSAGE", msg);
            
            if(t != null) {
                tplParams.put("STACKTRACE", NotifyMessage.stackTraceToString(t));
            }
            
            res.setHeader("Content-Type", "text/html;charset=utf-8");
            res.setStatus(code);
            String template = templateLoader.loadTemplate("error.ftl", tplParams);
            res.getWriter().println(template);
        }
    }

    public void setMessages(Messages i18n) {
        this.i18n = i18n;
    }

    public void setTemplateLoader(TemplateLoader templateLoader) {
        this.templateLoader = templateLoader;
    }
    
    public void setLogger(LogService logger) {
        this.logger = logger;
    }
    
    private static final String NOTIFY_MESSAGES = "notifyMessages";
    
    protected void addNotify(HttpServletRequest req, NotifyMessage msg) {
        getNotifyMessages(req).add(msg);
    }
    
    protected List<NotifyMessage> getNotifyMessages(HttpServletRequest req) {
        @SuppressWarnings("unchecked")
        List<NotifyMessage> msgs = (List<NotifyMessage>) req.getAttribute(NOTIFY_MESSAGES);
        if(msgs == null) {
            msgs = new LinkedList<NotifyMessage>();
            req.setAttribute(NOTIFY_MESSAGES, msgs);
        }
        return msgs;
    }
}
