package expectations.util;

import com.intuit.karate.XmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.StringWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

/**
 * An XPath processor with response "shape" aware XPath expression strategies.
 * <p>
 * The cascading Exception approach isn't the most performance efficient and we
 * could check for corner case expressions like "count(...)" but this would be
 * brittle.
 * <p>
 * Used as a replacement for Karate.xmlPath() Javascript Bridge facility.
 */
public class XPathHelper {

    public static String doQuery(String xml, String xpathQuery) {

        Logger logger = LoggerFactory.getLogger(XPathHelper.class);

        logger.debug("XPathHelper.doQuery() entered");
        logger.debug("XML doc:" + xml);
        logger.debug("XPath query:" + xpathQuery);

        Document doc = XmlUtils.toXmlDoc(xml);
        String result;

        try {
            XPathExpression expr = compile(xpathQuery);

            try {
                NodeList nodeList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                result = nodeListToString(nodeList);
            } catch (Exception xe) {
                logger.debug("...not a NODELIST shaped result...trying NODE");
                try {
                    // Fallback #1 - Node result
                    Node node = (Node) expr.evaluate(doc, XPathConstants.NODE);
                    result = XmlUtils.toString(node);
                    logger.debug("XPathHelper.doQuery() Node result:" + result);
                } catch (Exception xee) {
                    logger.debug("...not a NODE shaped result...trying NUMBER");
                    try {
                        // Fallback #2 - Numeric result (eg, count())
                        Double doubleResult = (Double) expr.evaluate(doc, XPathConstants.NUMBER);

                        if (Math.round(doubleResult) == doubleResult) {
                            // Truncate decimals
                            result = Long.toString(doubleResult.longValue());
                        } else {
                            result = Double.toString(doubleResult);
                        }

                        logger.debug("XPathHelper.doQuery() Numeric result:" + result);
                    } catch (Exception xeee) {
                        logger.debug("...not a NUMERIC shaped result...trying STRING");
                        // Fallback #3 - String Result (eg, Text Nodes)
                        result = (String) expr.evaluate(doc, XPathConstants.STRING);
                        logger.debug("XPathHelper.doQuery() String result:" + result);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("XPathHelper.doQuery(), Exception whilst processing xpath: " + e.toString());
            throw new RuntimeException(e);
        }

        logger.debug("XPathHelper.doQuery() result:" + result);

        return result;
    }

    // https://stackoverflow.com/questions/31399798/converting-nodelist-to-string-in-java
    private static String nodeListToString(NodeList nodeList) {

        StringWriter result = new StringWriter();

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node elem = nodeList.item(i);
            StringWriter buf = new StringWriter();

            try {
                Transformer xform = TransformerFactory.newInstance().newTransformer();
                // Optional formatting
                xform.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                // xform.setOutputProperty(OutputKeys.INDENT, "yes");       // Pretty print with multi-line indentation
                xform.transform(new DOMSource(elem), new StreamResult(buf));
                result.append(buf.toString());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return result.toString();
    }

    // Copied verbatim from KarateCore com.intuit.karate.XmlUtils L142
    // Changed to: package scope not private
    static XPathExpression compile(String path) {

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        try {
            return xpath.compile(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    // Return XML Document
    //  XmlUtils.toNewDocument(result);
}
