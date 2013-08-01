// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.package com.cloud.utils;
package com.cloud.utils;

import java.util.List;
import org.junit.Test;

import static com.cloud.utils.ReflectUtil.flattenProperties;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;

public final class ReflectUtilTest {

    @Test
    public void testFlattenNonNullProperties() throws Exception {

        final List<String> expectedResult = newArrayList("booleanProperty",
                TRUE.toString(), "intProperty", "1",
                "stringProperty", "foo");

        final Bean bean = new Bean(1, true, "foo");

        assertEquals(expectedResult, flattenProperties(bean, Bean.class));

    }

    @Test
    public void testFlattenNullProperties() throws Exception {

        final List<String> expectedResult = newArrayList("booleanProperty",
                TRUE.toString(), "intProperty", "1",
                "stringProperty", "null");

        final Bean bean = new Bean(1, true, null);

        assertEquals(expectedResult, flattenProperties(bean, Bean.class));

    }

    @Test
    public void testFlattenPropertiesNullTarget() throws Exception {
        assertEquals(emptyList(), flattenProperties(null, Bean.class));
    }

    public static final class Bean {

        private final int intProperty;
        private final boolean booleanProperty;
        private final String stringProperty;

        private Bean(final int intProperty, final boolean booleanProperty,
             final String stringProperty) {

            super();

            this.intProperty = intProperty;
            this.booleanProperty = booleanProperty;
            this.stringProperty = stringProperty;

        }

        public int getIntProperty() {
            return intProperty;
        }

        public boolean isBooleanProperty() {
            return booleanProperty;
        }

        public String getStringProperty() {
            return stringProperty;
        }

    }
}
