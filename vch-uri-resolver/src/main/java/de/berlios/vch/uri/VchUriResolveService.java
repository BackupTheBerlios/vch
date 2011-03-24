package de.berlios.vch.uri;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.ServiceException;
import org.osgi.service.log.LogService;

import de.berlios.vch.parser.IWebPage;

@Component
@Provides
public class VchUriResolveService implements IVchUriResolveService {

    @Requires
    private LogService logger;

    private Set<IVchUriResolver> resolvers = new HashSet<IVchUriResolver>();

    @Override
    public IWebPage resolve(URI vchuri) throws Exception {
        logger.log(LogService.LOG_DEBUG, "Trying to resolve " + vchuri);
        for (IVchUriResolver resolver : resolvers) {
            if (resolver.accept(vchuri)) {
                return resolver.resolve(vchuri);
            }
        }

        throw new ServiceException("No resolver found for URI " + vchuri);
    }

    // ############ ipojo stuff #########################################

    // validate and invalidate methods seem to be necessary for the bind methods to work
    @Validate
    public void start() {
    }

    @Invalidate
    public void stop() {
    }

    @Bind(id = "resolvers", aggregate = true)
    public synchronized void addResolver(IVchUriResolver resolver) {
        logger.log(LogService.LOG_INFO, "Adding URI resolver " + resolver.getClass().getName());
        resolvers.add(resolver);
        logger.log(LogService.LOG_INFO, resolvers.size() + " URI resolvers available");
    }

    @Unbind(id = "resolvers", aggregate = true)
    public synchronized void removeResolver(IVchUriResolver resolver) {
        logger.log(LogService.LOG_INFO, "Removing search provider " + resolver.getClass().getName());
        resolvers.remove(resolver);
        logger.log(LogService.LOG_INFO, resolvers.size() + " URI resolvers available");
    }

}
