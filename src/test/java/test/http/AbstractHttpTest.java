package test.http;

import java.io.IOException;
import java.util.logging.Level;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import test.http.mockserver.MockupWebServer;

import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebConversation;

import de.berlios.vch.http.HTTPServer;

public abstract class AbstractHttpTest {

    static WebConversation conversation;
    
    @BeforeClass 
    public static void setUpHttpUnit() throws IOException {
        HttpUnitOptions.setExceptionsThrownOnScriptError(true);
        HttpUnitOptions.setDefaultCharacterSet("utf-8");
        conversation = new WebConversation();
    }
    
    @BeforeClass
    public static void startMockupWebServer() throws InterruptedException {
        if(!MockupWebServer.getInstance().isRunning()) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        MockupWebServer.getInstance().startServer();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
            
            // wait for the web server to start
            Thread.sleep(1000);
        }
    }
    
    @BeforeClass
    public static void startVCH() {
        // set logging from VCH to SEVERE, while tests are running 
        java.util.logging.Logger root = java.util.logging.Logger.getLogger("");
        root.setLevel(Level.SEVERE);
        
        // start VCH
        HTTPServer.getInstance().start();
    }
    
    @AfterClass
    public static void stopVCH() {
        HTTPServer.getInstance().stop();
    }
    
    @AfterClass
    public static void stopMockupWebServer() throws IOException {
        if(MockupWebServer.getInstance().isRunning()) {
            MockupWebServer.getInstance().stopServer();
        }
    }
}
