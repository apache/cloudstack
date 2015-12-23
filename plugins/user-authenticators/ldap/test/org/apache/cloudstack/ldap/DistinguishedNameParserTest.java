package org.apache.cloudstack.ldap;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class DistinguishedNameParserTest {

    @Test(expected = IllegalArgumentException.class)
    public void testParseLeafeNameEmptyString() throws Exception {
        final String distinguishedName = "";
        DistinguishedNameParser.parseLeafName(distinguishedName);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseLeafeNameSingleMalformedKeyValue() throws Exception {
        final String distinguishedName = "someMalformedKeyValue";
        DistinguishedNameParser.parseLeafName(distinguishedName);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseLeafeNameNonSingleMalformedKeyValue() throws Exception {
        final String distinguishedName = "someMalformedKeyValue,key=value";
        DistinguishedNameParser.parseLeafName(distinguishedName);
    }

    @Test
    public void testParseLeafeNameSingleKeyValue() throws Exception {
        final String distinguishedName = "key=value";
        final String value = DistinguishedNameParser.parseLeafName(distinguishedName);

        assertThat(value, equalTo("value"));
    }

    @Test
    public void testParseLeafeNameMultipleKeyValue() throws Exception {
        final String distinguishedName = "key1=leaf,key2=nonleaf";
        final String value = DistinguishedNameParser.parseLeafName(distinguishedName);

        assertThat(value, equalTo("leaf"));
    }
}
