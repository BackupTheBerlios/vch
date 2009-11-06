package de.berlios.vch.logger;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jLogListener implements LogListener {

    private static transient Logger logger = LoggerFactory.getLogger(Slf4jLogListener.class);

    @Override
    public void logged(LogEntry le) {
        switch (le.getLevel()) {
        case LogService.LOG_DEBUG:
            logger.debug(le.getBundle().getSymbolicName() + ": " + le.getMessage(), le.getException());
            break;
        case LogService.LOG_INFO:
            logger.info(le.getBundle().getSymbolicName() + ": " + le.getMessage(), le.getException());
            break;
        case LogService.LOG_WARNING:
            logger.warn(le.getBundle().getSymbolicName() + ": " + le.getMessage(), le.getException());
            break;
        case LogService.LOG_ERROR:
            logger.error(le.getBundle().getSymbolicName() + ": " + le.getMessage(), le.getException());
            break;
        default:
            logger.trace(le.getBundle().getSymbolicName() + ": " + le.getMessage(), le.getException());
            break;
        }
    }
}
