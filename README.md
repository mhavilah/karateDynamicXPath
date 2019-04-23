# Karate v0.92 - Dynamic XPath issues

## Overview
A demonstration of issues with the dynamic XPath facility in Karate Framework v0.92.

- **Defective** Invocation using the Javascript Bridge karate.xmlPath()
- **Successful** 
  - invocation with direct Java interop
  - invocation with Karate DSL 

## Details

In Karate there are several ways to perform an XPath query on an XML document.

### Static XPath

If a **static** XPath expression is used then a simple Karate DSL call is satisfactory:

```cucumber
* xml msg = "<msg><headers><header>From</header><header>To</header></headers><body>...</body></msg>"
* def headers = $msg //header
* def count = get msg  count(//header)
```

In the above example the two XPath expressions return two differently shaped results:

- NodeList - list of header elements
- Numeric - count of header elements

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
 
 Dynamic XPaths are supported by the Javascript helper:  **karate.xmlPath()**.
 
 This Javascript based API supports dynamic XPath expressions, evaluated at runtime:
 
 ```cucumber 
 * def doXPath =
  """
    function(requestXML, xpathQuery) {
      var response = karate.xmlPath( requestXML, xpathQuery );
      return response;
    }
 """
 * def result = doXPath( request, "//headers" );
 * def count = doXPath( request, "count(//headers)" );
 ```
 
 Unfortunately, the wrapper code does not recognise the many possible XPath result shapes, (from **java.xml.xpath.XPathConstants**) ie:
 
 - NodeList vs
 - Numeric 
 - String
 etc
 
 And simply invokes the Karate core ScriptBridge method:  **com.intuit.karate.core.ScriptBridge.xmlPath()**
 
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
 
The method **XmlUtills.getNodeByPath()** makes the assumption that the XPath result will be a Node.
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

### XPath via Direct Java Interop

An alternate to using the faulty karate.xmlPath() facility for dynamic XPath Expressions returning arbitrary shaped data is to directly invoke the Java XPath API directly via a custom Java Helper class.

This approach is illustrated in the codebase as an interim work-around to the current karate.xmlPath() issues.

```java
    public static String doQuery(String xml, String xpathQuery) {
...
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
```

The above approach uses a series of stategies attempting to invoke the given dynamic XPath expression, assuming an XPath Query response shape of:
- NodeSet
- Node
- Number
- String

in decreasing precedence order.

For example an XPath returning an integer count() of XML elements will return throught the NUMBER path.

## Building the example

The source is a Maven project based on the Karate archetype project.

### Prerequisites

- git
- Maven 3.x
- Java 1.8.x
- IntelliJ (optional)

Maven will download all dependencies upon first build

```bash
$ git clone https://github.com/mhavilah/karateDynamicXPath.git
$ cd karateDynamicXPath
$ mvn clean test
```

## Running the example

The example consists of two tests:

- A Karate bootstrap test harness for the **users.feature**
  - expectations.ExpectationsTest
- A Unit test for the Java XPathHelper class
  - expectations.util.XPathHelperTest

The Karate test will run 4 scenarios.

### Failing Tests

Within the **ExpectationsTest**, the last scenario uses the faulty **karate.xmlPath()** API with a Numerical response type XPath Expression.

To disable this faulty scenario, just uncomment the "@ignore" tag within the **users/users.feature** Karate script:

```cucumber
Feature: Example of Karate Xpath success/failure

  Background:
...
...
# 
# The following will fail due to the NUMBER result -> NodeList cast error
#  javax.xml.xpath.XPathExpressionException: com.sun.org.apache.xpath.internal.XPathException: Can not convert #NUMBER to a NodeList!
# @ignore
  Scenario: count all users via karate xpath
    * print "==========================================================================="
    * print "= This is the failing scenario, which is invoked via the Karate ScriptBridge"
    * print "==========================================================================="

    * print testUsers
    * def userCount = karate.xmlPath(testUsers,xpathQuery)
    * match userCount == 4
```    
