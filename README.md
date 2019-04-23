# Karate v0.92 - Dynamic XPath issues

## Overview
A demonstration of issues with the dynamic XPath facility in Karate Framework.

- **Defective** Invocation using the Javascript Bridge karate.xmlPath()
- **Successful** 
  - invocation with direct Java interop
  - invocation with Karate DSL 

## Details

In Karate there are several ways to perform an XPath query on an XML document.

### Static XPath

If a **static** XPath expression is used then a simple DSL call is satisfactory:

```
* xml msg = "<msg><headers><header>From</header><header>To</header></headers><body>...</body></msg>"
* def headers = $msg //header
* def count = get msg  count(//header)
```

In the above example the two XPath expressions return two differently shaped results:

- NodeList - list of header elements
- Numberic - count of header elements

These scenarios are correctly handled by the Karate Core class: **com.intuit.karate.Script**:

```java
 public static ScriptValue evalXmlPathOnXmlNode(Node doc, String path) {
        NodeList nodeList;
        try {
            nodeList = XmlUtils.getNodeListByPath(doc, path);
        } catch (Exception e) {
            // hack, this happens for xpath functions that don't return nodes (e.g. count)
            String strValue = XmlUtils.getTextValueByPath(doc, path);
            return new ScriptValue(strValue);
        }
        ....
 ```
 
 ### Dynamic XPath
 
 Dynamic XPaths are supported by the Javascript helper:  karate.xmlPath().
 
 This Javascript based API supports dynamic XPath expressions, evaluated at runtime:
 
 ```cucumber 
 * def doXPath =
  """
    function(requestXML, xpathQuery) {
      var response = karate.xmlPath( requestXML, xpathQuery );
      return response;
    }
 """
 * def result = eval doXPath( request, "//headers" );
 * def count = eval doXPath( request, "count(//headers)" );
 ```
 
 Unfortunately, the wrapper code does not recognise the possible XPath result shapes, ie:
 
 - NodeList vs
 - Numeric 
 - String
 etc
 
 And simply invokes the Karate core ScriptBridge method:  **com.intuit.karate.core.ScriptBridge**
 
 ```java
  public Object xmlPath(Object o, String path) {
        if (!(o instanceof Node)) {
            if (o instanceof Map) {
                o = XmlUtils.fromMap((Map) o);
            } else {
                throw new RuntimeException("not XML or cannot convert: " + o);
            }
        }
        Node result = XmlUtils.getNodeByPath((Node) o, path, false);
        int childElementCount = XmlUtils.getChildElementCount(result);
        if (childElementCount == 0) {
            return StringUtils.trimToNull(result.getTextContent());
        }
        return XmlUtils.toNewDocument(result);
    }    
```
 
The method **XmlUtills.getNodeByPath()** makes the assumption that the XPath result will be a NodeList.
As such, XPath expressions that evaluate to a number (such as the count() XPath function) will fail to be cast:

```java
 public static Node getNodeByPath(Node node, String path, boolean create) {
        String searchPath = create ? stripNameSpacePrefixes(path) : path;
        XPathExpression expr = compile(searchPath);
        Node result;
        try {
            result = (Node) expr.evaluate(node, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        if (result == null && create) {
            Document doc = node.getNodeType() == Node.DOCUMENT_NODE ? (Document) node : node.getOwnerDocument();
            return createNodeByPath(doc, path);
        } else {
            return result;
        }
    }
```


