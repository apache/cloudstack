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

package com.cloud.agent.properties;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;

public class AgentPropertiesTest {

    @Test
    public void initTestBlockInstanceWithNullValueAndWithoutType() throws IllegalAccessException {
        AgentProperties agentProperties = new AgentProperties();
        Field[] fields = agentProperties.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (field.getType().equals(AgentProperties.Property.class)) {
                AgentProperties.Property property = (AgentProperties.Property) field.get(agentProperties);

                Assert.assertTrue(String.format("Either inform the default value or the class of property [%s], field [%s].", property.getName(), field.getName()),
                                    property.getDefaultValue() != null || property.getTypeClass() != null);
            }
        }
    }
}
