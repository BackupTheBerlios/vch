package de.berlios.vch.parser.zdfmediathek;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.utils.AsxParser;

public class WMVLinkgetter {

    private static transient Logger logger = LoggerFactory.getLogger(WMVLinkgetter.class);
    
    private final String PAGE_ENCODING = "utf-8";
    
    public String GetWMVLink(String link) {

        String wmvlink = "";

        try {
            String _url = link + "?&bw=dsl2000&pp=wmp&view=navJson";
            logger.trace("Looking for enclosure on page {}", _url);
            URL url = new URL(_url);
            URLConnection con = url.openConnection();
            con.addRequestProperty("Accept-Encoding", "gzip");

            InputStream in = con.getInputStream();
            String encoding = con.getContentEncoding();
            if("gzip".equalsIgnoreCase(encoding)) {
                in = new GZIPInputStream(in);
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(in, PAGE_ENCODING));
            String line;

            while ((line = br.readLine()) != null) {

                if (line.matches(".*assetUrl.*")) {
                    String asxfile = line.split("\042")[3];
                    if(asxfile.endsWith("galerie.swf") || asxfile.startsWith("/")) { // this is a flash picture gallery or something like that
                        return null;
                    }
                    
                    logger.trace("Found asx file at {}", asxfile);
                    wmvlink = AsxParser.getUri(asxfile);
                }

            }

        } catch (Exception e) {
            logger.error("An unexpected error occured", e);
        }

        return wmvlink;
    }
}
