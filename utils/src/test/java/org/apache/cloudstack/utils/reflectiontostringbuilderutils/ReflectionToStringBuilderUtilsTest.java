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

package org.apache.cloudstack.utils.reflectiontostringbuilderutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;
import junit.framework.TestCase;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.reflections.ReflectionUtils;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.w3c.*", "javax.xml.*", "org.xml.*"})
@PrepareForTest(ReflectionToStringBuilderUtils.class)
public class ReflectionToStringBuilderUtilsTest extends TestCase {

    private static final Set<ToStringStyle> TO_STRING_STYLES = new HashSet<>(Arrays.asList(ToStringStyle.DEFAULT_STYLE, ToStringStyle.JSON_STYLE, ToStringStyle.MULTI_LINE_STYLE,
          ToStringStyle.NO_CLASS_NAME_STYLE, ToStringStyle.NO_FIELD_NAMES_STYLE, ToStringStyle.SHORT_PREFIX_STYLE, ToStringStyle.SIMPLE_STYLE));

    private Class<?> classToReflect;
    private List<String> classToReflectFieldsNamesList;
    private String classToReflectRemovedField;
    private String[] classToReflectFieldsNamesArray;

    private static final ToStringStyle DEFAULT_STYLE = ToStringStyle.JSON_STYLE;
    private static final String DEFAULT_MULTIPLE_VALUES_SEPARATOR = ",";

    @Before
    public void setup(){
        classToReflect = String.class;
        classToReflectFieldsNamesList = ReflectionUtils.getAllFields(classToReflect).stream().map(objectField -> objectField.getName()).collect(Collectors.toList());
        classToReflectRemovedField = classToReflectFieldsNamesList.remove(0);
        classToReflectFieldsNamesArray = classToReflectFieldsNamesList.toArray(new String[classToReflectFieldsNamesList.size()]);
    }

    @Test
    public void validateIsValidObjectNullObjectMustReturnFalse(){
        boolean result = ReflectionToStringBuilderUtils.isValidObject(null);
        Assert.assertFalse(result);
    }

    @Test
    public void validateIsValidObjectNotNullObjectMustReturnTrue(){
        boolean result = ReflectionToStringBuilderUtils.isValidObject(new Object());
        Assert.assertTrue(result);
    }

    @Test
    public void validateIsCollectionObjectIsNotACollectionMustReturnFalse(){
        boolean result = ReflectionToStringBuilderUtils.isCollection(new Object());
        Assert.assertFalse(result);
    }

    @Test
    public void validateIsCollectionObjectIsACollectionMustReturnTrue(){
        boolean resultSet = ReflectionToStringBuilderUtils.isCollection(new HashSet<>());
        boolean resultList = ReflectionToStringBuilderUtils.isCollection(new ArrayList<>());
        boolean resultQueue = ReflectionToStringBuilderUtils.isCollection(new PriorityQueue<>());

        Assert.assertTrue(resultSet);
        Assert.assertTrue(resultList);
        Assert.assertTrue(resultQueue);
    }

    @Test
    public void validateGetObjectClassInvalidObjectMustReturnNull(){
        PowerMockito.spy(ReflectionToStringBuilderUtils.class);
        PowerMockito.when(ReflectionToStringBuilderUtils.isValidObject(Mockito.any())).thenReturn(false);

        Class<?> result = ReflectionToStringBuilderUtils.getObjectClass("test");

        Assert.assertNull(result);
    }

