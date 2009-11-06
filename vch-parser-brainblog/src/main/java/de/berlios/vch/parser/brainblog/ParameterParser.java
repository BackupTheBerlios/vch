package de.berlios.vch.parser.brainblog;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParameterParser {

    private static transient Logger logger = LoggerFactory.getLogger(ParameterParser.class);
    
    @SuppressWarnings("unchecked")
    public static void parseQuery(String query, Map<String, Object> parameters) throws UnsupportedEncodingException {
        if(query != null) {
            StringTokenizer st = new StringTokenizer(query, "&");
            while (st.hasMoreTokens()) {
                String keyValue = st.nextToken();
                StringTokenizer st2 = new StringTokenizer(keyValue, "=");
                String key = null;
                String value = "";
                if (st2.hasMoreTokens()) {
                    key = st2.nextToken();
                    key = URLDecoder.decode(key, "utf-8");
                }

                if (st2.hasMoreTokens()) {
                    value = st2.nextToken();
                    value = URLDecoder.decode(value, "utf-8");
                }

                logger.debug("Found key value pair: " + key + "," + value);
                if(parameters.containsKey(key)) {
                    logger.debug("Key already exists. Assuming array of values. Will bes tored in a list");
                    Object o = parameters.get(key);
                    if(o instanceof List) {
                        List<String> values = (List<String>) o;
                        values.add(value);
                    } else if(o instanceof String) {
                        List<String> values = new ArrayList<String>();
                        values.add((String)o);
                        values.add(value);
                        parameters.put(key, values);
                    }
                } else {
                    parameters.put(key, value);
                }
            }
        }
    }
}
