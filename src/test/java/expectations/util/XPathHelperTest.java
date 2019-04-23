package expectations.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * A simple unit test for our custom <code>XPathHelper</code> for Karate.
 * <p>
 * Exercises the multi-fall-back strategy implemented
 * for various shaped XML results.
 */
public class XPathHelperTest {

    private String xml = "<users><user><name>Homer</name><age>45.1</age></user><user><name>Bart</name><age>12.0</age></user></users>";

    @Test
    public void canHandleNodeListQuery() {

        String xpathQuery = "/users/user";

        String result = XPathHelper.doQuery(xml, xpathQuery);

        // count user = 2
        assertThat(result, startsWith("<user>"));
        assertThat(result, containsString("Homer"));
        assertThat(result, containsString("Bart"));
        // Node List String representation
        assertThat(result,
            equalTo("<user>\n<name>Homer</name>\n<age>45.1</age>\n</user>\n<user>\n<name>Bart</name>\n<age>12.0</age>\n</user>\n"));
    }

    @Test
    public void canHandleIntegerNumericQuery() {

        String xpathQuery = "count(/users/user)";

        String result = XPathHelper.doQuery(xml, xpathQuery);

        assertThat(result, equalTo("2"));
    }

    @Test
    public void canHandleDoubleNumericQuery() {

        String xpathQuery = "/users/user[1]/age/text()";

        String result = XPathHelper.doQuery(xml, xpathQuery);

        assertThat(result, equalTo("45.1"));
    }

    @Test(expected = RuntimeException.class)
    public void givenBadXPathThenWillThrow() {

        String xpathQuery = "/users/badFunction(";

        String result = XPathHelper.doQuery(xml, xpathQuery);

        // exceptionRootCause = javax.xml.xpath.XPathExpressionException
    }

    @Test
    public void givenUnresolvedQueryThenWillReturnEmptyString()
    {

        String xpathQuery = "/users/user[10]";

        String result = XPathHelper.doQuery(xml, xpathQuery);

        assertThat(result, is(notNullValue()));
        assertThat(result, equalTo(""));
    }
}

