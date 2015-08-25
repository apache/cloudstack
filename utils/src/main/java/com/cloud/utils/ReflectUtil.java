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

import static java.beans.Introspector.getBeanInfo;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.ClasspathHelper;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import com.google.common.collect.ImmutableSet;

import com.cloud.utils.exception.CloudRuntimeException;

public class ReflectUtil {

    private static final Logger s_logger = Logger.getLogger(ReflectUtil.class);
    private static final Logger logger = Logger.getLogger(Reflections.class);

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

    // Gets all classes with some annotation from a package
    public static Set<Class<?>> getClassesWithAnnotation(Class<? extends Annotation> annotation, String[] packageNames) {
        Reflections reflections;
        Set<Class<?>> classes = new HashSet<Class<?>>();
        ConfigurationBuilder builder=new ConfigurationBuilder();
        for (String packageName : packageNames) {
             builder.addUrls(ClasspathHelper.forPackage(packageName));
        }
        builder.setScanners(new SubTypesScanner(),new TypeAnnotationsScanner());
        reflections = new Reflections(builder);
        classes.addAll(reflections.getTypesAnnotatedWith(annotation));
        return classes;
    }

    // Checks against posted search classes if cmd is async
    public static boolean isCmdClassAsync(Class<?> cmdClass, Class<?>[] searchClasses) {
        boolean isAsync = false;
        Class<?> superClass = cmdClass;

        while (superClass != null && superClass != Object.class) {
            String superName = superClass.getName();
            for (Class<?> baseClass : searchClasses) {
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
    public static List<Field> getAllFieldsForClass(Class<?> cmdClass, Class<?> baseClass) {
        List<Field> fields = new ArrayList<Field>();
        Collections.addAll(fields, cmdClass.getDeclaredFields());
        Class<?> superClass = cmdClass.getSuperclass();
        while (baseClass.isAssignableFrom(superClass) && baseClass != superClass) {
            Field[] superClassFields = superClass.getDeclaredFields();
            if (superClassFields != null)
                Collections.addAll(fields, superClassFields);
            superClass = superClass.getSuperclass();
        }
        return fields;
    }

    /**
     * Returns all unique fields except excludeClasses for a cmd class
     * @param cmdClass    the class in which fields should be collected
     * @param excludeClasses the classes whose fields must be ignored
     * @return list of fields
     */
    public static Set<Field> getAllFieldsForClass(Class<?> cmdClass, Class<?>[] excludeClasses) {
        Set<Field> fields = new HashSet<Field>();
        Collections.addAll(fields, cmdClass.getDeclaredFields());
        Class<?> superClass = cmdClass.getSuperclass();

        while (superClass != null && superClass != Object.class) {
            String superName = superClass.getName();
            boolean isNameEqualToSuperName = false;
            for (Class<?> baseClass : excludeClasses) {
                if (superName.equals(baseClass.getName())) {
                    isNameEqualToSuperName = true;
                }
            }

            if (!isNameEqualToSuperName) {
                Field[] superClassFields = superClass.getDeclaredFields();
                if (superClassFields != null) {
                    Collections.addAll(fields, superClassFields);
                }
            }
            superClass = superClass.getSuperclass();
        }
        return fields;
    }

    public static List<String> flattenProperties(final Object target, final Class<?> clazz) {
        return flattenPropeties(target, clazz, "class");
    }

    public static List<String> flattenPropeties(final Object target, final Class<?> clazz, final String... excludedProperties) {
        return flattenProperties(target, clazz, ImmutableSet.copyOf(excludedProperties));
    }

    private static List<String> flattenProperties(final Object target, final Class<?> clazz, final ImmutableSet<String> excludedProperties) {

        assert clazz != null;

        if (target == null) {
            return emptyList();
        }

        assert clazz.isAssignableFrom(target.getClass());

        try {

            final BeanInfo beanInfo = getBeanInfo(clazz);
            final PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();

            final List<String> serializedProperties = new ArrayList<String>();
            for (final PropertyDescriptor descriptor : descriptors) {

                if (excludedProperties.contains(descriptor.getName())) {
                    continue;
                }

                serializedProperties.add(descriptor.getName());
                final Object value = descriptor.getReadMethod().invoke(target);
                serializedProperties.add(value != null ? value.toString() : "null");

            }

            return unmodifiableList(serializedProperties);

        } catch (IntrospectionException e) {
            s_logger.warn("Ignored IntrospectionException when serializing class " + target.getClass().getCanonicalName(), e);
        } catch (IllegalArgumentException e) {
            s_logger.warn("Ignored IllegalArgumentException when serializing class " + target.getClass().getCanonicalName(), e);
        } catch (IllegalAccessException e) {
            s_logger.warn("Ignored IllegalAccessException when serializing class " + target.getClass().getCanonicalName(), e);
        } catch (InvocationTargetException e) {
            s_logger.warn("Ignored InvocationTargetException when serializing class " + target.getClass().getCanonicalName(), e);
        }

        return emptyList();

    }

    public static String getEntityName(Class clz){
        if(clz == null)
            return null;

        String entityName = clz.getName();
        int index = entityName.lastIndexOf(".");
        if (index != -1) {
            return entityName.substring(index + 1);
        }else{
            return entityName;
        }
    }

}
