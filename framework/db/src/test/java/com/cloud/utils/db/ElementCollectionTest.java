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
// under the License.
package com.cloud.utils.db;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

public class ElementCollectionTest extends TestCase {
    static final Logger s_logger = Logger.getLogger(ElementCollectionTest.class);
    ArrayList<String> ar = null;
    List<String> lst = null;
    Collection<String> coll = null;
    String[] array = null;

    public void testArrayList() throws Exception {
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            Class<?> type1 = field.getType();
            Object collection = null;
            if (!type1.isArray()) {
                ParameterizedType type = (ParameterizedType)field.getGenericType();
                Type rawType = type.getRawType();
                Class<?> rawClazz = (Class<?>)rawType;
                if (!Modifier.isAbstract(rawClazz.getModifiers()) && !rawClazz.isInterface() && rawClazz.getConstructors().length != 0 &&
                    rawClazz.getConstructor() != null) {
                    collection = rawClazz.newInstance();
                }

                if (collection == null) {
                    if (Collection.class.isAssignableFrom(rawClazz)) {
                        collection = new ArrayList();
                    } else if (Set.class.isAssignableFrom(rawClazz)) {
                        collection = new HashSet();
                    }
                }
            } else {
                collection = Array.newInstance(String.class, 1);
            }
            field.set(this, collection);
            assert (field.get(this) != null);
        }
    }
}
