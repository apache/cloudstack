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

import groovy.lang.Binding;
import groovy.lang.GroovyObject;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.codehaus.groovy.runtime.metaclass.MissingMethodExceptionNoStack;

import java.io.File;
import java.io.IOException;

public class LibvirtKvmAgentHook {
    private final String script;
    private final String method;
    private final GroovyScriptEngine gse;
    private final Binding binding = new Binding();

    protected Logger logger = LogManager.getLogger(getClass());

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
    }

    public boolean isInitialized() {
        return this.gse != null;
    }

    public Object handle(Object arg) throws ResourceException, ScriptException {
        if (!isInitialized()) {
            logger.warn("Groovy scripting engine is not initialized. Data transformation skipped.");
            return arg;
        }

        GroovyObject cls = (GroovyObject) this.gse.run(this.script, binding);
        if (null == cls) {
            logger.warn("Groovy object is not received from script '" + this.script + "'.");
            return arg;
        } else {
            Object[] params = {logger, arg};
            try {
                Object res = cls.invokeMethod(this.method, params);
                return res;
            } catch (MissingMethodExceptionNoStack e) {
                logger.error("Error occurred when calling method from groovy script, {}", e);
                return arg;
            }
        }
    }
}
