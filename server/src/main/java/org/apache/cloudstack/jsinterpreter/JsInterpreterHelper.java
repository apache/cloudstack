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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.openjdk.nashorn.api.scripting.ScriptUtils;
import org.openjdk.nashorn.internal.runtime.Context;
import org.openjdk.nashorn.internal.runtime.ErrorManager;
import org.openjdk.nashorn.internal.runtime.options.Options;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsInterpreterHelper {
    private final Logger logger = LogManager.getLogger(getClass());

    private static final String NAME = "name";
    private static final String PROPERTY = "property";
    private static final String TYPE = "type";
    private static final String CALL_EXPRESSION = "CallExpression";

    private int callExpressions;

    private StringBuilder variable;

    private Set<String> variables;

    /**
     * Returns all variables from the given script.
     *
     * @param script the script to extract the variables.
     * @return A {@link Set<String>} containing all variables in the script.
     */
    public Set<String> getScriptVariables(String script) {
        String parseTree = getScriptAsJsonTree(script);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = null;
        variables = new HashSet<>();
        variable = new StringBuilder();

        try {
            jsonNode = mapper.readTree(parseTree);
        } catch (JsonProcessingException e) {
            logger.error("Unable to create the script JSON tree due to: [{}].", e.getMessage(), e);
        }

        logger.trace("Searching script variables from [{}].", script);
        iterateOverJsonTree(jsonNode.fields());

        if (StringUtils.isNotBlank(variable.toString())) {
            logger.trace("Adding variable [{}] into the variables set.", variable);
            removeCallFunctionsFromVariable();
            variables.add(variable.toString());
        }

        logger.trace("Found the following variables from the given script: [{}]", variables);
        return variables;
    }

    private String getScriptAsJsonTree(String script) {
        logger.trace("Creating JSON Tree for script [{}].", script);
        Options options = new Options("nashorn");
        options.set("anon.functions", true);
        options.set("parse.only", true);
        options.set("scripting", true);

        ErrorManager errors = new ErrorManager();
        Context context = new Context(options, errors, Thread.currentThread().getContextClassLoader());
        Context.setGlobal(context.createGlobal());

        return ScriptUtils.parse(script, "nashorn", false);
    }

    protected void iterateOverJsonTree(Iterator<Map.Entry<String, JsonNode>> iterator) {
        while (iterator.hasNext()) {
            iterateOverJsonTree(iterator.next());
        }
    }

    protected void iterateOverJsonTree(Map.Entry<String, JsonNode> fields) {
        JsonNode node = null;

        if (fields.getValue().isArray()) {
            iterateOverArrayNodes(fields);
        } else {
            node = fields.getValue();
        }

        String fieldName = searchIntoObjectNodes(node);

        if (fieldName == null) {
            String key = fields.getKey();
            if (TYPE.equals(key) && CALL_EXPRESSION.equals(node.textValue())) {
                callExpressions++;
            }

            if (NAME.equals(key) || PROPERTY.equals(key)) {
                appendFieldValueToVariable(key, node);
            }
        }
    }

    protected void iterateOverArrayNodes(Map.Entry<String, JsonNode> fields) {
        for (int count = 0; fields.getValue().get(count) != null; count++) {
            iterateOverJsonTree(fields.getValue().get(count).fields());
        }
    }

    protected String searchIntoObjectNodes(JsonNode node) {
        if (node == null) {
            return null;
        }

        String fieldName = null;
        Iterator<String> iterator = node.fieldNames();
        while (iterator.hasNext()) {
            fieldName = iterator.next();
            if (TYPE.equals(fieldName) && CALL_EXPRESSION.equals(node.get(fieldName).textValue())) {
                callExpressions++;
            }

            if (NAME.equals(fieldName) || PROPERTY.equals(fieldName)) {
                appendFieldValueToVariable(fieldName, node.get(fieldName));
            }

            if (node.get(fieldName).isArray()) {
                JsonNode blockStatementContent = node.get(fieldName).get(0);
                if (blockStatementContent != null) {
                    iterateOverJsonTree(blockStatementContent.fields());
                }
            } else {
                iterateOverJsonTree(node.get(fieldName).fields());
            }
        }

        return fieldName;
    }

    protected void appendFieldValueToVariable(String key, JsonNode node) {
        String nodeTextValue = node.textValue();
        if (nodeTextValue == null) {
            return;
        }

        if (PROPERTY.equals(key)) {
            logger.trace("Appending field value [{}] to variable [{}] as the field name is \"property\".", nodeTextValue, variable);
            variable.append(".").append(nodeTextValue);
            return;
        }

        logger.trace("Building new variable [{}] as the field name is \"name\"", nodeTextValue);
        if (StringUtils.isNotBlank(variable.toString())) {
            logger.trace("Adding variable [{}] into the variables set.", variable);
            removeCallFunctionsFromVariable();
            variables.add(variable.toString());
            variable.setLength(0);
        }
        variable.append(nodeTextValue);
    }

    protected void removeCallFunctionsFromVariable() {
        String[] disassembledVariable = variable.toString().split("\\.");
        variable.setLength(0);

        int newVariableSize = disassembledVariable.length - callExpressions;
        String[] newVariable = Arrays.copyOfRange(disassembledVariable, 0, newVariableSize);

        variable.append(String.join(".", newVariable));
        callExpressions = 0;
    }

    /**
     * Replaces all variables in script that matches the key in {@link Map} for their respective values.
     *
     * @param script the script which the variables will be replaced.
     * @param variablesToReplace a {@link Map} which has the key as the variable to be replaced and the value as the variable to replace.
     * @return A new script with the variables replaced.
     */
    public String replaceScriptVariables(String script, Map<String, String> variablesToReplace) {
        String regex = String.format("\\b(%s)\\b", String.join("|", variablesToReplace.keySet()));
        Matcher matcher = Pattern.compile(regex).matcher(script);

        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, variablesToReplace.get(matcher.group()));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    public int getCallExpressions() {
        return callExpressions;
    }

    public void setCallExpressions(int callExpressions) {
        this.callExpressions = callExpressions;
    }

    public StringBuilder getVariable() {
        return variable;
    }

    public void setVariable(StringBuilder variable) {
        this.variable = variable;
    }

    public Set<String> getVariables() {
        return variables;
    }

    public void setVariables(Set<String> variables) {
        this.variables = variables;
    }
}
