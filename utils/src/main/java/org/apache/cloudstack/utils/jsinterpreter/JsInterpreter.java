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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;
import com.eclipsesource.v8.V8;

/**
 * A facade to some methods of J2V8 with addition of automatic JSON conversion inside the JS, if needed.
 */
public class JsInterpreter implements Closeable {
    protected Logger logger = Logger.getLogger(JsInterpreter.class);

    protected V8 interpreter;
    protected String valuesToConvertToJsonBeforeExecutingTheCode = "";
    protected final String interpreterName = "V8";
    private final String injectingLogMessage = "Injecting variable [%s] of type [%s] and with value [%s] into the JS interpreter.";
    private ExecutorService executor;
    private TimeUnit defaultTimeUnit = TimeUnit.MILLISECONDS;
    private long timeout;
    private String timeoutDefaultMessage;

    /**
     * J2V8 was designed to mobile application and has validations to keep execution in single threads. Therefore, we cannot instantiate the interpreter in the main thread and
     * execute a script in another thread, which will be necessary to execute the script with a timeout. As a workaround, we will instantiate the interpreter in a thread and will
     * use this thread to execute any other interpreter's method.
     */
    public JsInterpreter(long timeout) {
        this.timeout = timeout;
        this.timeoutDefaultMessage = String.format("Timeout (in milliseconds) defined in the global setting [quota.activationrule.timeout]: [%s].", this.timeout);

        logger.trace(String.format("Initiating JS interpreter: %s.", interpreterName));

        executor = Executors.newSingleThreadExecutor();
        Callable<V8> task = new Callable<V8>() {
            public V8 call() {
                return V8.createV8Runtime();
            }
        };

        Future<V8> future = executor.submit(task);

        try {
            interpreter = future.get(this.timeout, defaultTimeUnit);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            String message = String.format("Unable to instantiate JS interpreter [%s] due to [%s]", interpreterName, e.getMessage());

            if (e instanceof TimeoutException) {
                message = String.format("Instantiation of JS interpreter [%s] took too long and timed out. %s", interpreterName, timeoutDefaultMessage);
            }

            logger.error(message, e);
            throw new CloudRuntimeException(message, e);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Injects a null variable with the given name in case the value is null.<br/>
     * Calls {@link V8#addNull(String)}.
     * @param name The name of the variable.
     * @param value The value of the variable.
     * @return True if the value is null and the variable was injected as null, otherwise false.
     */
    protected boolean injectNullVariable (String name, Object value) {
        if (value != null) {
            return false;
        }

        logger.trace(String.format("The variable [%s] has a null value. Therefore, we will inject it as null into the JS interpreter.", name));
        injectVariableInThread(name, null);
        return true;
    }

    /**
     * Injects a String variable with the given name.<br/>
     * In case the value is null, the method {@link JsInterpreter#injectNullVariable(String, Object)} will be called.<br/>
     * Calls {@link V8#add(String, String)}.
     * @param name The name of the variable.
     * @param value The value of the variable.
     */
    public void injectVariable(String name, String value) {
        injectVariable(name, value, false);
    }

    /**
     * Injects a String variable with the given name.<br/>
     * In case the value is null, the method {@link JsInterpreter#injectNullVariable(String, Object)} will be called.<br/>
     * In case the value is in JSON format, it will create a parse command to be executed before user's script.<br/>
     * Calls {@link V8#add(String, String)}.
     * @param name The name of the variable.
     * @param value The value of the variable.
     * @param isJsonValue Whether the value is in JSON format or not.
     */
    public void injectVariable(String name, String value, boolean isJsonValue) {
        if (injectNullVariable(name, value)) {
            return;
        }

        logger.trace(String.format(injectingLogMessage, name, value.getClass().getSimpleName(), value));
        injectVariableInThread(name, value);

        if (isJsonValue) {
            String jsonParse = String.format("%s = JSON.parse(%s);", name, name);

            logger.trace(String.format("The variable [%s], of type [%s], has a JSON value [%s]. Therefore, we will add a command to convert it to JSON in the interpreter: [%s].",
                    name, value.getClass(), value, jsonParse));

            valuesToConvertToJsonBeforeExecutingTheCode = String.format("%s %s", valuesToConvertToJsonBeforeExecutingTheCode, jsonParse);
        }
    }

    protected void injectVariableInThread(final String name, final String value) {
        Callable<V8> task = new Callable<V8>() {
            public V8 call() {
                if (value == null) {
                    interpreter.addNull(name);
                } else {
                    interpreter.add(name, value);
                }

                return interpreter;
            }
        };

        Future<V8> future = executor.submit(task);

        try {
            future.get(this.timeout, defaultTimeUnit);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            String message = String.format("Unable to inject variable [%s] with value [%s] into the JS interpreter due to [%s]", name, value, e.getMessage());

            if (e instanceof TimeoutException) {
                message = String.format("Injection of variable [%s] with value [%s] into the JS interpreter took too long and timed out. %s", name, value, timeoutDefaultMessage);
            }

            logger.error(message, e);
            throw new CloudRuntimeException(message, e);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Converts variables to JSON, in case they exist, and executes the user's script.
     * @param script Code to be executed.
     * @return The result of the executed code.
     */
    public Object executeScript(String script) {
        logger.trace(String.format("Adding JSON convertions [%s] before executing user's script [%s].", valuesToConvertToJsonBeforeExecutingTheCode, script));

        script = String.format("%s %s", valuesToConvertToJsonBeforeExecutingTheCode, script);
        valuesToConvertToJsonBeforeExecutingTheCode = "";

        logger.debug(String.format("Executing script [%s].", script));

        Object result = executeScriptInThread(script);

        logger.debug(String.format("The script [%s] had the following result: [%s].", script, result));
        return result;
    }

    protected Object executeScriptInThread(final String script) {
        Callable<Object> task = new Callable<Object>() {
            public Object call() {
                return interpreter.executeScript(script);
            }
        };

        Future<Object> future = executor.submit(task);

        try {
            return future.get(this.timeout, defaultTimeUnit);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            interpreter.terminateExecution();

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

    /**
     * Releases the JS interpreter.
     */
    @Override
    public void close() throws IOException {
        logger.trace(String.format("Releasing JS interpreter: %s.", interpreterName));

        Callable<V8> task = new Callable<V8>() {
            public V8 call() {
                interpreter.release();
                return interpreter;
            }
        };

        Future<V8> future = executor.submit(task);

        try {
            future.get(this.timeout, defaultTimeUnit);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            String message = String.format("Unable to release JS interpreter [%s] due to [%s]", interpreterName, e.getMessage());

            if (e instanceof TimeoutException) {
                message = String.format("Release of JS interpreter [%s] took too long and timed out. %s", interpreterName, timeoutDefaultMessage);
            }

            logger.error(message, e);
            throw new CloudRuntimeException(message, e);
        } finally {
            future.cancel(true);
        }
    }

}
