# Karate - Dynamic XPath

## Overview
A demonstration of issues with the dynamic XPath facility in Karate Framework.

- **Defective** Invocation using the Javascript Bridge karate.xmlPath()
- **Successful** invocation with direct Java interop

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
 
 
