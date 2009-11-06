package de.berlios.vch.logger;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.service.log.LogReaderService;

@Component
public class Slf4jLogger {
    
    private Slf4jLogListener logListener = new Slf4jLogListener();

    @Requires
    private LogReaderService logService;
    
    @Validate
    public void validate() {
        logService.addLogListener(logListener);
    }
    
    @Invalidate
    public void invalidate() {
        System.err.println("Whoopsy, the LogReaderService is gone!");
    }
}
