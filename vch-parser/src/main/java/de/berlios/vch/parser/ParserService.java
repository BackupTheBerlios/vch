package de.berlios.vch.parser;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.service.log.LogService;

import de.berlios.vch.http.client.cache.Cache;
import de.berlios.vch.i18n.Messages;

@Component
@Provides
public class ParserService implements IParserService {

    private Cache<String, IWebPage> cache = new Cache<String, IWebPage>(1000, 5, TimeUnit.MINUTES);

    @Requires
    private LogService logger;
    
    @Requires
    private Messages i18n;

    private Set<IWebParser> parsers = new HashSet<IWebParser>();
    
    @Override
    public IWebPage parse(URI vchpage) throws Exception {
        logger.log(LogService.LOG_INFO, "Trying to parse " + vchpage.toString());
        IWebPage page = null;
        if("vchpage://localhost".equals(vchpage.toString())) {
            page = getParserOverview();
        } else {
            String path = vchpage.getPath();
            
            // lookup page in cache 
            page = lookup(path);
            
            if(page != null) {
                // parse the page, if it is not the root page of the parser
                String rootUri = "vchpage://localhost/" + page.getParser();
                if(rootUri.equals(page.getUri().toString())) {
                    logger.log(LogService.LOG_DEBUG, "Getting root page for " + page.getParser());
                    page = getParser(page.getParser()).getRoot();
                } else {
                    logger.log(LogService.LOG_DEBUG, "Parsing page " + page.getTitle());
                    page = parsePage(page);
                }
            } else {
                String msg = "Page not found in cache";
                logger.log(LogService.LOG_ERROR, msg);
                throw new Exception(msg);
            }
        }
        
        // cache the parsed page
        if(!"vchpage".equals(page.getUri().getScheme())) {
            // calculate md5 identifier
            String md5 = md5(page.getUri().toString());

            // cache the page
            cache.put(md5, page);
        }
        
        // also cache the subpages
        if(page instanceof IOverviewPage) {
            IOverviewPage opage = (IOverviewPage) page;

            for (IWebPage subpage : opage.getPages()) {
                if(subpage.getUri() != null) {
                    if(!"vchpage".equals(subpage.getUri().getScheme())) {
                        // calculate md5 identifier
                        String md5 = md5(subpage.getUri().toString());
                        
                        // add page to cache
                        cache.put(md5, subpage);
                    }
                } else {
                    logger.log(LogService.LOG_ERROR, "Page URI of page " + page.getTitle() + "/" + subpage.getTitle()
                            + "[" + page.getParser() + "] is null. Please fix the parser.");
                }
            }
        } 
        
        setVchUri(page, vchpage);
        
        return page;
    }
    
    public IWebParser getParser(String id) {
        for (IWebParser parser : parsers) {
            if (parser.getId().equals(id)) {
                return parser;
            }
        }
        return null;
    }
    
    @Override
    public Set<IWebParser> getParsers() {
        return parsers;
    }
    
    public IOverviewPage getParserOverview() throws Exception {
        IOverviewPage overview = new OverviewPage();
        overview.setUri(new URI("vchpage://localhost"));
        overview.setVchUri(new URI("vchpage://localhost"));
        overview.setTitle(i18n.translate("sites"));
        cache.put("localhost", overview);
        for (IWebParser parser : parsers) {
            IOverviewPage parserPage = new OverviewPage();
            parserPage.setTitle(parser.getTitle());
            parserPage.setUri(new URI("vchpage://localhost/"+parser.getId()));
            parserPage.setVchUri(new URI("vchpage://localhost/"+parser.getId()));
            parserPage.setParser(parser.getId());
            overview.getPages().add(parserPage);
            cache.put(parser.getId(), parserPage);
        }
        
        Collections.sort(overview.getPages(), new WebPageTitleComparator());
        return overview;
    }
    
