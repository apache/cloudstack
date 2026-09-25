//
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
//

package org.apache.cloudstack.backup;

import com.cloud.agent.api.Command;
import org.apache.cloudstack.storage.to.KbossTO;

import java.util.List;

public class TakeKbossBackupCommand extends Command {

    private boolean quiesceVm;

    private boolean runningVM;

    private boolean endChain;

    private String vmName;

    private String imageStoreUrl;

    private List<String> backupChainImageStoreUrls;

    private List<KbossTO> kbossTOS;

    private boolean isolated;

    public TakeKbossBackupCommand(boolean quiesceVm, boolean runningVM, boolean endChain, String vmName, String imageStoreUrl, List<String> backupChainImageStoreUrls, List<KbossTO> kbossTOS, boolean isolated) {
        this.quiesceVm = quiesceVm;
        this.runningVM = runningVM;
        this.endChain = endChain;
        this.vmName = vmName;
        this.imageStoreUrl = imageStoreUrl;
        this.backupChainImageStoreUrls = backupChainImageStoreUrls;
        this.kbossTOS = kbossTOS;
        this.isolated = isolated;
    }

    public boolean isQuiesceVm() {
        return quiesceVm;
    }

    public boolean isRunningVM() {
        return runningVM;
    }

    public boolean isEndChain() {
        return endChain;
    }

    public String getVmName() {
        return vmName;
    }

    public String getImageStoreUrl() {
        return imageStoreUrl;
    }

    public List<String> getBackupChainImageStoreUrls() {
        return backupChainImageStoreUrls;
    }

    public List<KbossTO> getKbossTOs() {
        return kbossTOS;
    }

    public boolean isIsolated() {
        return isolated;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
