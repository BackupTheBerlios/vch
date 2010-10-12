package de.berlios.vch.parser;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.Tag;
import org.htmlparser.filters.CssSelectorNodeFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.Translate;

public class HtmlParserUtils {

    public static Node getElementById(final String id, Parser parser) throws ParserException {
        Node desired = null;

        NodeList nodes = parser.extractAllNodesThatMatch(new NodeFilter() {
            @Override
            public boolean accept(Node node) {
                if (node instanceof Tag) {
                    Tag tag = (Tag) node;
                    if (id.equals(tag.getAttribute("id"))) {
                        return true;
                    }
                }
                return false;
            }
        });

        if (nodes.size() > 0) {
            desired = nodes.elementAt(0);
        }

        return desired;
    }

    /**
     * Returns the tag selected by the given selector or null
     * @param html
     * @param charset
     * @param cssSelector
     * @return the tag selected by the given selector or null
     * @throws ParserException
     */
    public static Tag getTag(String html, String charset, String cssSelector) throws ParserException {
        NodeList list = getTags(html, charset, cssSelector);
        if (list.size() > 0) {
            Tag tag = (Tag) list.elementAt(0);
            return tag;
        } else {
            return null;
        }
    }

    public static NodeList getTags(String html, String charset, String cssSelector) throws ParserException {
        Parser parser = Parser.createParser(html, charset);
        CssSelectorNodeFilter filter = new CssSelectorNodeFilter(cssSelector);
        return parser.extractAllNodesThatMatch(filter);
    }

    /**
     * 
     * @param html
     * @param charset
     * @param cssSelector
     * @return The text content of the selected element or an empty string, if
     *         nothing has been selected
     * @throws ParserException
     */
    public static String getText(String html, String charset, String cssSelector) throws ParserException {
        Tag tag = getTag(html, charset, cssSelector);
        if (tag != null) {
            return Translate.decode(tag.toPlainTextString()).trim();
        } else {
            return "";
        }
    }
}
