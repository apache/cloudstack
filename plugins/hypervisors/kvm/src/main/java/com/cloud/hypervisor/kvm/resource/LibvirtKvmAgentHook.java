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

package com.cloud.hypervisor.kvm.resource;

import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import groovy.lang.Binding;
import groovy.lang.GroovyObject;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.codehaus.groovy.runtime.metaclass.MissingMethodExceptionNoStack;
import org.joda.time.Duration;

import java.io.File;
import java.io.IOException;

public class LibvirtKvmAgentHook {
    private final String script;
    private final String shellScript;
    private final String method;
    private final GroovyScriptEngine gse;
    private final Binding binding = new Binding();

    protected Logger logger = LogManager.getLogger(getClass());

    public LibvirtKvmAgentHook(String path, String script, String shellScript, String method) throws IOException {
        this.script = script;
        this.method = method;
        File full_path = new File(path, script);
        if (!full_path.canRead()) {
            logger.warn("Groovy script '" + full_path.toString() + "' is not available. Transformations will not be applied.");
            this.gse = null;
        } else {
            this.gse = new GroovyScriptEngine(path);
        }
        full_path = new File(path, shellScript);
        if (!full_path.canRead()) {
            logger.warn("Shell script '" + full_path.toString() + "' is not available. Transformations will not be applied.");
            this.shellScript = null;
        } else {
            this.shellScript = full_path.getAbsolutePath();
        }
    }

    public LibvirtKvmAgentHook(String path, String script, String method) throws IOException {
        this.script = script;
        this.method = method;
        File full_path = new File(path, script);
        if (!full_path.canRead()) {
            logger.warn("Groovy script '" + full_path.toString() + "' is not available. Transformations will not be applied.");
            this.gse = null;
        } else {
            this.gse = new GroovyScriptEngine(path);
        }
        this.shellScript = null;
    }

    public boolean isInitialized() {
        return this.gse != null;
    }

    /**
     * Sanitizes a string for safe use as a bash command argument by escaping special characters.
     * This prevents shell injection and parsing issues when passing multiline content like XML.
     */
    String sanitizeBashCommandArgument(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sanitized = new StringBuilder();
        for (char c : input.toCharArray()) {
            if ("\\\"'`$|&;()<>*?![]{}~".indexOf(c) != -1) {
                sanitized.append('\\');
            }
            sanitized.append(c);
        }
        return sanitized.toString();
    }

    public Object handle(Object arg) throws ResourceException, ScriptException {
        Object res = arg;
        if (isInitialized()) {
            GroovyObject cls = (GroovyObject) this.gse.run(this.script, binding);
            if (null == cls) {
                logger.warn("Groovy object is not received from script '" + this.script + "'.");
                return arg;
            } else {
                Object[] params = {logger, arg};
                try {
                    res = cls.invokeMethod(this.method, params);
                } catch (MissingMethodExceptionNoStack e) {
                    logger.error("Error occurred when calling method from groovy script, {}", e);
                    res = arg;
                }
            }
        } else {
            logger.warn("Groovy scripting engine is not initialized. Data transformation skipped.");
        }

        // Shell script
        if (this.shellScript != null) {
            logger.debug("Executing Shell script for transformation at: {}", this.shellScript);
            final Script command = new Script(this.shellScript, Duration.standardSeconds(30), logger);
            command.add(String.valueOf(this.method));
            command.add(String.valueOf(res));

            OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
            String result = command.execute(parser);
            if (result == null) {
                logger.debug("GPU discovery command executed successfully");
                res = parser.getLines();
            } else {
                logger.warn("Error occurred when calling script for transformation: {}", result);
            }
        } else {
            logger.debug("No shell script provided for transformation. Data transformation skipped.");
        }
        return res;
    }
}
