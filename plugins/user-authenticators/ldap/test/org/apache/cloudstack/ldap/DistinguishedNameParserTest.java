/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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
