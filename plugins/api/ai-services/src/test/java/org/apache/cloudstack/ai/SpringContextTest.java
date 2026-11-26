// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with this
// work for additional information regarding copyright ownership. The ASF
// licenses this file to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance with the
// License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

/**
 * Everything the plugin injects from its own packages must be a bean.
 *
 * <p>CloudStack's core context supplies its own DAOs — DomainDao,
 * HostDao, VMInstanceDao — but a type this plugin defines is one only
 * this plugin's context can declare. The context declared the manager
 * and nothing else, so a real management server found the module,
 * tried to build it and gave up:
 *
 * <pre>
 * Failed to load module [ai-services]: Error creating bean
 * 'aiServicesManagerImpl': Unsatisfied dependency 'deploymentDao';
 * No qualifying bean of type 'org.apache.cloudstack.ai.dao.AiDeploymentDao'
 * </pre>
 *
 * <p>Nothing caught it because no unit test builds the Spring context;
 * they construct the manager directly and set its fields. Only loading
 * the plugin into CloudStack could show it, and this is the cheap
 * guard that keeps it shown.
 */
class SpringContextTest {

    private static final String CONTEXT =
            "META-INF/cloudstack/ai-services/spring-ai-services-context.xml";

    private static String contextXml() throws Exception {
        try (InputStream in =
                SpringContextTest.class.getClassLoader().getResourceAsStream(CONTEXT)) {
            if (in == null) {
                throw new IllegalStateException(CONTEXT + " is not on the classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static List<String> declaredClasses(String xml) {
        List<String> found = new ArrayList<>();
        Matcher m = Pattern.compile("class=\"([^\"]+)\"").matcher(xml);
        while (m.find()) {
            found.add(m.group(1));
        }
        return found;
    }

    /** Types this plugin owns, which only its own context can supply. */
    private static boolean ours(Class<?> type) {
        return type.getName().startsWith("org.apache.cloudstack.ai.");
    }

    @Test
    void everyInjectedTypeThisPluginOwnsHasABean() throws Exception {
        List<String> declared = declaredClasses(contextXml());

        List<String> missing = new ArrayList<>();
        for (Field field : AiServicesManagerImpl.class.getDeclaredFields()) {
            if (field.getAnnotation(Inject.class) == null || !ours(field.getType())) {
                continue;
            }
            boolean satisfied = declared.stream().anyMatch(name -> {
                try {
                    return field.getType().isAssignableFrom(Class.forName(name));
                } catch (ClassNotFoundException absent) {
                    return false;
                }
            });
            if (!satisfied) {
                missing.add(field.getType().getName());
            }
        }

        assertTrue(missing.isEmpty(),
                "no bean declared for " + missing + " in " + CONTEXT
                        + "; CloudStack will refuse to load the module");
    }

    @Test
    void theManagerItselfIsDeclared() throws Exception {
        assertTrue(declaredClasses(contextXml())
                .contains(AiServicesManagerImpl.class.getName()));
    }

    @Test
    void everyDeclaredBeanClassExists() throws Exception {
        // A typo in the XML is the same outage, found at startup
        // rather than at compile time.
        List<String> absent = new ArrayList<>();
        for (String name : declaredClasses(contextXml())) {
            try {
                Class.forName(name);
            } catch (ClassNotFoundException missing) {
                absent.add(name);
            }
        }

        assertFalse(declaredClasses(contextXml()).isEmpty(), "no beans declared at all");
        assertTrue(absent.isEmpty(), "declared but not on the classpath: " + absent);
    }
}
