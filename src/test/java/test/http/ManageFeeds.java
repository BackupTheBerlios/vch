package test.http;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Test;
import org.xml.sax.SAXException;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

public class ManageFeeds extends AbstractHttpTest {

    @Test
    public void manageFeed() throws Exception {
        addFeed();
        deleteFeed();
    }
    
    public void addFeed() throws Exception {
        WebRequest request = new GetMethodWebRequest(TestConstants.EDITFEED_URI);
        request.setParameter("channel", TestConstants.FEED);
        request.setParameter("action", "add_channel");
        WebResponse response = conversation.getResponse(request);

        assertTrue("Fehler beim Anlegen des Feeds: \n" + response.getText(), response.getText().contains("Feed wurde erstellt"));
    }
    
    public void deleteFeed() throws MalformedURLException, IOException, SAXException {
        WebRequest request = new GetMethodWebRequest(TestConstants.EDITFEED_URI);
        request.setParameter("channel", TestConstants.FEED);
        request.setParameter("delete_channel", "");
        WebResponse response = conversation.getResponse(request);        
        
        assertTrue("Fehler beim Löschen des Feeds: \n" + response.getText(), response.getText().contains("Feed wurde gelöscht"));
    }
}
