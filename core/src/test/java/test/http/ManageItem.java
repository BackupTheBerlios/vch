package test.http;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

public class ManageItem extends AbstractHttpTest {
    
    public static final String title = "Testtitle";
    public static final String desc = "TestDesc";
    public static final String enc_link = "http://localhost/test.mp4";
    
    public static final String new_title = "Was anderes";
    public static final String new_desc = "Was neues";
    public static final String new_enc_link = "http://localhost/neu.mp4";
    
    private ManageFeeds mf = new ManageFeeds();
    
    @Before
    public void addFeed() throws Exception {
        mf.addFeed();
    }
    
    @Test
    public void manageItem() throws Exception {
        addItem();
        editItem();
        deleteItem();
    }
    
    private void deleteItem() throws MalformedURLException, IOException, SAXException {
        WebRequest request = new GetMethodWebRequest(TestConstants.EDITFEED_URI);
        request.setParameter("guid", ManageItem.enc_link);
        request.setParameter("action", "delete_item");
        WebResponse response = conversation.getResponse(request);

        assertTrue("Fehler beim Anlegen des Feeds", response.getText().contains("Eintrag wurde gelöscht"));
    }

    private void editItem() throws MalformedURLException, IOException, SAXException {
        WebRequest request = new GetMethodWebRequest(TestConstants.EDITFEED_URI);
        request.setParameter("guid", ManageItem.enc_link);
        request.setParameter("title", new_title);
        request.setParameter("desc", new_desc);
        request.setParameter("enc_link", new_enc_link);
        request.setParameter("action", "save_item");
        WebResponse response = conversation.getResponse(request);

        assertTrue("Fehler beim Anlegen des Feeds", response.getText().contains("Eintrag wurde gespeichert"));
    }

    public void addItem() throws Exception {
        WebRequest request = new GetMethodWebRequest(TestConstants.EDITFEED_URI);
        request.setParameter("channel", TestConstants.FEED);
        request.setParameter("title", title);
        request.setParameter("desc", desc);
        request.setParameter("enc_link", enc_link);
        request.setParameter("action", "add_item");
        WebResponse response = conversation.getResponse(request);

        assertTrue("Fehler beim Anlegen des Items", response.getText().contains("Eintrag wurde hinzugefügt"));
    }
    
    @After
    public void deleteFeed() throws MalformedURLException, IOException, SAXException {
        mf.deleteFeed();
    }
}
