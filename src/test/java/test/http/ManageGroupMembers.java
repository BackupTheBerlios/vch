package test.http;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

public class ManageGroupMembers extends AbstractHttpTest {
    
    private ManageGroup mg = new ManageGroup();
    private ManageFeeds mf = new ManageFeeds();
    
    @Before
    public void addFeedAndGroup() throws Exception {
        mf.addFeed();
        mg.addGroup();
    }
    
    @Test
    public void manageGroupMembers() throws Exception {
        // check, if we can add a feed to a group
        addFeedToGroup();
        
        // check, if we can remove a feed from a group 
        removeFeedFromGroup();
        
        // add the feed it again for the next test
        addFeedToGroup();
        
        // check, if we can delete a non empty group
        mg.deleteGroup();
    }
    
    private void addFeedToGroup() throws MalformedURLException, IOException, SAXException {
        // add the feed to the group
        WebRequest request = new GetMethodWebRequest(TestConstants.GROUP_MEMBER_CONFIG_URI);
        request.setParameter("group_name", TestConstants.GROUP);
        request.setParameter("submit_add", ">>");
        request.setParameter("channels", TestConstants.FEED);
        WebResponse response = conversation.getResponse(request);
        
        // check, if the channel has moved to the members list
        WebForm form = response.getFormWithID("channel_form");
        String added_channel = form.getOptionValues("members")[0];
        assertEquals("Feed wurde nicht der Gruppe hinzugefügt", TestConstants.FEED, added_channel);
    }
    
    private void removeFeedFromGroup() throws MalformedURLException, IOException, SAXException {
        // remove the feed from the group
        WebRequest request = new GetMethodWebRequest(TestConstants.GROUP_MEMBER_CONFIG_URI);
        request.setParameter("group_name", TestConstants.GROUP);
        request.setParameter("submit_delete", "<<");
        request.setParameter("members", TestConstants.FEED);
        WebResponse response = conversation.getResponse(request);
        
        // check, if the channel has moved to the members list
        WebForm form = response.getFormWithID("channel_form");
        String removed_channel = form.getOptionValues("channels")[0];
        assertEquals("Feed wurde nicht aus Gruppe gelöscht", TestConstants.FEED, removed_channel);
    }
}
