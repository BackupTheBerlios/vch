package de.berlios.vch.json;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class StringUtils {

    public static void dumpJson(JSONObject jo) throws JSONException {
        dumpJson(jo, "");
    }

    private static void dumpJson(Object json, String indent) throws JSONException {
        if (json instanceof JSONObject) {
            JSONObject jo = (JSONObject) json;
            @SuppressWarnings("unchecked")
            Iterator<String> keys = jo.keys();
            System.out.println(indent + '{');
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = jo.get(key);
                if (value instanceof JSONObject || value instanceof JSONArray) {
                    System.err.println(indent + key + " = ");
                    dumpJson(value, indent + "  ");
                } else {
                    System.err.println(indent + key + " = " + value);
                }
            }
            System.out.println(indent + '}');
        } else if (json instanceof JSONArray) {
            JSONArray ja = (JSONArray) json;
            System.out.println(indent + '[');
            for (int i = 0; i < ja.length(); i++) {
                Object value = ja.get(i);
                if (value instanceof JSONObject || value instanceof JSONArray) {
                    dumpJson(value, indent + "  ");
                } else {
                    System.err.println(indent + value);
                }
            }
            System.out.println(indent + ']');
        }
    }
}
