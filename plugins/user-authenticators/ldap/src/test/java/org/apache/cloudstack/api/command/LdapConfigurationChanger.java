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
package org.apache.cloudstack.api.command;

import java.lang.reflect.Field;

interface LdapConfigurationChanger {
    /**
     * sets a possibly not accessible field of the target object.
     * @param target the object to set a hidden fields value in.
     * @param name the name of the field to set.
     * @param o intended value for the field "name"
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    default void setHiddenField(Object target, final String name, final Object o) throws IllegalAccessException, NoSuchFieldException {
        Class<?> klas = target.getClass();
        Field f = getFirstFoundField(name, klas);
        f.setAccessible(true);
        f.set(target, o);
    }

    /**
     * the first field found by this name in the class "klas" or any of it's superclasses except for {@code Object}. Implementers of this interface can decide to also return any field in implemented interfaces or in {@code Object}.
     *
     * @param name of the field to find
     * @param klas class to get a field by name "name" from
     * @return a {@code Field} by the name "name"
     * @throws NoSuchFieldException
     */
    default Field getFirstFoundField(String name, Class<?> klas) throws NoSuchFieldException {
        try {
            return klas.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            Class<?> parent = klas.getSuperclass();
            if(parent.equals(Object.class)) {
                throw e;
            }
            return getFirstFoundField(name, parent);
        }
    }
}
