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
package org.apache.cloudstack.context;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.response.ExceptionResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.dc.DataCenter;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class ErrorMessageResolverTest {

    @Mock
    CallContext callContextMock;

    MockedStatic<CallContext> callContextMocked;
    MockedStatic<PropertiesUtil> propertiesUtilMocked;

    Path tmpFile;

    @Before
    public void setup() {
        callContextMocked = Mockito.mockStatic(CallContext.class);
        callContextMocked.when(CallContext::current).thenReturn(callContextMock);
        propertiesUtilMocked = Mockito.mockStatic(PropertiesUtil.class);
        propertiesUtilMocked.when(() -> PropertiesUtil.findConfigFile(anyString())).thenReturn(null);
    }

    @After
    public void tearDown() throws Exception {
        callContextMocked.close();
        propertiesUtilMocked.close();
        if (tmpFile != null) {
            try {
                Files.deleteIfExists(tmpFile);
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    public void getVariableNamesInErrorKey_shouldReturnEmptyListForTemplateWithoutVariables() {
        String template = "This is a template without variables.";
        List<String> result = ErrorMessageResolver.getVariableNamesInErrorKey(template);
        Assert.assertTrue("Result should be an empty list", result.isEmpty());
    }

    @Test
    public void getVariableNamesInErrorKey_shouldExtractSingleVariable() {
        String template = "This is a template with one variable: {{variable}}.";
        List<String> result = ErrorMessageResolver.getVariableNamesInErrorKey(template);
        Assert.assertEquals("Result should contain one variable", 1, result.size());
        Assert.assertEquals("Variable name should be 'variable'", "variable", result.get(0));
    }

    @Test
    public void getVariableNamesInErrorKey_shouldExtractMultipleVariables() {
        String template = "This template has {{var1}} and {{var2}}.";
        List<String> result = ErrorMessageResolver.getVariableNamesInErrorKey(template);
        Assert.assertEquals("Result should contain two variables", 2, result.size());
        Assert.assertEquals("First variable name should be 'var1'", "var1", result.get(0));
        Assert.assertEquals("Second variable name should be 'var2'", "var2", result.get(1));
    }

    @Test
    public void getVariableNamesInErrorKey_shouldIgnoreMalformedVariables() {
        String template = "This template has {{var1} and {{var2}}.";
        List<String> result = ErrorMessageResolver.getVariableNamesInErrorKey(template);
        Assert.assertEquals("Result should contain one valid variable", 1, result.size());
        Assert.assertEquals("Variable name should be 'var2'", "var2", result.get(0));
    }

    @Test
    public void getVariableNamesInErrorKey_shouldHandleEmptyTemplate() {
        String template = "";
        List<String> result = ErrorMessageResolver.getVariableNamesInErrorKey(template);
        Assert.assertTrue("Result should be an empty list", result.isEmpty());
    }

    @Test
    public void getVariableNamesInErrorKey_shouldHandleTemplateWithOnlyVariables() {
        String template = "{{var1}}{{var2}}{{var3}}";
        List<String> result = ErrorMessageResolver.getVariableNamesInErrorKey(template);
        Assert.assertEquals("Result should contain three variables", 3, result.size());
        Assert.assertEquals("First variable name should be 'var1'", "var1", result.get(0));
        Assert.assertEquals("Second variable name should be 'var2'", "var2", result.get(1));
        Assert.assertEquals("Third variable name should be 'var3'", "var3", result.get(2));
    }

    @Test
    public void getCombinedMetadataFromErrorTemplate_shouldReturnMetadataWhenNoVariablesInTemplate() {
        String template = "This is a template without variables.";
        Map<String, Object> metadata = Map.of("key1", "value1");
        Map<String, Object> result = ErrorMessageResolver.getCombinedMetadataFromErrorTemplate(template, metadata);
        Assert.assertEquals("Result should match the input metadata", metadata, result);
    }

    @Test
    public void getCombinedMetadataFromErrorTemplate_shouldReturnEmptyMapWhenContextMetadataIsEmpty() {
        String template = "This template has {{var1}}.";
        Map<String, Object> metadata = Map.of();
        when(callContextMock.getErrorContextParameters()).thenReturn(Map.of());
        Map<String, Object> result = ErrorMessageResolver.getCombinedMetadataFromErrorTemplate(template, metadata);
        Assert.assertTrue("Result should be an empty map", result.isEmpty());
    }

    @Test
    public void getCombinedMetadataFromErrorTemplate_shouldCombineContextAndProvidedMetadata() {
        String template = "This template has {{var1}} and {{var2}}.";
        Map<String, Object> metadata = Map.of("key1", "value1");
        when(callContextMock.getErrorContextParameters()).thenReturn(Map.of("var1", "valueVar1", "var2", "valueVar2"));
        Map<String, Object> result = ErrorMessageResolver.getCombinedMetadataFromErrorTemplate(template, metadata);
        Assert.assertEquals("Result should contain combined metadata", 3, result.size());
        Assert.assertEquals("Result should contain context metadata for var1", "valueVar1", result.get("var1"));
        Assert.assertEquals("Result should contain context metadata for var2", "valueVar2", result.get("var2"));
        Assert.assertEquals("Result should contain provided metadata", "value1", result.get("key1"));
    }

    @Test
    public void getCombinedMetadataFromErrorTemplate_shouldIgnoreVariablesNotInContextMetadata() {
        String template = "This template has {{var1}} and {{var2}}.";
        Map<String, Object> metadata = Map.of("key1", "value1");
        when(callContextMock.getErrorContextParameters()).thenReturn(Map.of("var1", "valueVar1"));
        Map<String, Object> result = ErrorMessageResolver.getCombinedMetadataFromErrorTemplate(template, metadata);
        Assert.assertEquals("Result should contain combined metadata", 2, result.size());
        Assert.assertEquals("Result should contain context metadata for var1", "valueVar1", result.get("var1"));
        Assert.assertEquals("Result should contain provided metadata", "value1", result.get("key1"));
    }

    @Test
    public void getCombinedMetadataFromErrorTemplate_shouldReturnProvidedMetadataWhenTemplateIsEmpty() {
        String template = "";
        Map<String, Object> metadata = Map.of("key1", "value1");
        Map<String, Object> result = ErrorMessageResolver.getCombinedMetadataFromErrorTemplate(template, metadata);
        Assert.assertEquals("Result should match the input metadata", metadata, result);
    }

    @Test
    public void getStringMap_shouldReturnEmptyMapWhenMetadataIsEmpty() {
        Map<String, Object> metadata = Map.of();
        Map<String, String> result = ErrorMessageResolver.getStringMap(metadata);
        Assert.assertTrue("Result should be an empty map", result.isEmpty());
    }

    @Test
    public void getStringMap_shouldConvertAllMetadataValuesToStrings() {
        Map<String, Object> metadata = Map.of("key1", 123, "key2", true, "key3", "value");
        Map<String, String> result = ErrorMessageResolver.getStringMap(metadata);
        Assert.assertEquals("Result should contain all keys", 3, result.size());
        Assert.assertEquals("Value for key1 should be '123'", "123", result.get("key1"));
        Assert.assertEquals("Value for key2 should be 'true'", "true", result.get("key2"));
        Assert.assertEquals("Value for key3 should be 'value'", "value", result.get("key3"));
    }

    @Test
    public void getStringMap_shouldHandleNullValuesInMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("key1", null);
        metadata.put("key2", "value");
        Map<String, String> result = ErrorMessageResolver.getStringMap(metadata);
        Assert.assertEquals("Result should contain all keys", 2, result.size());
        Assert.assertNull("Value for key1 should be null", result.get("key1"));
        Assert.assertEquals("Value for key2 should be 'value'", "value", result.get("key2"));
    }

    @Test
    public void getStringMap_shouldReturnEmptyMapWhenMetadataIsNull() {
        Map<String, String> result = ErrorMessageResolver.getStringMap(null);
        Assert.assertTrue("Result should be an empty map", result.isEmpty());
    }

    @Test
    public void getMetadataObjectStringValue_shouldReturnNullWhenObjectIsNull() {
        Assert.assertNull(ErrorMessageResolver.getMetadataObjectStringValue(null));
    }

    @Test
    public void getMetadataObjectStringValue_shouldReturnUuidWhenNameIsUnavailable() {
        Identity identityMock = Mockito.mock(Identity.class);
        when(identityMock.getUuid()).thenReturn("uuid-1234");
        Assert.assertEquals("uuid-1234", ErrorMessageResolver.getMetadataObjectStringValue(identityMock));
    }

    @Test
    public void getMetadataObjectStringValue_shouldReturnNameWhenAvailable() {
        DataCenter identityMock = Mockito.mock(DataCenter.class);
        when(identityMock.getUuid()).thenReturn("uuid-1234");
        when(identityMock.getName()).thenReturn("TestName");
        Assert.assertEquals("'TestName'", ErrorMessageResolver.getMetadataObjectStringValue(identityMock));
    }

    @Test
    public void getMetadataObjectStringValue_shouldIncludeIdAndUuidForRootAdmin() {
        DataCenter internalIdentityMock = Mockito.mock(DataCenter.class);
        when(internalIdentityMock.getUuid()).thenReturn("uuid-5678");
        if (ErrorMessageResolver.INCLUDE_METADATA_ID_IN_MESSAGE) {
            when(internalIdentityMock.getId()).thenReturn(42L);
        }
        when(internalIdentityMock.getName()).thenReturn("AdminName");
        when(CallContext.current().isCallingAccountRootAdmin()).thenReturn(true);
        String expected = String.format("'AdminName' (%sUUID: uuid-5678)", ErrorMessageResolver.INCLUDE_METADATA_ID_IN_MESSAGE ? "ID: 42, " : "");
        Assert.assertEquals(expected, ErrorMessageResolver.getMetadataObjectStringValue(internalIdentityMock));
    }

    @Test
    public void getMetadataObjectStringValue_shouldFallbackToToStringWhenNameAndUuidAreUnavailable() {
        Object obj = new Object();
        Assert.assertEquals(obj.toString(), ErrorMessageResolver.getMetadataObjectStringValue(obj));
    }

    @Test
    public void getMetadataObjectStringValue_shouldReturnNameOnlyForNonRootAdmin() {
        DataCenter internalIdentityMock = Mockito.mock(DataCenter.class);
        when(internalIdentityMock.getName()).thenReturn("UserName");
        when(CallContext.current().isCallingAccountRootAdmin()).thenReturn(false);
        Assert.assertEquals("'UserName'", ErrorMessageResolver.getMetadataObjectStringValue(internalIdentityMock));
    }

    @Test
    public void expand_shouldReturnTemplateWhenMetadataIsEmpty() {
        String template = "This is a template with no placeholders.";
        Map<String, String> metadata = Map.of();
        String result = ErrorMessageResolver.expand(template, metadata);
        Assert.assertEquals("This is a template with no placeholders.", result);
    }

    @Test
    public void expand_shouldReplaceSinglePlaceholderWithMetadataValue() {
        String template = "Hello, {{name}}!";
        Map<String, String> metadata = Map.of("name", "World");
        String result = ErrorMessageResolver.expand(template, metadata);
        Assert.assertEquals("Hello, World!", result);
    }

    @Test
    public void expand_shouldReplaceMultiplePlaceholdersWithMetadataValues() {
        String template = "Hello, {{name}}! Today is {{day}}.";
        Map<String, String> metadata = Map.of("name", "Alice", "day", "Monday");
        String result = ErrorMessageResolver.expand(template, metadata);
        Assert.assertEquals("Hello, Alice! Today is Monday.", result);
    }

    @Test
    public void expand_shouldIgnorePlaceholdersWithoutMatchingMetadata() {
        String template = "Hello, {{name}}! Your age is {{age}}.";
        Map<String, String> metadata = Map.of("name", "Bob");
        String result = ErrorMessageResolver.expand(template, metadata);
        Assert.assertEquals("Hello, Bob! Your age is {{age}}.", result);
    }

    @Test
    public void expand_shouldHandleNullMetadataValues() {
        String template = "Hello, {{name}}!";
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("name", null);
        String result = ErrorMessageResolver.expand(template, metadata);
        Assert.assertEquals("Hello, {{name}}!", result);
    }

    @Test
    public void expand_shouldReturnTemplateWhenMetadataIsNull() {
        String template = "This is a template with no placeholders.";
        String result = ErrorMessageResolver.expand(template, null);
        Assert.assertEquals("This is a template with no placeholders.", result);
    }

    @Test
    public void expand_shouldHandleTemplateWithNoPlaceholders() {
        String template = "This template has no placeholders.";
        Map<String, String> metadata = Map.of("key", "value");
        String result = ErrorMessageResolver.expand(template, metadata);
        Assert.assertEquals("This template has no placeholders.", result);
    }

    @Test
    public void getMessage_shouldReturnErrorKeyWhenTemplateIsNull() {
        String errorKey = "error.key";
        String result = ErrorMessageResolver.getMessage(errorKey, Map.of());
        Assert.assertEquals(errorKey, result);
    }

    private void createTempFileWithTemplate(String errorKey, String template) {
        try {
            tmpFile = Files.createTempFile("error-template-", ".txt");
            Files.writeString(tmpFile, String.format("{ \"%s\": \"%s\" }", errorKey, template));
            propertiesUtilMocked.when(() -> PropertiesUtil.findConfigFile(anyString()))
                    .thenReturn(tmpFile.toFile());
        } catch (IOException e) {
            Assert.fail("Failed to create temporary file for testing: " + e.getMessage());
        }
    }

    @Test
    public void getMessage_shouldReturnExpandedMessageWhenTemplateExists() {
        String errorKey = "error.key";
        String template = "Error occurred: {{field}}";
        Map<String, Object> metadata = Map.of("field", "value");
        createTempFileWithTemplate(errorKey, template);
        when(callContextMock.getErrorContextParameters()).thenReturn(Map.of());
        String result = ErrorMessageResolver.getMessage(errorKey, metadata);
        Assert.assertEquals("Error occurred: value", result);
    }

    @Test
    public void getMessage_shouldHandleEmptyMetadata() {
        String errorKey = "error.key";
        String template = "Error occurred.";
        createTempFileWithTemplate(errorKey, template);
        String result = ErrorMessageResolver.getMessage(errorKey, Map.of());
        Assert.assertEquals("Error occurred.", result);
    }

    @Test
    public void getMessage_shouldHandleNullMetadata() {
        String errorKey = "error.key";
        String template = "Error occurred.";
        createTempFileWithTemplate(errorKey, template);
        String result = ErrorMessageResolver.getMessage(errorKey, null);
        Assert.assertEquals("Error occurred.", result);
    }

    @Test
    public void getMessage_shouldCombineContextAndProvidedMetadata() {
        String errorKey = "error.key";
        String template = "Error in {{field1}} and {{field2}}.";
        Map<String, Object> metadata = Map.of("field1", "value1");
        createTempFileWithTemplate(errorKey, template);
        when(callContextMock.getErrorContextParameters()).thenReturn(Map.of("field2", "value2"));
        String result = ErrorMessageResolver.getMessage(errorKey, metadata);
        Assert.assertEquals("Error in value1 and value2.", result);
    }

    @Test
    public void updateExceptionResponse_shouldSetErrorTextAndMetadataWhenKeyAndTemplateExist() {
        ExceptionResponse response = new ExceptionResponse();
        CloudRuntimeException cre = Mockito.mock(CloudRuntimeException.class);
        String key = "error.key";
        String template = "Error occurred: {{field}}";
        Map<String, Object> metadata = Map.of("field", "value");

        when(cre.getMessageKey()).thenReturn(key);
        when(cre.getMetadata()).thenReturn(metadata);
        createTempFileWithTemplate(key, template);

        ErrorMessageResolver.updateExceptionResponse(response, cre);

        Assert.assertEquals(key, response.getErrorTextKey());
        Assert.assertEquals("Error occurred: value", response.getErrorText());
        Assert.assertEquals(Map.of("field", "value"), response.getErrorMetadata());
    }

    @Test
    public void updateExceptionResponse_shouldSetErrorTextKeyOnlyWhenTemplateDoesNotExist() {
        ExceptionResponse response = new ExceptionResponse();
        CloudRuntimeException cre = Mockito.mock(CloudRuntimeException.class);
        String key = "error.key";
        Map<String, Object> metadata = Map.of("field", "value");

        when(cre.getMessageKey()).thenReturn(key);
        when(cre.getMetadata()).thenReturn(metadata);

        ErrorMessageResolver.updateExceptionResponse(response, cre);

        Assert.assertEquals(key, response.getErrorTextKey());
        Assert.assertEquals(key, response.getErrorText());
        Assert.assertEquals(Map.of("field", "value"), response.getErrorMetadata());
    }

    @Test
    public void updateExceptionResponse_shouldHandleNullKeyAndCauseIsNotInvalidParameterValueException() {
        ExceptionResponse response = new ExceptionResponse();
        String originalErrorText = response.getErrorText();
        CloudRuntimeException cre = Mockito.mock(CloudRuntimeException.class);

        when(cre.getMessageKey()).thenReturn(null);
        when(cre.getCause()).thenReturn(new RuntimeException());

        ErrorMessageResolver.updateExceptionResponse(response, cre);

        Assert.assertNull(response.getErrorTextKey());
        Assert.assertNull(response.getErrorMetadata());
        Assert.assertEquals(originalErrorText, response.getErrorText());
    }

    @Test
    public void updateExceptionResponse_shouldUseCauseMetadataWhenKeyIsNullAndCauseIsInvalidParameterValueException() {
        ExceptionResponse response = new ExceptionResponse();
        InvalidParameterValueException cause = Mockito.mock(InvalidParameterValueException.class);
        CloudRuntimeException cre = Mockito.mock(CloudRuntimeException.class);
        String key = "error.key";
        String template = "Error occurred: {{field}}";
        Map<String, Object> metadata = Map.of("field", "value");

        when(cre.getMessageKey()).thenReturn(null);
        when(cre.getCause()).thenReturn(cause);
        when(cause.getMessageKey()).thenReturn(key);
        when(cause.getMetadata()).thenReturn(metadata);
        createTempFileWithTemplate(key, template);

        ErrorMessageResolver.updateExceptionResponse(response, cre);

        Assert.assertEquals(key, response.getErrorTextKey());
        Assert.assertEquals("Error occurred: value", response.getErrorText());
        Assert.assertEquals(Map.of("field", "value"), response.getErrorMetadata());
    }

    @Test
    public void updateExceptionResponse_shouldHandleNullMetadata() {
        ExceptionResponse response = new ExceptionResponse();
        CloudRuntimeException cre = Mockito.mock(CloudRuntimeException.class);
        String key = "error.key";
        String template = "Error occurred.";

        when(cre.getMessageKey()).thenReturn(key);
        when(cre.getMetadata()).thenReturn(null);
        createTempFileWithTemplate(key, template);

        ErrorMessageResolver.updateExceptionResponse(response, cre);

        Assert.assertEquals(key, response.getErrorTextKey());
        Assert.assertEquals("Error occurred.", response.getErrorText());
        Assert.assertTrue(response.getErrorMetadata().isEmpty());
    }
}
