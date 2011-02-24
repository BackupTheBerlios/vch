package de.berlios.vch.logger;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogService;

public class JulLogListener implements LogListener {

    private HashMap<Long, Logger> loggers = new HashMap<Long, Logger>();

    @Override
    public void logged(LogEntry log) {
        if ((log.getBundle() == null) || (log.getBundle().getSymbolicName() == null)) {
            // if there is no name, it's probably the framework emitting a log
            // This should not happen and we don't want to log something anonymous
            return;
        }

        // Retrieve a Logger object, or create it if none exists.
        Logger logger = loggers.get(log.getBundle().getBundleId());
        if (logger == null) {
            logger = Logger.getLogger(log.getBundle().getSymbolicName());
            loggers.put(log.getBundle().getBundleId(), logger);
        }

        // If there is an exception available, use it, otherwise just log
        // the message
        if (log.getException() != null) {
            switch (log.getLevel()) {
            case LogService.LOG_DEBUG:
                logger.log(Level.FINE, log.getMessage(), log.getException());
                break;
            case LogService.LOG_INFO:
                logger.log(Level.INFO, log.getMessage(), log.getException());
                break;
            case LogService.LOG_WARNING:
                logger.log(Level.WARNING, log.getMessage(), log.getException());
                break;
            case LogService.LOG_ERROR:
                logger.log(Level.SEVERE, log.getMessage(), log.getException());
                break;
            }
        } else {
            switch (log.getLevel()) {
            case LogService.LOG_DEBUG:
                logger.fine(log.getMessage());
                break;
            case LogService.LOG_INFO:
                logger.info(log.getMessage());
                break;
            case LogService.LOG_WARNING:
                logger.warning(log.getMessage());
                break;
            case LogService.LOG_ERROR:
                logger.severe(log.getMessage());
                break;
            }
        }
    }

}
