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
package org.apache.cloudstack.backup;

import com.cloud.agent.api.Command;
import org.apache.cloudstack.storage.to.KbossTO;

import java.util.List;

public class CleanupKbossBackupErrorCommand extends Command {

    private boolean runningVM;

    private boolean errorOnCreate;

    private boolean endOfChain;

    private boolean isTopDelta;

    private String vmName;

    private String imageStoreUrl;

    private List<KbossTO> kbossTOS;

    public CleanupKbossBackupErrorCommand(boolean runningVM, boolean errorOnCreate, boolean endOfChain, boolean isTopDelta, String vmName, String imageStoreUrl, List<KbossTO> kbossTOS) {
        this.errorOnCreate = errorOnCreate;
        this.runningVM = runningVM;
        this.endOfChain = endOfChain;
        this.isTopDelta = isTopDelta;
        this.vmName = vmName;
        this.imageStoreUrl = imageStoreUrl;
        this.kbossTOS = kbossTOS;
    }

    public boolean isErrorOnCreate() {
        return errorOnCreate;
    }

    public boolean isEndOfChain() {
        return endOfChain;
    }

    public boolean isTopDelta() {
        return isTopDelta;
    }

    public boolean isRunningVM() {
        return runningVM;
    }

    public String getVmName() {
        return vmName;
    }

    public String getImageStoreUrl() {
        return imageStoreUrl;
    }

    public List<KbossTO> getKbossTOs() {
        return kbossTOS;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
