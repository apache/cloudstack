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
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.apache.commons.collections.MapUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openjdk.nashorn.api.scripting.ClassFilter;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

import com.cloud.utils.exception.CloudRuntimeException;

/**
 * Executes JavaScript with strong restrictions to mitigate RCE risks.
 * - Disables Java interop via --no-java AND a deny-all ClassFilter
 * - Disables Nashorn syntax extensions
 * - Uses Bindings instead of string-splicing variables
 * - Fresh ScriptContext per execution, with timeout on a daemon worker
 */
public class JsInterpreter implements Closeable {
    protected Logger logger = LogManager.getLogger(JsInterpreter.class);

    protected static final List<String> RESTRICTED_TOKENS = Arrays.asList( "engine", "context", "factory",
            "Java", "java", "Packages"," javax", "load", "loadWithNewGlobal", "print", "factory", "getClass",
            "runCommand", "Runtime", "exec", "ProcessBuilder", "Thread", "thread", "Threads", "Class", "class");

    protected ScriptEngine interpreter;
    protected String interpreterName;
    private final String injectingLogMessage = "Injecting variable [%s] with value [%s] into the JS interpreter.";
    protected ExecutorService executor;
    private TimeUnit defaultTimeUnit = TimeUnit.MILLISECONDS;
    private long timeout;
    private String timeoutDefaultMessage;

    // Store variables as Objects; they go into Bindings (no code splicing)
    protected Map<String, Object> variables = new LinkedHashMap<>();

    /** Deny-all filter: no Java class is visible from scripts. */
    static final class DenyAllClassFilter implements ClassFilter {
        @Override public boolean exposeToScripts(String className) { return false; }
    }

    /**
     * Constructor created exclusively for unit testing.
     */
    protected JsInterpreter() { }

    public JsInterpreter(long timeout) {
        this.timeout = timeout;
        this.timeoutDefaultMessage = String.format(
                "Timeout (in milliseconds) defined in the global setting [quota.activationrule.timeout]: [%s].", this.timeout);

        if (System.getProperty("nashorn.args") == null) {
            System.setProperty("nashorn.args", "--no-java --no-syntax-extensions");
        }

        this.executor = new ThreadPoolExecutor(
                1, 1, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                r -> {
                    Thread t = new Thread(r, "JsInterpreter-worker");
                    t.setDaemon(true);
                    return t;
                }
        );

        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        this.interpreterName = factory.getEngineName();
        logger.trace(String.format("Initiating JS interpreter: %s.", interpreterName));

        setScriptEngineDisablingJavaLanguage(factory);
    }

    protected void setScriptEngineDisablingJavaLanguage(NashornScriptEngineFactory factory) {
        String[] opts = new String[] {
                "--no-java",
                "--no-syntax-extensions",
        };
        interpreter = factory.getScriptEngine(
                opts,
                JsInterpreter.class.getClassLoader(),
                new DenyAllClassFilter()
        );
    }

    /** Discards the current variables map and create a new one. */
    public void discardCurrentVariables() {
        logger.trace("Discarding current variables map and creating a new one.");
        variables = new LinkedHashMap<>();
    }

    /**
     * Adds a variable that will be exposed via ENGINE_SCOPE bindings.
     * Safe against code injection (no string concatenation).
     */
    public void injectVariable(String key, Object value) {
        if (key == null) return;
        logger.trace(String.format(injectingLogMessage, key, String.valueOf(value)));
        variables.put(key, value);
    }

    /**
     * @deprecated Not needed when using Bindings; kept for source compatibility.
     *             Prefer {@link #injectVariable(String, Object)}.
     */
    @Deprecated
    public void injectStringVariable(String key, String value) {
        if (value == null) {
            logger.trace(String.format("Not injecting [%s] because its value is null.", key));
            return;
        }
        injectVariable(key, value);
    }

    /**
     * Injects the variables via Bindings and executes the script with a fresh context.
     * @param script Code to be executed.
     * @return The result of the executed script.
     */
    public Object executeScript(String script) {
        Objects.requireNonNull(script, "script");

        logger.debug(String.format("Executing script [%s].", script));

        Object result = executeScriptInThread(script);

        logger.debug(String.format("The script [%s] had the following result: [%s].", script, result));
        return result;
    }

    protected Object executeScriptInThread(String script) {
        final Callable<Object> task = () -> {
            final SimpleScriptContext ctx = new SimpleScriptContext();

            final Bindings engineBindings = new SimpleBindings();
            if (MapUtils.isNotEmpty(variables)) {
                engineBindings.putAll(variables);
            }
            for (String token : RESTRICTED_TOKENS) {
                engineBindings.put(token, null);
            }
            ctx.setBindings(engineBindings, ScriptContext.ENGINE_SCOPE);

            final StringWriter out = new StringWriter();
            ctx.setWriter(out);

            try {
                final CompiledScript compiled = ((Compilable) interpreter).compile(script);
                Object result = compiled.eval(ctx);
                if (out.getBuffer().length() > 0) {
                    logger.info("Script produced output on stdout: [{}]", out);
                }
                return result;
            } catch (ScriptException se) {
                String msg = se.getMessage() == null ? "Script error" : se.getMessage();
                throw new ScriptException("Script error: " + msg, se.getFileName(), se.getLineNumber(), se.getColumnNumber());
            }
        };

        final Future<Object> future = executor.submit(task);

        try {
            return future.get(this.timeout, defaultTimeUnit);
        } catch (TimeoutException e) {
            String message = String.format(
                    "Execution of script [%s] took too long and timed out. %s", script, timeoutDefaultMessage);
            logger.error(message, e);
            throw new CloudRuntimeException(message, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String message = String.format("Execution of script [%s] was interrupted.", script);
            logger.error(message, e);
            throw new CloudRuntimeException(message, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            String message = String.format("Unable to execute script [%s] due to [%s]", script, cause.getMessage());
            logger.error(message, cause);
            throw new CloudRuntimeException(message, cause);
        } finally {
            future.cancel(true);
        }
    }

    @Override
    public void close() throws IOException {
        executor.shutdownNow();
    }
}
