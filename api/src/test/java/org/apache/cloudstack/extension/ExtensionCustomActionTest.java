//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package org.apache.cloudstack.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.junit.Test;

import com.cloud.exception.InvalidParameterValueException;

public class ExtensionCustomActionTest {

    @Test
    public void testResourceType() {
        ExtensionCustomAction.ResourceType vmType = ExtensionCustomAction.ResourceType.VirtualMachine;
        assertEquals(com.cloud.vm.VirtualMachine.class, vmType.getAssociatedClass());
    }

    @Test
    public void testParameterTypeSupportsOptions() {
        assertTrue(ExtensionCustomAction.Parameter.Type.STRING.canSupportsOptions());
        assertTrue(ExtensionCustomAction.Parameter.Type.NUMBER.canSupportsOptions());
        assertFalse(ExtensionCustomAction.Parameter.Type.BOOLEAN.canSupportsOptions());
        assertFalse(ExtensionCustomAction.Parameter.Type.DATE.canSupportsOptions());
    }

    @Test
    public void testValidationFormatBaseType() {
        assertEquals(ExtensionCustomAction.Parameter.Type.STRING,
            ExtensionCustomAction.Parameter.ValidationFormat.UUID.getBaseType());
        assertEquals(ExtensionCustomAction.Parameter.Type.STRING,
            ExtensionCustomAction.Parameter.ValidationFormat.EMAIL.getBaseType());
        assertEquals(ExtensionCustomAction.Parameter.Type.STRING,
            ExtensionCustomAction.Parameter.ValidationFormat.PASSWORD.getBaseType());
        assertEquals(ExtensionCustomAction.Parameter.Type.STRING,
            ExtensionCustomAction.Parameter.ValidationFormat.URL.getBaseType());
        assertEquals(ExtensionCustomAction.Parameter.Type.NUMBER,
            ExtensionCustomAction.Parameter.ValidationFormat.DECIMAL.getBaseType());
        assertNull(ExtensionCustomAction.Parameter.ValidationFormat.NONE.getBaseType());
    }

