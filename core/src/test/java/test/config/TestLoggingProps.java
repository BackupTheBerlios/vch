package test.config;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

public class TestLoggingProps {
    Properties props = new Properties();
    
    @Before
    public void loadPRoperties() throws FileNotFoundException, IOException {
        props.load(new FileReader("src/main/config/logging.properties"));
    }
    
    @Test
    public void testRootLevel() {
        String root = props.getProperty(".level");
        assertTrue(".level ist falsch", root != null && root.equals("FINEST"));
    }
    
    @Test
    public void testHandlers() {
        String level = props.getProperty("java.util.logging.FileHandler.level");
        assertTrue("FileHandler hat level " + level + " statt FINEST", level != null && level.equals("FINEST"));
        
        level = props.getProperty("java.util.logging.ConsoleHandler.level");
        assertTrue("ConsoleHandler hat " + level + " statt INFO", level != null && level.equals("INFO"));
    }
}
