// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with this
// work for additional information regarding copyright ownership. The ASF
// licenses this file to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance with the
// License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.ai.engine;

/**
 * The engine refused a call, or could not be reached.
 *
 * Carries the HTTP status and the RFC 9457 problem type where the engine
 * answered, so a command can map a 403 from the engine onto a CloudStack
 * permission error rather than a generic failure.
 */
public class EngineException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;
    private final String problemType;

    public EngineException(String message) {
        this(message, 0, null);
    }

    public EngineException(String message, int statusCode, String problemType) {
        super(message);
        this.statusCode = statusCode;
        this.problemType = problemType;
    }

    public EngineException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.problemType = null;
    }

    /** HTTP status, or 0 when the engine could not be reached at all. */
    public int getStatusCode() {
        return statusCode;
    }

    /** The RFC 9457 problem type, or null. */
    public String getProblemType() {
        return problemType;
    }

    /** Whether no engine replica answered, as opposed to answering badly. */
    public boolean isUnreachable() {
        return statusCode == 0;
    }
}