    @Test
    public void validateGetObjectClassObjectIsNotACollectionMustReturnObjectClass(){
        Class<?> expectedResult = classToReflect;

        PowerMockito.spy(ReflectionToStringBuilderUtils.class);
        PowerMockito.when(ReflectionToStringBuilderUtils.isValidObject(Mockito.any())).thenReturn(true);
        PowerMockito.when(ReflectionToStringBuilderUtils.isCollection(Mockito.any())).thenReturn(false);

        Class<?> result = ReflectionToStringBuilderUtils.getObjectClass("test");

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void validateGetObjectClassObjectIsAnEmptyCollectionMustReturnNull(){
        PowerMockito.spy(ReflectionToStringBuilderUtils.class);
        PowerMockito.when(ReflectionToStringBuilderUtils.isValidObject(Mockito.any())).thenReturn(true);
        PowerMockito.when(ReflectionToStringBuilderUtils.isCollection(Mockito.any())).thenReturn(true);

        Class<?> result = ReflectionToStringBuilderUtils.getObjectClass(new ArrayList<String>());

        Assert.assertNull(result);
    }

    @Test
    public void validateGetObjectClassObjectIsACollectionWithOnlyNullValuesMustReturnNull(){
        PowerMockito.spy(ReflectionToStringBuilderUtils.class);
        PowerMockito.when(ReflectionToStringBuilderUtils.isValidObject(Mockito.any())).thenReturn(true);
        PowerMockito.when(ReflectionToStringBuilderUtils.isCollection(Mockito.any())).thenReturn(true);

        Class<?> result = ReflectionToStringBuilderUtils.getObjectClass(new ArrayList<String>(Arrays.asList(null, null)));

        Assert.assertNull(result);
    }

    @Test
    public void validateGetObjectClassObjectIsACollectionWithAtLeastOneObjectsMustReturnObjectClass(){
        Class<?> expectedResult = classToReflect;

        PowerMockito.spy(ReflectionToStringBuilderUtils.class);
        PowerMockito.when(ReflectionToStringBuilderUtils.isValidObject(Mockito.any())).thenReturn(true);
        PowerMockito.when(ReflectionToStringBuilderUtils.isCollection(Mockito.any())).thenReturn(true);

        Class<?> result = ReflectionToStringBuilderUtils.getObjectClass(new ArrayList<>(Arrays.asList(null, "test1")));

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void validateGetNonSelectedFieldsEmptyOrNullSelectedFieldsMustReturnEmptyArray(){
        String[] expectedResult = new String[0];
        String[] resultEmpty = ReflectionToStringBuilderUtils.getNonSelectedFields(new String(), new String[0]);
        String[] resultNull = ReflectionToStringBuilderUtils.getNonSelectedFields(new String(), (String[]) null);

        Assert.assertArrayEquals(expectedResult, resultEmpty);
        Assert.assertArrayEquals(expectedResult, resultNull);
    }

    @Test
    public void validateGetNonSelectedFieldsNullObjectClassMustReturnNull(){
        PowerMockito.spy(ReflectionToStringBuilderUtils.class);
        PowerMockito.when(ReflectionToStringBuilderUtils.getObjectClass(Mockito.any())).thenReturn(null);

        String[] result = ReflectionToStringBuilderUtils.getNonSelectedFields(null, "test1", "test2");

        Assert.assertNull(result);
    }

    @Test
    public void validateGetNonSelectedFieldsObjectIsNotACollectionAndValidSelectedFieldsMustReturnNonSelectedFields(){
        String fieldToRemove = classToReflectRemovedField;
        String[] expectedResult = classToReflectFieldsNamesArray;
        Arrays.sort(expectedResult);

        String[] result = ReflectionToStringBuilderUtils.getNonSelectedFields("test", fieldToRemove);
        Arrays.sort(result);
        Assert.assertArrayEquals(expectedResult, result);
    }

    @Test
    public void validateGetNonSelectedFieldsObjectIsACollectionAndValidSelectedFieldsMustReturnNonSelectedFields(){
        String fieldToRemove = classToReflectRemovedField;
        String[] expectedResult = classToReflectFieldsNamesArray;
        Arrays.sort(expectedResult);

        String[] result = ReflectionToStringBuilderUtils.getNonSelectedFields(Arrays.asList("test1", "test2"), fieldToRemove);
        Arrays.sort(result);
        Assert.assertArrayEquals(expectedResult, result);
    }

    @Test
    public void validateGetReflectedObject(){
        String fieldToRemove = classToReflectRemovedField;

        TO_STRING_STYLES.forEach(style -> {
            String objectToReflect = "test";
            String expectedResult = new ReflectionToStringBuilder(objectToReflect, style).setExcludeFieldNames(fieldToRemove).toString();
            String result = ReflectionToStringBuilderUtils.getReflectedObject(objectToReflect, style, fieldToRemove);
            Assert.assertEquals(expectedResult, result);
        });
    }

    @Test
    public void validateReflectCollectionInvalidObjectNorACollectionMustReturnNull() throws Exception{
        String fieldToRemove = classToReflectRemovedField;
        TO_STRING_STYLES.forEach(style -> {
            String resultNull = ReflectionToStringBuilderUtils.reflectCollection(null, style, "-", fieldToRemove);
            String resultNotACollection = ReflectionToStringBuilderUtils.reflectCollection(new Object(), style, "-", fieldToRemove);

            Assert.assertNull(resultNull);
            Assert.assertNull(resultNotACollection);
        });
    }

    @Test
    public void validateReflectCollectionWithOnlyNullValuesMustReturnEmptyString() throws Exception{
        String fieldToRemove = classToReflectRemovedField;
        Set<String> objectToReflect = new HashSet<>(Arrays.asList(null, null));

        TO_STRING_STYLES.forEach(style -> {
            String expectedResult = "";
            String result = ReflectionToStringBuilderUtils.reflectCollection(objectToReflect, style, "-", fieldToRemove);

            Assert.assertEquals(expectedResult, result);
        });
    }

    @Test
    public void validateReflectCollectionValuesMustReturnReflection() throws Exception{
        String fieldToRemove = classToReflectRemovedField;
        Set<String> objectToReflect = new HashSet<>(Arrays.asList(null, "test1", null, "test2"));

        TO_STRING_STYLES.forEach(style -> {
            String expectedResult = String.valueOf(objectToReflect
              .stream()
              .filter(obj -> obj != null)
              .map(obj -> ReflectionToStringBuilderUtils.getReflectedObject(obj, style, "-", fieldToRemove))
              .collect(Collectors.joining("-")));

            String result = ReflectionToStringBuilderUtils.reflectCollection(objectToReflect, style, "-", fieldToRemove);

            Assert.assertEquals(expectedResult, result);
        });
    }

    @Test
    public void validateReflectOnlySelectedFieldsNullNonSelectedFieldsMustReturnNull() throws Exception{
        PowerMockito.spy(ReflectionToStringBuilderUtils.class);
        PowerMockito.when(ReflectionToStringBuilderUtils.getNonSelectedFields(Mockito.any(), Mockito.any())).thenReturn(null);

        TO_STRING_STYLES.forEach(style -> {
            String result = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(null, style, "-");
            Assert.assertNull(result);
        });
    }

    @Test
    public void validateReflectOnlySelectedFieldsEmptyNonSelectedFieldsMustReturnEmptyString() throws Exception{
        String expectedResult = "";

        PowerMockito.spy(ReflectionToStringBuilderUtils.class);
        PowerMockito.when(ReflectionToStringBuilderUtils.getNonSelectedFields(Mockito.any(), Mockito.any())).thenReturn(new String[0]);

        TO_STRING_STYLES.forEach(style -> {
            String result = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(null, style, "-");
            Assert.assertEquals(expectedResult, result);
        });
    }

    @Test
    public void validateReflectOnlySelectedFieldsObjectIsACollectionMustReflectCollection() throws Exception{
        String fieldToRemove = classToReflectRemovedField;
        String expectedResult = "test";

        PowerMockito.spy(ReflectionToStringBuilderUtils.class);
        PowerMockito.when(ReflectionToStringBuilderUtils.getNonSelectedFields(Mockito.any(), Mockito.any())).thenReturn(classToReflectFieldsNamesArray);
        PowerMockito.when(ReflectionToStringBuilderUtils.reflectCollection(Mockito.any(), Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(expectedResult);

        TO_STRING_STYLES.forEach(style -> {
            String result = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(new Object(), style, "-", fieldToRemove);
            Assert.assertEquals(expectedResult, result);
        });
    }

    @Test
    public void validateReflectOnlySelectedFieldsObjectIsNotACollectionMustReflectObject() throws Exception{
        String expectedResult = "test";

        PowerMockito.spy(ReflectionToStringBuilderUtils.class);
        PowerMockito.when(ReflectionToStringBuilderUtils.getNonSelectedFields(Mockito.any(), Mockito.any())).thenReturn(classToReflectFieldsNamesArray);
        PowerMockito.when(ReflectionToStringBuilderUtils.reflectCollection(Mockito.any(), Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(null);

        for (ToStringStyle style : TO_STRING_STYLES){
            PowerMockito.doReturn(expectedResult).when(ReflectionToStringBuilderUtils.class, "getReflectedObject", Mockito.any(), Mockito.any(), Mockito.any());
            String result = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(expectedResult, style, "-", classToReflectFieldsNamesArray);
            Assert.assertEquals(expectedResult, result);
        }
    }

    @Test
    public void validateReflectOnlySelectedFieldsDefaultStyleReflectionNullMustReturnNull(){
        String expectedResult = null;

        PowerMockito.spy(ReflectionToStringBuilderUtils.class);
        PowerMockito.when(ReflectionToStringBuilderUtils.reflectOnlySelectedFields(Mockito.any(), Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(null);

        String result = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(new Object(), (String[]) null);
        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void validateReflectOnlySelectedFieldsDefaultStyleReflectCollectionMustReturnValue(){
        String expectedResult = "[test]";

        PowerMockito.spy(ReflectionToStringBuilderUtils.class);
        PowerMockito.when(ReflectionToStringBuilderUtils.reflectOnlySelectedFields(Mockito.any(), Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn("test");
        PowerMockito.when(ReflectionToStringBuilderUtils.isCollection(Mockito.any())).thenReturn(true);

        String result = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(new Object());
        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void validateReflectOnlySelectedFieldsDefaultStyleReflectMustReturnValue(){
        String expectedResult = "test";

        PowerMockito.spy(ReflectionToStringBuilderUtils.class);
        PowerMockito.when(ReflectionToStringBuilderUtils.reflectOnlySelectedFields(Mockito.any(), Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(expectedResult);
        PowerMockito.when(ReflectionToStringBuilderUtils.isCollection(Mockito.any())).thenReturn(false);

        String result = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(new Object());
        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void reflectCollectionTestCallBaseReflectCollectionMethodWithDefaultParameters() {
        String expected = "test";

        PowerMockito.spy(ReflectionToStringBuilderUtils.class);
        PowerMockito.when(ReflectionToStringBuilderUtils.reflectCollection(Mockito.any(), Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(expected);

        Object object = Mockito.mock(Object.class);
        String result = ReflectionToStringBuilderUtils.reflectCollection(object);

        Assert.assertEquals(expected, result);

        PowerMockito.verifyStatic(ReflectionToStringBuilderUtils.class);
        String[] excludeFields = null;
        ReflectionToStringBuilderUtils.reflectCollection(object, DEFAULT_STYLE, DEFAULT_MULTIPLE_VALUES_SEPARATOR, excludeFields);
    }
}
