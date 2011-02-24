package de.berlios.vch.logger;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;

@Component
public class JulLogger {
    @Requires
    private LogService logger;

    @Requires
    private LogReaderService logService;

    private LogListener logListener = new JulLogListener();

    @Validate
    public void validate() {
        logService.addLogListener(logListener);
        logger.log(LogService.LOG_INFO, "LogListener registered");
    }

    @Invalidate
    public void invalidate() {
        System.err.println("Whoopsy, the LogReaderService is gone!");
    }
}
