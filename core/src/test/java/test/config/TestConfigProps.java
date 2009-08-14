package test.config;

import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestConfigProps {
    
    Properties props = new Properties();
    
    @Before
    public void loadPRoperties() throws FileNotFoundException, IOException {
        props.load(new FileReader("src/main/config/vodcatcherhelper.properties"));
    }
    
    @Test
    public void testJDBC() throws Exception {
        // test the JDBC URL
        String jdbcUrl = props.getProperty("db.connection.url");
        assertTrue("JDBC URL ist falsch", jdbcUrl != null && !jdbcUrl.contains("hsql://"));
    }
    
    @Test
    public void testParsersDisabled() {
        // test, if all parsers are disabled
        for (Object okey : props.keySet()) {
            String key = (String) okey;
            if(key.startsWith("de.berlios.vch.parser.") && key.endsWith(".enabled")) {
                String value = props.getProperty(key);
                assertTrue("Parser not disabled " + key, "false".equalsIgnoreCase(value));
            }
        }
    }
    
    @Test 
    public void unfinishedFeaturesCommented() {
        // check, that unfinished features have comments
        String[] features = {
            "de.berlios.vch.parser.n24.N24Parser.enabled",
            "flvstreamer.path"
        };
        for (String feature : features) {
            Assert.assertNull(feature + " is in config", props.get(feature));
        }
    }
     
    @Test
    public void testPasswordEmpty() {
        // test, if the password is not set
        String dbpass = props.getProperty("db.pass");
        assertTrue("Password is set", dbpass == null || dbpass.length() == 0);
    }
}
