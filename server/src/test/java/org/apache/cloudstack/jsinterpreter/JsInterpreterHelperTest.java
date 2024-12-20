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

package org.apache.cloudstack.jsinterpreter;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class JsInterpreterHelperTest {

    @Spy
    private JsInterpreterHelper jsInterpreterHelperSpy;

    @Mock
    private JsonNode jsonNodeMock;

    @Mock
    private Iterator<String> fieldNamesMock;

    @Mock
    private Map.Entry<String, JsonNode> fields;

    public void setupIterateOverJsonTreeTests() {
        JsonNode node = Map.entry("array", jsonNodeMock).getValue();
        Mockito.doReturn(true).when(node).isArray();
        Mockito.doReturn(node).when(fields).getValue();
    }

    @Test
    public void getScriptVariablesTestReturnVariables() {
        String script = "if (account.name == 'test') { domain.id } else { zone.id }";

        Set<String> variables = jsInterpreterHelperSpy.getScriptVariables(script);

        Assert.assertEquals(variables.size(), 3);
        Assert.assertTrue(variables.containsAll(List.of("account.name", "domain.id", "zone.id")));
    }

    @Test
    public void getScriptVariablesTestScriptWithoutVariablesReturnEmptyList() {
        String script = "if (4 < 2) { 3 } else if (3 != 3) { 3 } else { 3 > 3 } while (false) { 3 }";

        Set<String> variables = jsInterpreterHelperSpy.getScriptVariables(script);

        Assert.assertTrue(variables.isEmpty());
    }

    @Test
    public void replaceScriptVariablesTestReturnScriptWithVariablesReplaced() {
        String script = "if (account.name == 'test') { domain.id } else { zone.id }";
        Map<String, String> newVariables = new HashMap<>();
        newVariables.put("account.name", "accountname");
        newVariables.put("domain.id", "domainid");
        newVariables.put("zone.id", "zoneid");

        String newScript = jsInterpreterHelperSpy.replaceScriptVariables(script, newVariables);

        Assert.assertEquals("if (accountname == 'test') { domainid } else { zoneid }", newScript);
    }

    @Test
    public void searchIntoObjectNodesTestNullNodeReturnNull() {
        String fieldName = jsInterpreterHelperSpy.searchIntoObjectNodes(null);

        Assert.assertEquals(null, fieldName);
    }

    @Test
    public void searchIntoObjectNodesTestNonEmptyFieldNamesReturnFieldName() {
        Mockito.doReturn(true, false).when(fieldNamesMock).hasNext();
        Mockito.doReturn("fieldName").when(fieldNamesMock).next();
        Mockito.doReturn(jsonNodeMock).when(jsonNodeMock).get("fieldName");
        Mockito.doNothing().when(jsInterpreterHelperSpy).iterateOverJsonTree((Iterator<Map.Entry<String, JsonNode>>) Mockito.any());
        Mockito.doReturn(fieldNamesMock).when(jsonNodeMock).fieldNames();

        String fieldName = jsInterpreterHelperSpy.searchIntoObjectNodes(jsonNodeMock);

        Assert.assertEquals("fieldName", fieldName);
    }

    @Test
    public void searchIntoObjectNodesTestNameFieldAppendFieldValueToVariable() {
        Mockito.doReturn(true, false).when(fieldNamesMock).hasNext();
        Mockito.doReturn("name").when(fieldNamesMock).next();
        Mockito.doReturn(jsonNodeMock).when(jsonNodeMock).get("name");
        Mockito.doNothing().when(jsInterpreterHelperSpy).iterateOverJsonTree((Iterator<Map.Entry<String, JsonNode>>) Mockito.any());
        Mockito.doReturn(fieldNamesMock).when(jsonNodeMock).fieldNames();

        jsInterpreterHelperSpy.searchIntoObjectNodes(jsonNodeMock);

        Mockito.verify(jsInterpreterHelperSpy, Mockito.times(1)).appendFieldValueToVariable(Mockito.any(), Mockito.any());
    }

    @Test
    public void searchIntoObjectNodesTestPropertyFieldAppendFieldValueToVariable() {
        Mockito.doReturn(true, false).when(fieldNamesMock).hasNext();
        Mockito.doReturn("property").when(fieldNamesMock).next();
        Mockito.doReturn(jsonNodeMock).when(jsonNodeMock).get("property");
        Mockito.doNothing().when(jsInterpreterHelperSpy).iterateOverJsonTree((Iterator<Map.Entry<String, JsonNode>>) Mockito.any());
        Mockito.doReturn(fieldNamesMock).when(jsonNodeMock).fieldNames();

        jsInterpreterHelperSpy.searchIntoObjectNodes(jsonNodeMock);

        Mockito.verify(jsInterpreterHelperSpy, Mockito.times(1)).appendFieldValueToVariable(Mockito.any(), Mockito.any());
    }

    @Test
    public void appendFieldValueToVariableTestPropertyKeyAppendFieldValueAsAttribute() {
        jsInterpreterHelperSpy.setVariable(new StringBuilder("account"));
        jsInterpreterHelperSpy.setVariables(new HashSet<>());
        Mockito.doReturn("name").when(jsonNodeMock).textValue();

        jsInterpreterHelperSpy.appendFieldValueToVariable("property", jsonNodeMock);

        Assert.assertEquals("account.name", jsInterpreterHelperSpy.getVariable().toString());
    }

    @Test
    public void appendFieldValueToVariableTestNameKeyAppendFieldValueAsNewVariable() {
        jsInterpreterHelperSpy.setVariable(new StringBuilder("account"));
        jsInterpreterHelperSpy.setVariables(new HashSet<>());
        Mockito.doReturn("zone").when(jsonNodeMock).textValue();

        jsInterpreterHelperSpy.appendFieldValueToVariable("name", jsonNodeMock);

        Assert.assertEquals("zone", jsInterpreterHelperSpy.getVariable().toString());
    }

    @Test
    public void iterateOverJsonTreeTestMethodsCall() {
        setupIterateOverJsonTreeTests();

        jsInterpreterHelperSpy.iterateOverJsonTree(fields);

        Mockito.verify(jsInterpreterHelperSpy, Mockito.times(1)).iterateOverArrayNodes(Mockito.any());
        Mockito.verify(jsInterpreterHelperSpy, Mockito.times(1)).searchIntoObjectNodes(Mockito.any());
    }

    @Test
    public void iterateOverJsonTreeTestFieldNameNullAndNameKeyAppendFieldValueToVariable() {
        setupIterateOverJsonTreeTests();
        Mockito.doReturn("name").when(fields).getKey();
        Mockito.doNothing().when(jsInterpreterHelperSpy).appendFieldValueToVariable(Mockito.any(), Mockito.any());

        jsInterpreterHelperSpy.iterateOverJsonTree(fields);

        Mockito.verify(jsInterpreterHelperSpy, Mockito.times(1)).appendFieldValueToVariable(Mockito.any(), Mockito.any());
    }

    @Test
    public void iterateOverJsonTreeTestFieldNameNullAndNamePropertyAppendFieldValueToVariable() {
        setupIterateOverJsonTreeTests();
        Mockito.doReturn("property").when(fields).getKey();
        Mockito.doNothing().when(jsInterpreterHelperSpy).appendFieldValueToVariable(Mockito.any(), Mockito.any());

        jsInterpreterHelperSpy.iterateOverJsonTree(fields);

        Mockito.verify(jsInterpreterHelperSpy, Mockito.times(1)).appendFieldValueToVariable(Mockito.any(), Mockito.any());
    }

    @Test
    public void iterateOverArrayNodesTestThreeSizeArrayCallIterateOverJsonTreeThreeTimes() {
        Map<String, JsonNode> fields = new HashMap<>();
        fields.put("field", jsonNodeMock);

        JsonNode root = Mockito.mock(JsonNode.class);
        JsonNode node1 = Mockito.mock(JsonNode.class);
        JsonNode node2 = Mockito.mock(JsonNode.class);
        JsonNode node3 = Mockito.mock(JsonNode.class);
        Mockito.doReturn(fields.entrySet().iterator()).when(node1).fields();
        Mockito.doReturn(fields.entrySet().iterator()).when(node2).fields();
        Mockito.doReturn(fields.entrySet().iterator()).when(node3).fields();

        Map<String, JsonNode> childrenMap = new HashMap<>();
        childrenMap.put("node1", node1);
        childrenMap.put("node2", node2);
        childrenMap.put("node3", node3);

        Map.Entry<String, JsonNode> rootEntry = Map.entry("rootNode", root);

        Mockito.doReturn(node1).when(rootEntry.getValue()).get(0);
        Mockito.doReturn(node2).when(rootEntry.getValue()).get(1);
        Mockito.doReturn(node3).when(rootEntry.getValue()).get(2);
        Mockito.doNothing().when(jsInterpreterHelperSpy).iterateOverJsonTree((Iterator<Map.Entry<String, JsonNode>>) Mockito.any());

        jsInterpreterHelperSpy.iterateOverArrayNodes(rootEntry);

        Mockito.verify(jsInterpreterHelperSpy, Mockito.times(3)).iterateOverJsonTree((Iterator<Map.Entry<String, JsonNode>>) Mockito.any());
    }

    @Test
    public void removeCallFunctionsFromVariableTestTwoCallExpressionsRemoveTwoLastProperties() {
        jsInterpreterHelperSpy.setCallExpressions(2);
        jsInterpreterHelperSpy.setVariable(new StringBuilder("value.osName.toLowerCase().indexOf('windows')"));

        jsInterpreterHelperSpy.removeCallFunctionsFromVariable();

        Assert.assertEquals("value.osName", jsInterpreterHelperSpy.getVariable().toString());
    }
}