    @Test
    public void testParameterFromMapValid() {
        Map<String, String> map = new HashMap<>();
        map.put(ApiConstants.NAME, "testParam");
        map.put(ApiConstants.TYPE, "STRING");
        map.put(ApiConstants.VALIDATION_FORMAT, "EMAIL");
        map.put(ApiConstants.REQUIRED, "true");
        map.put(ApiConstants.VALUE_OPTIONS, "test@example.com,another@test.com");

        ExtensionCustomAction.Parameter param = ExtensionCustomAction.Parameter.fromMap(map);

        assertEquals("testParam", param.getName());
        assertEquals(ExtensionCustomAction.Parameter.Type.STRING, param.getType());
        assertEquals(ExtensionCustomAction.Parameter.ValidationFormat.EMAIL, param.getValidationFormat());
        assertTrue(param.isRequired());
        assertEquals(2, param.getValueOptions().size());
        assertTrue(param.getValueOptions().contains("test@example.com"));
        assertTrue(param.getValueOptions().contains("another@test.com"));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testParameterFromMapEmptyName() {
        Map<String, String> map = new HashMap<>();
        map.put(ApiConstants.NAME, "");
        map.put(ApiConstants.TYPE, "STRING");

        ExtensionCustomAction.Parameter.fromMap(map);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testParameterFromMapNoType() {
        Map<String, String> map = new HashMap<>();
        map.put(ApiConstants.NAME, "testParam");

        ExtensionCustomAction.Parameter.fromMap(map);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testParameterFromMapInvalidType() {
        Map<String, String> map = new HashMap<>();
        map.put(ApiConstants.NAME, "testParam");
        map.put(ApiConstants.TYPE, "INVALID_TYPE");

        ExtensionCustomAction.Parameter.fromMap(map);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testParameterFromMapInvalidValidationFormat() {
        Map<String, String> map = new HashMap<>();
        map.put(ApiConstants.NAME, "testParam");
        map.put(ApiConstants.TYPE, "STRING");
        map.put(ApiConstants.VALIDATION_FORMAT, "INVALID_FORMAT");

        ExtensionCustomAction.Parameter.fromMap(map);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testParameterFromMapMismatchedTypeAndFormat() {
        Map<String, String> map = new HashMap<>();
        map.put(ApiConstants.NAME, "testParam");
        map.put(ApiConstants.TYPE, "STRING");
        map.put(ApiConstants.VALIDATION_FORMAT, "DECIMAL");

        ExtensionCustomAction.Parameter.fromMap(map);
    }

    @Test
    public void testParameterFromMapWithNumberOptions() {
        Map<String, String> map = new HashMap<>();
        map.put(ApiConstants.NAME, "testParam");
        map.put(ApiConstants.TYPE, "NUMBER");
        map.put(ApiConstants.VALIDATION_FORMAT, "DECIMAL");
        map.put(ApiConstants.VALUE_OPTIONS, "1.5,2.7,3.0");

        ExtensionCustomAction.Parameter param = ExtensionCustomAction.Parameter.fromMap(map);

        assertEquals(ExtensionCustomAction.Parameter.Type.NUMBER, param.getType());
        assertEquals(ExtensionCustomAction.Parameter.ValidationFormat.DECIMAL, param.getValidationFormat());
        assertEquals(3, param.getValueOptions().size());
        assertTrue(param.getValueOptions().contains(1.5f));
        assertTrue(param.getValueOptions().contains(2.7f));
        assertTrue(param.getValueOptions().contains(3.0f));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testParameterFromMapInvalidNumberOptions() {
        Map<String, String> map = new HashMap<>();
        map.put(ApiConstants.NAME, "testParam");
        map.put(ApiConstants.TYPE, "NUMBER");
        map.put(ApiConstants.VALUE_OPTIONS, "1.5,invalid,3.0");

        ExtensionCustomAction.Parameter.fromMap(map);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testParameterFromMapInvalidEmailOptions() {
        Map<String, String> map = new HashMap<>();
        map.put(ApiConstants.NAME, "testParam");
        map.put(ApiConstants.TYPE, "STRING");
        map.put(ApiConstants.VALIDATION_FORMAT, "EMAIL");
        map.put(ApiConstants.VALUE_OPTIONS, "valid@email.com,invalid-email");

        ExtensionCustomAction.Parameter.fromMap(map);
    }

    @Test
    public void testValidatedValueString() {
        ExtensionCustomAction.Parameter param = new ExtensionCustomAction.Parameter(
            "testParam",
            ExtensionCustomAction.Parameter.Type.STRING,
            ExtensionCustomAction.Parameter.ValidationFormat.EMAIL,
            null,
            false
        );

        Object result = param.validatedValue("test@example.com");
        assertEquals("test@example.com", result);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidatedValueInvalidEmail() {
        ExtensionCustomAction.Parameter param = new ExtensionCustomAction.Parameter(
            "testParam",
            ExtensionCustomAction.Parameter.Type.STRING,
            ExtensionCustomAction.Parameter.ValidationFormat.EMAIL,
            null,
            false
        );

        param.validatedValue("invalid-email");
    }

    @Test
    public void testValidatedValueUUID() {
        ExtensionCustomAction.Parameter param = new ExtensionCustomAction.Parameter(
            "testParam",
            ExtensionCustomAction.Parameter.Type.STRING,
            ExtensionCustomAction.Parameter.ValidationFormat.UUID,
            null,
            false
        );

        String validUUID = "550e8400-e29b-41d4-a716-446655440000";
        Object result = param.validatedValue(validUUID);
        assertEquals(validUUID, result);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidatedValueInvalidUUID() {
        ExtensionCustomAction.Parameter param = new ExtensionCustomAction.Parameter(
            "testParam",
            ExtensionCustomAction.Parameter.Type.STRING,
            ExtensionCustomAction.Parameter.ValidationFormat.UUID,
            null,
            false
        );

        param.validatedValue("invalid-uuid");
    }

    @Test
    public void testValidatedValueURL() {
        ExtensionCustomAction.Parameter param = new ExtensionCustomAction.Parameter(
            "testParam",
            ExtensionCustomAction.Parameter.Type.STRING,
            ExtensionCustomAction.Parameter.ValidationFormat.URL,
            null,
            false
        );

        Object result = param.validatedValue("https://example.com");
        assertEquals("https://example.com", result);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidatedValueInvalidURL() {
        ExtensionCustomAction.Parameter param = new ExtensionCustomAction.Parameter(
            "testParam",
            ExtensionCustomAction.Parameter.Type.STRING,
            ExtensionCustomAction.Parameter.ValidationFormat.URL,
            null,
            false
        );

        param.validatedValue("not-a-url");
    }

    @Test
    public void testValidatedValuePassword() {
        ExtensionCustomAction.Parameter param = new ExtensionCustomAction.Parameter(
            "testParam",
            ExtensionCustomAction.Parameter.Type.STRING,
            ExtensionCustomAction.Parameter.ValidationFormat.PASSWORD,
            null,
            false
        );

        Object result = param.validatedValue("mypassword");
        assertEquals("mypassword", result);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidatedValueEmptyPassword() {
        ExtensionCustomAction.Parameter param = new ExtensionCustomAction.Parameter(
            "testParam",
            ExtensionCustomAction.Parameter.Type.STRING,
            ExtensionCustomAction.Parameter.ValidationFormat.PASSWORD,
            null,
            false
        );

        param.validatedValue("   ");
    }

    @Test
    public void testValidatedValueNumber() {
        ExtensionCustomAction.Parameter param = new ExtensionCustomAction.Parameter(
            "testParam",
            ExtensionCustomAction.Parameter.Type.NUMBER,
            ExtensionCustomAction.Parameter.ValidationFormat.NONE,
            null,
            false
        );

        Object result = param.validatedValue("42");
        assertEquals(42, result);
    }

    @Test
    public void testValidatedValueDecimal() {
        ExtensionCustomAction.Parameter param = new ExtensionCustomAction.Parameter(
            "testParam",
            ExtensionCustomAction.Parameter.Type.NUMBER,
            ExtensionCustomAction.Parameter.ValidationFormat.DECIMAL,
            null,
            false
        );

        Object result = param.validatedValue("3.14");
        assertEquals(3.14f, result);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidatedValueInvalidNumber() {
        ExtensionCustomAction.Parameter param = new ExtensionCustomAction.Parameter(
            "testParam",
            ExtensionCustomAction.Parameter.Type.NUMBER,
            ExtensionCustomAction.Parameter.ValidationFormat.NONE,
            null,
            false
        );

        param.validatedValue("not-a-number");
    }

    @Test
    public void testValidatedValueBoolean() {
        ExtensionCustomAction.Parameter param = new ExtensionCustomAction.Parameter(
            "testParam",
            ExtensionCustomAction.Parameter.Type.BOOLEAN,
            ExtensionCustomAction.Parameter.ValidationFormat.NONE,
            null,
            false
        );

        Object result = param.validatedValue("true");
        assertEquals(true, result);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidatedValueInvalidBoolean() {
        ExtensionCustomAction.Parameter param = new ExtensionCustomAction.Parameter(
            "testParam",
            ExtensionCustomAction.Parameter.Type.BOOLEAN,
            ExtensionCustomAction.Parameter.ValidationFormat.NONE,
            null,
            false
        );

        Object result = param.validatedValue("maybe");
    }

    @Test
    public void testValidatedValueWithOptions() {
        List<Object> options = Arrays.asList("option1", "option2", "option3");
        ExtensionCustomAction.Parameter param = new ExtensionCustomAction.Parameter(
            "testParam",
            ExtensionCustomAction.Parameter.Type.STRING,
            ExtensionCustomAction.Parameter.ValidationFormat.NONE,
            options,
            false
        );

        Object result = param.validatedValue("option2");
        assertEquals("option2", result);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidatedValueNotInOptions() {
        List<Object> options = Arrays.asList("option1", "option2", "option3");
        ExtensionCustomAction.Parameter param = new ExtensionCustomAction.Parameter(
            "testParam",
            ExtensionCustomAction.Parameter.Type.STRING,
            ExtensionCustomAction.Parameter.ValidationFormat.NONE,
            options,
            false
        );

        param.validatedValue("option4");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidatedValueEmpty() {
        ExtensionCustomAction.Parameter param = new ExtensionCustomAction.Parameter(
            "testParam",
            ExtensionCustomAction.Parameter.Type.STRING,
            ExtensionCustomAction.Parameter.ValidationFormat.NONE,
            null,
            false
        );

        param.validatedValue("");
    }

    @Test
    public void testValidateParameterValues() {
        List<ExtensionCustomAction.Parameter> paramDefs = Arrays.asList(
            new ExtensionCustomAction.Parameter("required1", ExtensionCustomAction.Parameter.Type.STRING,
                ExtensionCustomAction.Parameter.ValidationFormat.NONE, null, true),
            new ExtensionCustomAction.Parameter("required2", ExtensionCustomAction.Parameter.Type.NUMBER,
                ExtensionCustomAction.Parameter.ValidationFormat.NONE, null, true),
            new ExtensionCustomAction.Parameter("optional", ExtensionCustomAction.Parameter.Type.STRING,
                ExtensionCustomAction.Parameter.ValidationFormat.NONE, null, false)
        );

        Map<String, String> suppliedValues = new HashMap<>();
        suppliedValues.put("required1", "value1");
        suppliedValues.put("required2", "42");
        suppliedValues.put("optional", "optionalValue");

        Map<String, Object> result = ExtensionCustomAction.Parameter.validateParameterValues(paramDefs, suppliedValues);

        assertEquals("value1", result.get("required1"));
        assertEquals(42, result.get("required2"));
        assertEquals("optionalValue", result.get("optional"));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateParameterValuesMissingRequired() {
        List<ExtensionCustomAction.Parameter> paramDefs = Arrays.asList(
            new ExtensionCustomAction.Parameter("required1", ExtensionCustomAction.Parameter.Type.STRING,
                ExtensionCustomAction.Parameter.ValidationFormat.NONE, null, true)
        );

        Map<String, String> suppliedValues = new HashMap<>();

        ExtensionCustomAction.Parameter.validateParameterValues(paramDefs, suppliedValues);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateParameterValuesEmptyRequired() {
        List<ExtensionCustomAction.Parameter> paramDefs = Arrays.asList(
            new ExtensionCustomAction.Parameter("required1", ExtensionCustomAction.Parameter.Type.STRING,
                ExtensionCustomAction.Parameter.ValidationFormat.NONE, null, true)
        );

        Map<String, String> suppliedValues = new HashMap<>();
        suppliedValues.put("required1", "   ");

        ExtensionCustomAction.Parameter.validateParameterValues(paramDefs, suppliedValues);
    }

    @Test
    public void testValidateParameterValuesNullSupplied() {
        List<ExtensionCustomAction.Parameter> paramDefs = Arrays.asList(
            new ExtensionCustomAction.Parameter("optional", ExtensionCustomAction.Parameter.Type.STRING,
                ExtensionCustomAction.Parameter.ValidationFormat.NONE, null, false)
        );

        Map<String, Object> result = ExtensionCustomAction.Parameter.validateParameterValues(paramDefs, null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testJsonSerializationDeserialization() {
        List<ExtensionCustomAction.Parameter> originalParams = Arrays.asList(
            new ExtensionCustomAction.Parameter("param1", ExtensionCustomAction.Parameter.Type.STRING,
                ExtensionCustomAction.Parameter.ValidationFormat.EMAIL, Arrays.asList("test@example.com"), true),
            new ExtensionCustomAction.Parameter("param2", ExtensionCustomAction.Parameter.Type.NUMBER,
                ExtensionCustomAction.Parameter.ValidationFormat.DECIMAL, Arrays.asList(1.5f, 2.7f), false)
        );

        String json = ExtensionCustomAction.Parameter.toJsonFromList(originalParams);
        List<ExtensionCustomAction.Parameter> deserializedParams = ExtensionCustomAction.Parameter.toListFromJson(json);

        assertEquals(originalParams.size(), deserializedParams.size());
        assertEquals(originalParams.get(0).getName(), deserializedParams.get(0).getName());
        assertEquals(originalParams.get(0).getType(), deserializedParams.get(0).getType());
        assertEquals(originalParams.get(0).getValidationFormat(), deserializedParams.get(0).getValidationFormat());
        assertEquals(originalParams.get(0).isRequired(), deserializedParams.get(0).isRequired());
        assertEquals(originalParams.get(0).getValueOptions(), deserializedParams.get(0).getValueOptions());
    }

    @Test
    public void testToString() {
        ExtensionCustomAction.Parameter param = new ExtensionCustomAction.Parameter(
            "testParam",
            ExtensionCustomAction.Parameter.Type.STRING,
            ExtensionCustomAction.Parameter.ValidationFormat.EMAIL,
            null,
            true
        );

        String result = param.toString();
        assertTrue(result.contains("testParam"));
        assertTrue(result.contains("STRING"));
        assertTrue(result.contains("true"));
    }
}
