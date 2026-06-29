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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.cloudstack.api.APICommand;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

public class RuleTest {

    private static List<String> apiNames;
    private static List<Rule> apiRules;
    private static ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);

    @BeforeClass
    public static void setup() {
        provider.addIncludeFilter(new AnnotationTypeFilter(APICommand.class));
        Set<BeanDefinition> beanDefinitions = provider.findCandidateComponents("org.apache.cloudstack.api");

        apiNames = new ArrayList<>();
        apiRules = new ArrayList<>();
        for(BeanDefinition bd : beanDefinitions) {
            if (bd instanceof AnnotatedBeanDefinition) {
                Map<String, Object> annotationAttributeMap = ((AnnotatedBeanDefinition) bd).getMetadata()
                        .getAnnotationAttributes(APICommand.class.getName());
                String apiName = annotationAttributeMap.get("name").toString();
                apiNames.add(apiName);
                apiRules.add(new Rule(apiName));
            }
        }
    }

    @Test
    public void testToString() throws Exception {
        Rule rule = new Rule("someString");
        Assert.assertEquals(rule.toString(), "someString");
    }

    @Test
    public void ruleMatchesTestNoMatchesOnEmptyString() throws Exception {
        String testCmd = "";
        List<String> matches = new ArrayList<>();
        for (Rule rule : apiRules) {
            if (rule.matches(testCmd)) {
                matches.add(rule.getRuleString());
            }
        }

        Assert.assertEquals(matches.size(), 0);
    }

    @Test
    public void ruleMatchesTestNoMatchesOnNull() throws Exception {
        List<String> matches = new ArrayList<>();
        for (Rule rule : apiRules) {
            if (rule.matches(null)) {
                matches.add(rule.getRuleString());
            }
        }

        Assert.assertTrue(matches.isEmpty());
    }

    @Test
    public void ruleMatchesTestNoMatchesOnSpaceCharacter() throws Exception {
        String testCmd = " ";
        List<String> matches = new ArrayList<>();
        for (Rule rule : apiRules) {
            if (rule.matches(testCmd)) {
                matches.add(rule.getRuleString());
            }
        }

        Assert.assertTrue(matches.isEmpty());
    }

    @Test
    public void ruleMatchesTestWildCardOnEndWorksAsNormalRegex() {
        setup();
        Pattern regexPattern = Pattern.compile("list.*");
        Rule acsRegexRule = new Rule("list*");

        List<String> nonMatches = new ArrayList<>();
        for (String apiName : apiNames) {
            if (acsRegexRule.matches(apiName) != regexPattern.matcher(apiName).matches()) {
                nonMatches.add(apiName);
            }
        }

        Assert.assertTrue(nonMatches.isEmpty());
    }

    @Test
    public void ruleMatchesTestWildCardOnMiddleWorksAsNormalRegex() {
        setup();
        Pattern regexPattern = Pattern.compile("list.*s");
        Rule acsRegexRule = new Rule("list*s");

        List<String> nonMatches = new ArrayList<>();
        for (String apiName : apiNames) {
            if (acsRegexRule.matches(apiName) != regexPattern.matcher(apiName).matches()) {
                nonMatches.add(apiName);
            }
        }

        Assert.assertTrue(nonMatches.isEmpty());
    }

    @Test
    public void ruleMatchesTestWildCardOnStartWorksAsNormalRegex() {
        setup();
        Pattern regexPattern = Pattern.compile(".*User");
        Rule acsRegexRule = new Rule("*User");

        List<String> nonMatches = new ArrayList<>();
        for (String apiName : apiNames) {
            if (acsRegexRule.matches(apiName) != regexPattern.matcher(apiName).matches()) {
                nonMatches.add(apiName);
            }
        }

        Assert.assertTrue(nonMatches.isEmpty());
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
    public void ruleMatchesTestWildcardOnRuleAndCommand() throws Exception {
        Rule rule = new Rule("*");
        Assert.assertTrue(rule.matches("list*"));
    }

    @Test
    public void ruleMatchesTestWildcardOnRuleAndCommandNotAllowed() throws Exception {
        Rule rule = new Rule("list*");
        Assert.assertFalse(rule.matches("*"));
    }

    @Test
    public void ruleMatchesTestWithMultipleStars() throws Exception {
        Rule rule = new Rule("list***");
        Assert.assertFalse(rule.matches("api"));
    }

    @Test
    public void testRuleToStringWithValidStrings() throws Exception {
        for (String rule : Arrays.asList("a", "1", "someApi", "someApi321", "123SomeApi",
                "prefix*", "*middle*", "*Suffix",
                "*", "**", "f***", "m0nk3yMa**g1c*")) {
            Assert.assertEquals(new Rule(rule).toString(), rule);
        }
    }

    @Test
    public void testRuleToStringWithInvalidStrings() throws Exception {
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
