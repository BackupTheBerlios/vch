package de.berlios.vch.http.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.http.client.cache.Cache;

public class HttpUtils {
    private static transient Logger logger = LoggerFactory.getLogger(HttpUtils.class);
    
    private static TimeUnit tu = TimeUnit.MINUTES;
    private static int period = 5;
    private static Cache<String, String> stringCache = new Cache<String, String>(1000, period, tu);
    private static Cache<String, HttpResponse> responseCache = new Cache<String, HttpResponse>(1000, period, tu);
    private static Cache<String, Map<String, List<String>>> headerCache = new Cache<String, Map<String, List<String>>>(1000, period, tu);
    
    public static String get(String url, Map<String, String> headers, String charset) throws IOException {
        String cachedPage = (String) stringCache.get(url);
        if(cachedPage != null) {
            logger.trace("Page found in cache");
            return cachedPage;
        } else {
            logger.trace("Downloading page {}", url);
            URL page = new URL(url);
            URLConnection con = page.openConnection();
            if(headers != null) {
                for (Iterator<Entry<String,String>> iterator = headers.entrySet().iterator(); iterator.hasNext();) {
                    Entry<String, String> entry = iterator.next();
                    con.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int length = -1;
            byte[] b = new byte[1024];
            InputStream in = con.getInputStream();
            while( (length = in.read(b)) > 0 ) {
                bos.write(b, 0, length);
            }
         
            String pageContent = new String(bos.toByteArray(), charset);
            stringCache.put(url, pageContent);
            return pageContent;
        }
    }
    
    public static HttpResponse getResponse(String url, Map<String, String> headers, String charset) throws IOException {
        HttpResponse response = (HttpResponse) responseCache.get(url);
        if(response != null) {
            logger.trace("Page found in cache");
            return response;
        } else {
            logger.trace("Downloading page {}", url);
            URL page = new URL(url);
            URLConnection con = page.openConnection();
            if(headers != null) {
                for (Iterator<Entry<String,String>> iterator = headers.entrySet().iterator(); iterator.hasNext();) {
                    Entry<String, String> entry = iterator.next();
                    con.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int length = -1;
            byte[] b = new byte[1024];
            InputStream in = con.getInputStream();
            while( (length = in.read(b)) > 0 ) {
                bos.write(b, 0, length);
            }
            
            response = new HttpResponse(new String(bos.toByteArray(), charset), con.getHeaderFields());
            responseCache.put(url, response);
            return response;
        }
    }
    
    /**
     * 
     * @param url
     * @param headers
     * @param content the post body
     * @param responseCharset the expected charset of the response
     * @return
     * @throws IOException
     */
    public static String post(String url, Map<String, String> headers, byte[] content, String responseCharset) throws IOException {
        // initialize the connection
        URL page = new URL(url);
        HttpURLConnection con = (HttpURLConnection) page.openConnection();
        con.setRequestMethod("POST");
        if(headers != null) {
            for (Iterator<Entry<String,String>> iterator = headers.entrySet().iterator(); iterator.hasNext();) {
                Entry<String, String> entry = iterator.next();
                con.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        con.setDoOutput(true);
        
        //send the post
        OutputStream os = con.getOutputStream();
        os.write(content);
        os.flush();
        
        // read the response
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int length = -1;
        byte[] b = new byte[1024];
        InputStream in = con.getInputStream();
        while( (length = in.read(b)) > 0 ) {
            bos.write(b, 0, length);
        }
        
        return new String(bos.toByteArray(), responseCharset);
    }
    
    /**
     * Adds a parameter to a given URI
     * @param uri
     * @param param
     * @param value
     * @return
     */
    public static String addParameter(String uri, String param, String value) {
        StringBuilder sb = new StringBuilder(uri);
        if(uri.contains("?")) {
            sb.append('&');
        } else {
            sb.append('?');
        }
        
        sb.append(param);
        sb.append('=');
        sb.append(value);
        
        return sb.toString();
    }

    public static Map<String, List<String>> head(String url, Map<String, String> headers, String charset) throws IOException {
        logger.trace("Request HEAD for page {}", url);
        Map<String, List<String>> header = headerCache.get(url);
        if(header != null) {
            return header;
        } else {
            URL page = new URL(url);
            URLConnection con = page.openConnection();
            if(headers != null) {
                for (Iterator<Entry<String,String>> iterator = headers.entrySet().iterator(); iterator.hasNext();) {
                    Entry<String, String> entry = iterator.next();
                    con.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            
            return con.getHeaderFields();
        }
    }
    
    public static String getHeaderField(Map<String, List<String>> headers, String headerField) {
        if(!headers.containsKey(headerField)) {
            return null;
        }
        
        List<String> value = headers.get(headerField);
        if(value.size() == 1) {
            return value.get(0);
        } else {
            throw new RuntimeException("Header contains several values and cannot be mapped to a single String");
        }
    }
}
