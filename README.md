# Karate - Dynamic XPath

## Overview
A demonstration of dynamic XPath in Karate Framework:

- Invocation using the Javascript Bridge karate.xmlPath()
- Alternate invocation with the Java interop

## Details

In Karate there are several ways to perform an XPath query on an XML document.

If a static XPath expression is used then a simple DSL call is satisfactory:

```
* xml msg = "<msg><header></header><body></body></msg>"
* def count = get msg //header
```