    private IWebPage parsePage(IWebPage page) throws Exception {
        IWebParser parser = getParser(page.getParser());
        IWebPage parsedPage = null;
        if(parser.getId().equalsIgnoreCase(page.getUri().getPath())) {
            parsedPage = parser.getRoot();
        } else {
            parsedPage = parser.parse(page);
        }
        parsedPage.setParser(parser.getId());
        return parsedPage;
    }
    
    private IWebPage lookup(String path) throws Exception {
        if(!path.isEmpty()) {
            Scanner scanner = new Scanner(path).useDelimiter("/");
            String parserId = scanner.next();
            IWebParser parser = getParser(parserId);
            IWebPage parent = null;
            if(scanner.hasNext()) {
                while(scanner.hasNext()) {
                    String md5Uri = scanner.next();
                    IWebPage page = cache.get(md5Uri);
                    if(page == null) {
                        logger.log(LogService.LOG_INFO, "Page " + md5Uri + " not found in cache");
                        // the current page is not in the cache, we have to 
                        // parse the parent page and then add it to the cache
                        if(parent == null) {
                            logger.log(LogService.LOG_DEBUG, "Getting root page for parser " + parser.getId());
                            parent = parser.getRoot();
                        } else {
                            logger.log(LogService.LOG_DEBUG, "Parsing parent page " + parent.getTitle());
                            parent = parser.parse(parent);
                        }
                        // we have parsed the parent page, now we can add all
                        // subpages to the cache. the desired page will then be
                        // in the cache, too
                        if(parent instanceof IOverviewPage) {
                            IOverviewPage opage = (IOverviewPage) parent;
                            for (IWebPage subpage : opage.getPages()) {
                                cache.put(md5(subpage.getUri().toString()), subpage);
                            }
                            
                            // now we can retrieve the desired page from the cache
                            page = cache.get(md5Uri);
                        } else {
                            throw new Exception("Parent page "+md5Uri+" is part of the path, but seems to be an IVideoPage");
                        }
                    } 
    
                    // we have found the page. if it is the last element in
                    // the path, we can return it, otherwise we have to continue
                    // with the next part
                    if(scanner.hasNext()) {
                        parent = page;
                    } else {
                        return page;
                    }
                }
            } else {
                IWebPage page = cache.get(parserId);
                if(page == null) {
                    page = parser.getRoot();
                }
                return page;
            }
        }
        return null;
    }
    
    private void setVchUri(IWebPage page, URI parent) throws Exception {
        URI vchUri = new URI(parent.toString() + "/" + md5(page.getUri().toString()));
        page.setVchUri(vchUri);
        if(page instanceof IOverviewPage) {
            IOverviewPage opage = (IOverviewPage) page;
            for (IWebPage subpage : opage.getPages()) {
                setVchUri(subpage, vchUri);
            }
        }
    }
    
    private static String md5(String s) throws NoSuchAlgorithmException {
        String digest = "";

        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] b = s.getBytes();
        md.update(b, 0, b.length);
        byte[] md5Bytes = md.digest();

        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = ((int) md5Bytes[i]) & 0xff;
            if (val < 16)
                hexValue.append("0");
            hexValue.append(Integer.toHexString(val));
        }
        digest = hexValue.toString();

        return digest;
    }
    
    
// ############ ipojo stuff #########################################    
    
    // validate and invalidate method seem to be necessary for the bind methods to work
    @Validate
    public void start() {}
    
    @Invalidate
    public void stop() {}

    @Bind(id = "parsers", aggregate = true)
    public synchronized void addParser(IWebParser parser) {
        logger.log(LogService.LOG_INFO, "Adding parser " + parser.getId());
        parsers.add(parser);
        logger.log(LogService.LOG_INFO, parsers.size() + " parsers available");
    }
    
    @Unbind(id="parsers", aggregate = true)
    public synchronized void removeParser(IWebParser parser) {
        logger.log(LogService.LOG_INFO, "Removing parser " + parser.getId());
        parsers.remove(parser);
        logger.log(LogService.LOG_INFO, parsers.size() + " parsers available");
    }
}
