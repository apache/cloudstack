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
package org.apache.cloudstack.utils.reflectiontostringbuilderutils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.reflections.ReflectionUtils;

/**
 * Provides methods that ReflectionToStringBuilder does not have yet, as:
 * <br><br>
 * - Reflect a collection of objects, according to object data structure; ReflectionToStringBuilder will execute a toString based on the collection itself,
 * and not on the objects contained in it.<br>
 * - Reflect only selected fields (ReflectionToStringBuilder just has methods to exclude fields).
 */
public class ReflectionToStringBuilderUtils {
    protected static Logger LOGGER = LogManager.getLogger(ReflectionToStringBuilderUtils.class);
    private static final ToStringStyle DEFAULT_STYLE = ToStringStyle.JSON_STYLE;

    /**
    * Default separator to join objects when the parameter <b>object</b> is a Collection.
    */
    private static final String DEFAULT_MULTIPLE_VALUES_SEPARATOR = ",";

    /**
     * Reflect only selected fields of the object into a JSON formatted String.
     * @param object Object to be reflected.
     * @param selectedFields Fields names that must return in JSON.
     * @return If <b>object</b> is null, returns null.<br>
     * If <b>selectedFields</b> is null, returns an empty String.<br>
     * If <b>object</b> is a Collection, returns a JSON array containing the elements, else, returns the object as JSON.<br>
     */
    public static String reflectOnlySelectedFields(Object object, String... selectedFields) {
        String reflection = reflectOnlySelectedFields(object, DEFAULT_STYLE, ",", selectedFields);

        if (reflection == null) {
            return null;
        }

        if (isCollection(object)) {
            return String.format("[%s]", reflection);
        }

        return reflection;
    }

    /**
     * Reflect only selected fields of the object into a formatted String.
     * @param object Object to be reflected.
     * @param style Style to format the string.
     * @param multipleValuesSeparator Separator, in case the object is a Collection.
     * @param selectedFields Fields names that must be reflected.
     * @return If <b>object</b> is null, returns null.<br>
     * If <b>selectedFields</b> is null, returns an empty String.<br>
     * If <b>object</b> is a Collection, returns a <b>style</b> formatted string containing the elements, else, returns the object as the <b>style</b> parameter.<br>
     */
    public static String reflectOnlySelectedFields(Object object, ToStringStyle style, String multipleValuesSeparator, String... selectedFields) {
        String[] nonSelectedFields = getNonSelectedFields(object, selectedFields);
        if (nonSelectedFields == null) {
            return null;
        }

        if (ArrayUtils.isEmpty(nonSelectedFields)) {
            return "";
        }

        String collectionReflection = reflectCollection(object, style, multipleValuesSeparator, nonSelectedFields);
        return collectionReflection != null ? collectionReflection : getReflectedObject(object, style, nonSelectedFields);
    }

    /**
     * Validate if object is not null.
     * @param object
     * @return <b>true</b> if it is not null (valid) or <b>false</b> if it is null (invalid).
     */
    protected static boolean isValidObject(Object object) {
        if (object != null) {
            return true;
        }

        LOGGER.debug("Object is null, not reflecting it.");
        return false;
    }

    /**
     * Similar to {@link ReflectionToStringBuilderUtils#reflectOnlySelectedFields(Object, ToStringStyle, String, String...)}, but excluding the fields instead of reflecting which
     * were selected.<br><br>
     * This method must be called only to {@link Collection}, as it will reflect the objects contained in it.<br>
     * To reflect the Collection itself or other objects, see {@link ReflectionToStringBuilder}.
     * @param object Collection to be reflected.
     * @param style Style to format the string.
     * @param multipleValuesSeparator Separator when joining the objects.
     * @param fieldsToExclude Fields names that must not be reflected.
     * @return If <b>object</b> is null or is not a Collection, returns null.<br>
     * If <b>selectedFields</b> is null, returns an empty String.<br>
     * If <b>object</b> is a Collection, returns a <b>style</b> formatted string containing the not null elements, else, returns the object as the <b>style</b> parameter.<br>
     */
    public static String reflectCollection(Object object, ToStringStyle style, String multipleValuesSeparator, String... fieldsToExclude){
        if (!isCollection(object) || !isValidObject(object)) {
            return null;
        }

        return String.valueOf(((Collection) object).stream()
          .filter(obj -> obj != null)
          .map(obj -> getReflectedObject(obj, style, fieldsToExclude))
          .collect(Collectors.joining(multipleValuesSeparator == null ? DEFAULT_MULTIPLE_VALUES_SEPARATOR : multipleValuesSeparator)));
    }


