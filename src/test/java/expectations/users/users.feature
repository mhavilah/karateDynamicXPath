Feature: Example of Karate Xpath success/failure

  Background:
    * def testUsers =
 """
<users></users>
"""
    * set testUsers /users/user
      | path           | 1       | 2      | 3         | 4         |
      | name/firstName | 'John'  | 'Jane' | 'Bart'    | 'Homer'   |
      | name/lastName  | 'Smith' | 'Doe'  | 'Simpson' | 'Simpson' |
      | age            | 10      | 20     | 30        | 40        |

    * def xpathQuery = "count(/users/user)"
    * def xpathQueryFn =
"""
  function(xml, xpathQuery) {
    var XPathHelper = Java.type("expectations.util.XPathHelper");
    var result  = XPathHelper.doQuery(xml, xpathQuery);
    return result;
} 
"""

  Scenario: print all users
    * print testUsers
    * match $testUsers.users.user.length() == 4

  Scenario: select a user via xpath
    * def lastUser = karate.xmlPath(testUsers,'/users/user[last()]')
    * print "LastUser:", lastUser
    * match lastUser ==  "<user><name><firstName>Homer</firstName><lastName>Simpson</lastName></name><age>40</age></user>"

  Scenario: select a user first name via xpath
    * def firstName = karate.xmlPath(testUsers,'/users/user[last()]/name/firstName')
    * print "firstName:", firstName
    * match firstName ==  "Homer"

  Scenario: select a user's name text via xpath
    * def firstUser = karate.xmlPath(testUsers,'//user[1]/name/firstName/text()')
    * print "FirstUser:", firstUser
    * match firstUser ==  "John"

  # Note the following: returns the wrapping element - not just the element's text value
  Scenario: select a user first name via a custom Java XPath Helper
    * xmlstring testUserXMLString = testUsers
    * def firstName = xpathQueryFn(testUserXMLString,'/users/user[last()]/name/firstName')
    * print "firstName:", firstName
    * match firstName ==  "<firstName>Homer</firstName>"

  # Now explicitly retrieve the element's text value
  Scenario: select a user first name via a custom Java XPath Helper
    * xmlstring testUserXMLString = testUsers
    * def firstName = xpathQueryFn(testUserXMLString,'/users/user[last()]/name/firstName/text()')
    * print "firstName:", firstName
    * match firstName ==  "Homer"

  Scenario: count all users via a custom Java XPath Helper
# Force a string representation of the XML
# Not Karate's internal JSON analogue of XML..
    * xmlstring testUserXMLString = testUsers
    * def userCount = xpathQueryFn( testUserXMLString, xpathQuery )
    * print "Result:", userCount
    * match userCount == 4

  Scenario: select a user via a custom Java XPath Helper
    * xmlstring testUserXMLString = testUsers
    * def lastUser = xpathQueryFn(testUserXMLString,'/users/user[last()]')
    * print "LastUser:", lastUser
    * match lastUser ==  "<user><name><firstName>Homer</firstName><lastName>Simpson</lastName></name><age>40</age></user>"

# In Karate v0.9.2 and earlier the following will fail.
# Due to the NUMBER result -> NodeList cast error within the Karate ScriptBridge class
#  javax.xml.xpath.XPathExpressionException: com.sun.org.apache.xpath.internal.XPathException: Can not convert #NUMBER to a NodeList!
# @ignore
  Scenario: count all users via karate xpath
    * print "==========================================================================="
    * print "= This is the failing scenario, which is invoked via the Karate ScriptBridge"
    * print "==========================================================================="

    * print testUsers
    * def userCount = karate.xmlPath(testUsers,xpathQuery)
    * match userCount == 4
