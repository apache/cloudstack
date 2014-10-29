//
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
//

package com.cloud.utils;

import static com.cloud.utils.ReflectUtil.flattenProperties;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public final class ReflectUtilTest {

    @Test
    public void testFlattenNonNullProperties() throws Exception {

        final List<String> expectedResult = newArrayList("booleanProperty", TRUE.toString(), "intProperty", "1", "stringProperty", "foo");

        final Bean bean = new Bean(1, true, "foo");

        assertEquals(expectedResult, flattenProperties(bean, Bean.class));

    }

    @Test
    public void testFlattenNullProperties() throws Exception {

        final List<String> expectedResult = newArrayList("booleanProperty", TRUE.toString(), "intProperty", "1", "stringProperty", "null");

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

        private Bean(final int intProperty, final boolean booleanProperty, final String stringProperty) {

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

    static class Empty {
    }

    static class Foo {
        String fooField;
        int fooIntField;
    }

    static class Bar extends Foo {
        String barField;
        int barIntField;
    }

    static class Baz extends Foo {
        String bazField;
        int bazIntField;
    }

    @Test
    public void getAllFieldsForClassWithFoo() throws NoSuchFieldException, SecurityException {
        Set<Field> fooFields = ReflectUtil.getAllFieldsForClass(Foo.class, new Class<?>[] {});
        Assert.assertNotNull(fooFields);
        Assert.assertTrue(fooFields.contains(Foo.class.getDeclaredField("fooField")));
        Assert.assertTrue(fooFields.contains(Foo.class.getDeclaredField("fooIntField")));
    }

    @Test
    public void getAllFieldsForClassWithBar() throws NoSuchFieldException, SecurityException {
        Set<Field> barFields = ReflectUtil.getAllFieldsForClass(Bar.class, new Class<?>[] {});
        Assert.assertNotNull(barFields);
        Assert.assertTrue(barFields.contains(Foo.class.getDeclaredField("fooField")));
        Assert.assertTrue(barFields.contains(Foo.class.getDeclaredField("fooIntField")));
        Assert.assertTrue(barFields.contains(Bar.class.getDeclaredField("barField")));
        Assert.assertTrue(barFields.contains(Bar.class.getDeclaredField("barIntField")));
    }

    @Test
    public void getAllFieldsForClassWithBarWithoutFoo() throws NoSuchFieldException, SecurityException {
        Set<Field> barFields = ReflectUtil.getAllFieldsForClass(Bar.class, new Class<?>[] {Foo.class});
        Assert.assertNotNull(barFields);
        Assert.assertTrue(barFields.contains(Bar.class.getDeclaredField("barField")));
        Assert.assertTrue(barFields.contains(Bar.class.getDeclaredField("barIntField")));
    }

    @Test
    public void getAllFieldsForClassWithBazWithoutBar() throws NoSuchFieldException, SecurityException {
        Set<Field> bazFields = ReflectUtil.getAllFieldsForClass(Baz.class, new Class<?>[] {Bar.class});
        Assert.assertNotNull(bazFields);
        Assert.assertTrue(bazFields.contains(Foo.class.getDeclaredField("fooField")));
        Assert.assertTrue(bazFields.contains(Foo.class.getDeclaredField("fooIntField")));
        Assert.assertTrue(bazFields.contains(Baz.class.getDeclaredField("bazField")));
        Assert.assertTrue(bazFields.contains(Baz.class.getDeclaredField("bazIntField")));
    }

}
