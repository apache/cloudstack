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

import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.DataTO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.DataStoreRole;

public final class CopyCommand extends Command implements StorageSubSystemCommand {
    private DataTO srcTO;
    private DataTO destTO;
    private DataTO cacheTO;
    boolean executeInSequence = false;


    public CopyCommand(DataTO srcData, DataTO destData, int timeout, boolean executeInSequence) {
        super();
        this.srcTO = srcData;
        this.destTO = destData;
        this.setWait(timeout);
        this.executeInSequence = executeInSequence; // default is to run in parallel, so false here
/*        
        // special handling for vmware parallel vm deployment bug https://issues.apache.org/jira/browse/CLOUDSTACK-3568
        if (srcTO instanceof TemplateObjectTO && destTO instanceof VolumeObjectTO) {
            // create a volume wrapper vm from a template on primary storage
            TemplateObjectTO srcTmplt = (TemplateObjectTO) srcTO;
            VolumeObjectTO destVol = (VolumeObjectTO) destTO;
            if (srcTmplt.getHypervisorType() == HypervisorType.VMware && srcTmplt.getDataStore().getRole() == DataStoreRole.Primary
                    && destVol.getDataStore().getRole() == DataStoreRole.Primary) {
                this.executeInSequence = true;
            }
        }
*/
    }

    public DataTO getDestTO() {
        return this.destTO;
    }

    public void setSrcTO(DataTO srcTO) {
        this.srcTO = srcTO;
    }

    public void setDestTO(DataTO destTO) {
        this.destTO = destTO;
    }

    public DataTO getSrcTO() {
        return this.srcTO;
    }

    @Override
    public boolean executeInSequence() {
        return executeInSequence;
    }

    public DataTO getCacheTO() {
        return cacheTO;
    }

    public void setCacheTO(DataTO cacheTO) {
        this.cacheTO = cacheTO;
    }

    public int getWaitInMillSeconds() {
        return this.getWait() * 1000;
    }

}
