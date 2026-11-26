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

package org.apache.cloudstack.storage.command;

import com.cloud.agent.api.Answer;

public class CheckFileInIsoAnswer extends Answer {

    private boolean filePresent;

    public CheckFileInIsoAnswer() {
        super();
    }

    /**
     * The ISO was read and the file was or was not found in it.
     */
    public CheckFileInIsoAnswer(final CheckFileInIsoCommand cmd, final boolean filePresent) {
        super(cmd, true, null);
        this.filePresent = filePresent;
    }

    /**
     * The ISO could not be read, so nothing is known about the file.
     */
    public CheckFileInIsoAnswer(final CheckFileInIsoCommand cmd, final String errorDetails) {
        super(cmd, false, errorDetails);
    }

    public boolean isFilePresent() {
        return filePresent;
    }
}
