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
package com.cloud.agent.api;

import com.cloud.utils.exception.ExceptionUtil;

public class Answer extends Command {
    protected boolean result;
    protected String details;

    protected Answer() {
        this(null);
    }

    public Answer(final Command command) {
        this(command, true, null);
    }

    public Answer(final Command command, final boolean success, final String details) {
        result = success;
        this.details = details;
    }

    public Answer(final Command command, final Exception e) {
        this(command, false, ExceptionUtil.toString(e));
    }

    public boolean getResult() {
        return result;
    }

    public String getDetails() {
        return details;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public static UnsupportedAnswer createUnsupportedCommandAnswer(final Command cmd) {
        return new UnsupportedAnswer(cmd, "Unsupported command issued: " + cmd.toString() + ".  Are you sure you got the right type of server?");
    }

    public static UnsupportedAnswer createUnsupportedVersionAnswer(final Command cmd) {
        return new UnsupportedAnswer(cmd, "Unsupported Version.");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Answer)) return false;
        if (!super.equals(o)) return false;

        Answer answer = (Answer) o;

        if (result != answer.result) return false;
        if (details != null ? !details.equals(answer.details) : answer.details != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result1 = super.hashCode();
        result1 = 31 * result1 + (result ? 1 : 0);
        result1 = 31 * result1 + (details != null ? details.hashCode() : 0);
        return result1;
    }
}
