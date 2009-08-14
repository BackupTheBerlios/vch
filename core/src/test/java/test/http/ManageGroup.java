package test.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

public class ManageGroup extends AbstractHttpTest {
    
    @Test
    public void manageGroup() throws Exception {
        addGroup();
        editGroup();
        deleteGroup();
    }
    
    public void addGroup() throws Exception {
        WebRequest request = new GetMethodWebRequest(TestConstants.GROUP_CONFIG_URI);
        request.setParameter("group_name", TestConstants.GROUP);
        request.setParameter("action", "addgroup");
        WebResponse response = conversation.getResponse(request);

        assertTrue("Fehler beim anlegen der Gruppe", response.getText().contains("Gruppe wurde angelegt"));
    }
    
    public void editGroup() throws Exception {
        String desc = "Beschreibung mit Umlauten und Sonderzeichen <>&!§$%&/()=?äöü";
        WebRequest request = new GetMethodWebRequest(TestConstants.GROUP_MEMBER_CONFIG_URI);
        request.setParameter("group_name", TestConstants.GROUP);
        request.setParameter("action", "save_desc");
        request.setParameter("desc", desc);
        
        WebResponse response = conversation.getResponse(request);
        WebForm form = response.getFormWithID("change_desc");
        String _desc = form.getParameterValues("desc")[0];
        assertEquals("Beschreibung stimmt nicht überein", desc, _desc);
    }
    
    public void deleteGroup() throws Exception {
        WebRequest request = new GetMethodWebRequest(TestConstants.GROUP_CONFIG_URI);
        request.setParameter("group_name", TestConstants.GROUP);
        request.setParameter("action", "delete");
        request.setParameter("delete_entries", "true");
        WebResponse response = conversation.getResponse(request);

        assertTrue("Fehler beim Löschen der Gruppe", response.getText().contains("Gruppe wurde gelöscht"));
    }
}
