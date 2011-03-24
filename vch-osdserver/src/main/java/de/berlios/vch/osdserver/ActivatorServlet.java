package de.berlios.vch.osdserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.Handler;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.InstanceStateListener;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.apache.felix.ipojo.handlers.dependency.Dependency;
import org.apache.felix.ipojo.handlers.dependency.DependencyHandler;
import org.osgi.framework.ServiceException;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.log.LogService;

import de.berlios.vch.osdserver.OsdSession.OsdSessionListener;
import de.berlios.vch.web.servlets.VchHttpServlet;

@Component
public class ActivatorServlet extends VchHttpServlet {

    public final static String PATH = "/osdserver";

    @Requires
    private LogService logger;

    @Requires
    private HttpService httpService;

    @Requires(filter = "(factory.name=de.berlios.vch.osdserver.OsdSession)")
    private Factory sessionFactory;

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, String> requestPrefs = new HashMap<String, String>();
        Enumeration<?> prefNames = req.getParameterNames();
        while (prefNames.hasMoreElements()) {
            String name = (String) prefNames.nextElement();
            if (name != null) {
                String v = req.getParameter(name);
                if (v != null) {
                    requestPrefs.put(name, v);
                }
            }
        }

        try {
            OsdSession session = createSession(requestPrefs);
            Thread t = new Thread(session);
            t.setName("Osdserver Session");
            t.start();
            resp.getWriter().println("Osdserver session started");
        } catch (MissingHandlerException mhe) {
            throw new ServletException("OsdSession is missing a requirement", mhe);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private OsdSession createSession(Map<String, String> requestPrefs) throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        final InstanceManager instance = (InstanceManager) sessionFactory.createComponentInstance(null);
        instance.start();

        if (instance.getState() == ComponentInstance.VALID) {
            instance.addInstanceStateListener(new InstanceStateListener() {
                @Override
                public void stateChanged(ComponentInstance instance, int state) {
                    if (state == ComponentInstance.INVALID) {
                        instance.stop();
                        instance.dispose();
                    }
                }
            });
            OsdSession session = (OsdSession) instance.getPojoObject();
            session.setRequestPreferences(requestPrefs);
            session.addOsdSessionListener(new OsdSessionListener() {
                @Override
                public void sessionEnded() {
                    instance.stop();
                    instance.dispose();
                }
            });
            logger.log(LogService.LOG_DEBUG, "Session instance: " + session);
            return session;
        } else {
            handleInvalidInstance(instance);
            return null;
        }
    }

    private void handleInvalidInstance(InstanceManager instance) {
        for (Handler handler : instance.getRegistredHandlers()) {
            if (!handler.getValidity()) {
                if (handler instanceof DependencyHandler) {
                    DependencyHandler dh = (DependencyHandler) handler;
                    List<String> unresolvedDeps = new ArrayList<String>();
                    for (Dependency dep : dh.getDependencies()) {
                        if (dep.getState() != Dependency.RESOLVED) {
                            unresolvedDeps.add(dep.getSpecification().getName());
                        }
                    }
                    String msg = "OsdSession is missing the dependencies " + unresolvedDeps.toString();
                    instance.dispose();
                    throw new ServiceException(msg);
                } else {
                    logger.log(LogService.LOG_WARNING, "OsdSession is missing a requirement " + handler.getDescription());
                }
            }
        }
        instance.dispose();
        throw new ServiceException("OsdSession is missing a requirement");
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        get(req, resp);
    }

    @Validate
    public void start() throws ServletException, NamespaceException {
        // register the servlet
        httpService.registerServlet(ActivatorServlet.PATH, this, null, null);
    }

    @Invalidate
    public void stop() {
        // unregister the config servlet
        if (httpService != null) {
            httpService.unregister(ActivatorServlet.PATH);
        }
    }
}
