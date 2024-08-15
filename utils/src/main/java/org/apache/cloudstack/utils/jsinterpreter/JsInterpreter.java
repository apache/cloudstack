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

package org.apache.cloudstack.utils.jsinterpreter;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.collections.MapUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.utils.exception.CloudRuntimeException;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.ScriptEngine;

/**
 * A class to execute JavaScript scripts, with the possibility to inject context to the scripts.
 */
public class JsInterpreter implements Closeable {
    protected Logger logger = LogManager.getLogger(JsInterpreter.class);

    protected ScriptEngine interpreter;
    protected String interpreterName;
    private final String injectingLogMessage = "Injecting variable [%s] with value [%s] into the JS interpreter.";
    protected ExecutorService executor;
    private TimeUnit defaultTimeUnit = TimeUnit.MILLISECONDS;
    private long timeout;
    private String timeoutDefaultMessage;
    protected Map<String, String> variables = new LinkedHashMap<>();

    /**
     * Constructor created exclusively for unit testing.
     */
    protected JsInterpreter() {
    }

    public JsInterpreter(long timeout) {
        this.timeout = timeout;
        this.timeoutDefaultMessage = String.format("Timeout (in milliseconds) defined in the global setting [quota.activationrule.timeout]: [%s].", this.timeout);

        executor = Executors.newSingleThreadExecutor();
        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();

        this.interpreterName = factory.getEngineName();
        logger.trace(String.format("Initiating JS interpreter: %s.", interpreterName));

        setScriptEngineDisablingJavaLanguage(factory);
    }

    protected void setScriptEngineDisablingJavaLanguage(NashornScriptEngineFactory factory) {
        interpreter = factory.getScriptEngine("--no-java");
    }

    /**
     * Discards the current variables map and create a new one.
     */
    public void discardCurrentVariables() {
        logger.trace("Discarding current variables map and creating a new one.");
        variables = new LinkedHashMap<>();
    }

    /**
     * Adds the parameters to a Map that will be converted to JS variables right before executing the script.
     * @param key The name of the variable.
     * @param value The value of the variable.
     */
    public void injectVariable(String key, String value) {
        logger.trace(String.format(injectingLogMessage, key, value));
        variables.put(key, value);
    }

    /**
     * Adds the parameter, surrounded by double quotes, to a Map that will be converted to a JS variable right before executing the script.
     * @param key The name of the variable.
     * @param value The value of the variable.
     */
    public void injectStringVariable(String key, String value) {
        if (value == null) {
            logger.trace(String.format("Not injecting [%s] because its value is null.", key));
            return;
        }
        value = String.format("\"%s\"", value);
        logger.trace(String.format(injectingLogMessage, key, value));
        variables.put(key, value);
    }

    /**
     * Injects the variables to the script and execute it.
     * @param script Code to be executed.
     * @return The result of the executed script.
     */
    public Object executeScript(String script) {
        script = addVariablesToScript(script);

        logger.debug(String.format("Executing script [%s].", script));

        Object result = executeScriptInThread(script);

        logger.debug(String.format("The script [%s] had the following result: [%s].", script, result));
        return result;
    }

    protected Object executeScriptInThread(String script) {
        Callable<Object> task = () -> interpreter.eval(script);

        Future<Object> future = executor.submit(task);

        try {
            return future.get(this.timeout, defaultTimeUnit);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            String message = String.format("Unable to execute script [%s] due to [%s]", script, e.getMessage());

            if (e instanceof TimeoutException) {
                message = String.format("Execution of script [%s] took too long and timed out. %s", script, timeoutDefaultMessage);
            }

            logger.error(message, e);
            throw new CloudRuntimeException(message, e);
        } finally {
            future.cancel(true);
        }
    }

    protected String addVariablesToScript(String script) {
        if (MapUtils.isEmpty(variables)) {
            logger.trace(String.format("There is no variables to add to script [%s]. Returning.", script));
            return script;
        }

        String variablesToString = "";
        for (Map.Entry<String, String> variable : variables.entrySet()) {
            variablesToString = String.format("%s %s = %s;", variablesToString, variable.getKey(), variable.getValue());
        }

        return String.format("%s %s", variablesToString, script);
    }


    @Override
    public void close() throws IOException {
        executor.shutdown();
    }
}
