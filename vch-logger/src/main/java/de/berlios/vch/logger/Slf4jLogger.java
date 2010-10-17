package de.berlios.vch.logger;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;

@Component
public class Slf4jLogger {
    @Requires
    private LogService logger;
    
    @Requires
    private LogReaderService logService;
    
    @Validate
    public void validate() {
        Slf4jLogListener logListener = new Slf4jLogListener();
        logService.addLogListener(logListener);
        logger.log(LogService.LOG_INFO, "LogListener registered");
    }
    
    @Invalidate
    public void invalidate() {
        System.err.println("Whoopsy, the LogReaderService is gone!");
    }
}