    /**
     * Similar to {@link ReflectionToStringBuilderUtils#reflectOnlySelectedFields(Object, ToStringStyle, String, String...)}, but reflecting the whole collection.<br><br>
     * This method must be called only to {@link Collection}, as it will reflect the objects contained in it.<br>
     * To reflect the Collection itself or other objects, see {@link ReflectionToStringBuilder}.
     * @param object Collection to be reflected.
     * @return If <b>object</b> is null or is not a Collection, returns null.<br>
     * If <b>object</b> is a Collection, returns a <b>style</b> formatted string containing the not null elements, else, returns the object as the <b>style</b> parameter.<br>
     */
    public static String reflectCollection(Object object){
        String[] excludeFields = null;
        return reflectCollection(object, DEFAULT_STYLE, DEFAULT_MULTIPLE_VALUES_SEPARATOR, excludeFields);
    }

    /**
     * Verify if object is a Collection.
     * @param object
     * @return <b>true</b> if it is a Collection or <b>false</b> if not.
     */
    protected static boolean isCollection(Object object) {
        return object instanceof Collection;
    }

    /**
     * Create a new ReflectionToStringBuilder according to parameters.
     * @param object Object to be reflected.
     * @param style Style to format the string.
     * @param fieldsToExclude Fields names to be removed from the reflection.
     * @return A ReflectionToStringBuilder according to parameters
     */
    protected static String getReflectedObject(Object object, ToStringStyle style, String... fieldsToExclude) {
        return new ReflectionToStringBuilder(object, style).setExcludeFieldNames(fieldsToExclude).toString();
    }

    /**
     * Method to retrieve all fields declared in class, except selected fields. If the object is a collection, it will search for any not null object and reflect it.
     * @param object Object to getReflectionObject.
     * @param selectedFields Fields names that must no return.
     * @return Retrieve all fields declared in class, except selected fields. If the object is a collection, it will search for any not null object and reflect it.
     * @throws SecurityException
     */
    protected static String[] getNonSelectedFields(Object object, String... selectedFields) throws SecurityException {
        if (ArrayUtils.isEmpty(selectedFields)) {
            return new String[0];
        }

        Class<?> classToReflect = getObjectClass(object);
        if (classToReflect == null) {
             return null;
        }

        Set<Field> objectFields = ReflectionUtils.getAllFields(classToReflect);
        List<String> objectFieldsNames = objectFields.stream().map(objectField -> objectField.getName()).collect(Collectors.toList());

        objectFieldsNames.removeAll(Arrays.asList(selectedFields));
        return objectFieldsNames.toArray(new String[objectFieldsNames.size()]);
    }

    /**
     * Get class from object.
     * @param object
     * @return If <b>object</b> is not a Collection, returns its class.<br>
     * if it is a Collection, <b>null</b> if it is an empty Collection or has only null values, else, it will return the class of the Collection data object.
     */
    protected static Class<?> getObjectClass(Object object){
        if (!isValidObject(object)){
            return null;
        }

        if (!isCollection(object)) {
            return object.getClass();
        }

        Collection objectAsCollection = (Collection)object;

        Optional<Object> anyNotNullObject = objectAsCollection.stream().filter(obj -> obj != null).findAny();
        if (anyNotNullObject.isEmpty()) {
            LOGGER.info(String.format("Collection [%s] is empty or has only null values, not reflecting it.", objectAsCollection));
            return null;
        }

        return anyNotNullObject.get().getClass();
    }
}
