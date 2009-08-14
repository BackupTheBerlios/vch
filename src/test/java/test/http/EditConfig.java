package test.http;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

public class EditConfig extends AbstractHttpTest {
    
    @Test
    public void changeConfig() throws Exception {
        String url = "http://localhost:8080/config";
        
        // change ajax.enabled to true
        String value = "false";
        WebRequest request = new PostMethodWebRequest(url);
        request.setParameter("ajax.enabled", value);
        request.setParameter("action", "save");
        WebResponse response = conversation.getResponse(request);
        assertNotNull("Kein Response von URL '" + url + "'.", response);

        WebForm form = response.getFormWithID("config_form");
        assertNotNull("Formular nicht gefunden.", form);
        
        String ajaxEnabled = form.getParameterValues("ajax.enabled")[0];
        assertEquals("ajax.enabled wurde nicht geändert", value, ajaxEnabled);
        
        // change ajax.enabled to false
        value = "true";
        request = new PostMethodWebRequest(url);
        request.setParameter("ajax.enabled", value);
        request.setParameter("action", "save");
        response = conversation.getResponse(request);
        assertNotNull("Kein Response von URL '" + url + "'.", response);

        form = response.getFormWithID("config_form");
        assertNotNull("Formular nicht gefunden.", form);
        
        ajaxEnabled = form.getParameterValues("ajax.enabled")[0];
        assertEquals("ajax.enabled wurde nicht geändert", value, ajaxEnabled);
    }
}
