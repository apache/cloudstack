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
package com.cloud.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cloud.utils.exception.CloudRuntimeException;
import org.reflections.Reflections;

public class ReflectUtil {
    public static Pair<Class<?>, Field> getAnyField(Class<?> clazz, String fieldName) {
        try {
            return new Pair<Class<?>, Field>(clazz, clazz.getDeclaredField(fieldName));
        } catch (SecurityException e) {
            throw new CloudRuntimeException("How the heck?", e);
        } catch (NoSuchFieldException e) {
            // Do I really want this?  No I don't but what can I do?  It only throws the NoSuchFieldException.
            Class<?> parent = clazz.getSuperclass();
            if (parent != null) {
                return getAnyField(parent, fieldName);
            }
            return null;
        }
    }
    
    public static Method findMethod(Class<?> clazz, String methodName) {
        do {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (methodName.equals(method.getName())) {
                    return method;
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != null);
        return null;
    }

    // Gets all classes with some annotation from a package
    public static Set<Class<?>> getClassesWithAnnotation(Class<? extends Annotation> annotation,
                                                         String[] packageNames) {
        Reflections reflections;
        Set<Class<?>> classes = new HashSet<Class<?>>();
        for(String packageName: packageNames) {
            reflections = new Reflections(packageName);
            classes.addAll(reflections.getTypesAnnotatedWith(annotation));
        }
        return classes;
    }

    // Checks against posted search classes if cmd is async
    public static boolean isCmdClassAsync(Class<?> cmdClass,
                                          Class<?>[] searchClasses) {
        boolean isAsync = false;
        Class<?> superClass = cmdClass;

        while (superClass != null && superClass != Object.class) {
            String superName = superClass.getName();
            for (Class<?> baseClass: searchClasses) {
                if (superName.equals(baseClass.getName())) {
                    isAsync = true;
                    break;
                }
            }
            if (isAsync)
                break;
            superClass = superClass.getSuperclass();
        }
        return isAsync;
    }

    // Returns all fields until a base class for a cmd class
    public static List<Field> getAllFieldsForClass(Class<?> cmdClass,
                                                   Class<?> baseClass) {
        List<Field> fields = new ArrayList<Field>();
        Collections.addAll(fields, cmdClass.getDeclaredFields());
        Class<?> superClass = cmdClass.getSuperclass();
        while (baseClass.isAssignableFrom(superClass)) {
            Field[] superClassFields = superClass.getDeclaredFields();
            if (superClassFields != null)
                Collections.addAll(fields, superClassFields);
            superClass = superClass.getSuperclass();
        }
        return fields;
    }

    // Returns all unique fields except excludeClasses for a cmd class
    public static Set<Field> getAllFieldsForClass(Class<?> cmdClass,
                                                  Class<?>[] excludeClasses) {
        Set<Field> fields = new HashSet<Field>();
        Collections.addAll(fields, cmdClass.getDeclaredFields());
        Class<?> superClass = cmdClass.getSuperclass();

        while (superClass != null && superClass != Object.class) {
            String superName = superClass.getName();
            boolean isNameEqualToSuperName = false;
            for (Class<?> baseClass: excludeClasses)
                if (superName.equals(baseClass.getName()))
                    isNameEqualToSuperName = true;

            if (!isNameEqualToSuperName) {
                Field[] superClassFields = superClass.getDeclaredFields();
                if (superClassFields != null)
                    Collections.addAll(fields, superClassFields);
            }
            superClass = superClass.getSuperclass();
        }
        return fields;
    }

}
