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

public class ExtensionServerActionBaseCommand  extends Command {
    long msId;
    long extensionId;
    String extensionName;
    boolean extensionUserDefined;
    String extensionRelativeEntryPointPath;

    protected ExtensionServerActionBaseCommand(long msId, Extension extension) {
        this.msId = msId;
        this.extensionId = extension.getId();
        this.extensionName = extension.getName();
        this.extensionUserDefined = extension.isUserDefined();
        this.extensionRelativeEntryPointPath = extension.getRelativeEntryPoint();
    }

    public long getMsId() {
        return msId;
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

    public String getExtensionRelativeEntryPointPath() {
        return extensionRelativeEntryPointPath;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
