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

package org.apache.cloudstack.framework.extensions.command;

import org.apache.cloudstack.extension.Extension;

import com.cloud.agent.api.Command;

public class ExtensionBaseCommand extends Command {
    private final long extensionId;
    private final String extensionName;
    private final boolean extensionUserDefined;
    private final String extensionRelativePath;
    private final Extension.State extensionState;

    protected ExtensionBaseCommand(Extension extension) {
        this.extensionId = extension.getId();
        this.extensionName = extension.getName();
        this.extensionUserDefined = extension.isUserDefined();
        this.extensionRelativePath = extension.getRelativePath();
        this.extensionState = extension.getState();
    }

    public long getExtensionId() {
        return extensionId;
    }

    public String getExtensionName() {
        return extensionName;
    }

    public boolean isExtensionUserDefined() {
        return extensionUserDefined;
    }

    public String getExtensionRelativePath() {
        return extensionRelativePath;
    }

    public Extension.State getExtensionState() {
        return extensionState;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
