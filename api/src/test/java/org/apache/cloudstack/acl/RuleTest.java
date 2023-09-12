// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.acl;

import com.cloud.exception.InvalidParameterValueException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class RuleTest {

    @Test
    public void testToString() throws Exception {
        Rule rule = new Rule("someString");
        Assert.assertEquals(rule.toString(), "someString");
    }

    @Test
    public void testMatchesEmpty() throws Exception {
        Rule rule = new Rule("someString");
        Assert.assertFalse(rule.matches(""));
    }

    @Test
    public void testMatchesNull() throws Exception {
        Rule rule = new Rule("someString");
        Assert.assertFalse(rule.matches(null));
    }

    @Test
    public void testMatchesSpace() throws Exception {
        Rule rule = new Rule("someString");
        Assert.assertFalse(rule.matches(" "));
    }

    @Test
    public void testMatchesAPI() throws Exception {
        Rule rule = new Rule("someApi");
        Assert.assertTrue(rule.matches("someApi"));
    }

    @Test
    public void testMatchesWildcardSuffix() throws Exception {
        Rule rule = new Rule("list*");
        Assert.assertTrue(rule.matches("listHosts"));
    }

    @Test
    public void testMatchesWildcardPrefix() throws Exception {
        Rule rule = new Rule("*User");
        Assert.assertTrue(rule.matches("createUser"));
    }

    @Test
    public void testMatchesWildcardMiddle() throws Exception {
        Rule rule = new Rule("list*s");
        Assert.assertTrue(rule.matches("listClusters"));
    }

    @Test
    public void testValidateRuleWithValidData() throws Exception {
        for (String rule : Arrays.asList("a", "1", "someApi", "someApi321", "123SomeApi",
                "prefix*", "*middle*", "*Suffix",
                "*", "**", "f***", "m0nk3yMa**g1c*")) {
            Assert.assertEquals(new Rule(rule).toString(), rule);
        }
    }

    @Test
    public void testValidateRuleWithInvalidData() throws Exception {
        for (String rule : Arrays.asList(null, "", " ", "  ", "\n", "\t", "\r", "\"", "\'",
                "^someApi$", "^someApi", "some$", "some-Api;", "some,Api",
                "^", "$", "^$", ".*", "\\w+", "r**l3rd0@Kr3", "j@s1n|+|0ȷ",
                "[a-z0-9-]+", "^([a-z0-9_\\.-]+)@([\\da-z\\.-]+)\\.([a-z\\.]{2,6})$")) {
            try {
                new Rule(rule);
                Assert.fail("Invalid rule, exception was expected");
            } catch (InvalidParameterValueException e) {
                Assert.assertTrue(e.getMessage().startsWith("Only API names and wildcards are allowed"));
            }
        }
    }
}
