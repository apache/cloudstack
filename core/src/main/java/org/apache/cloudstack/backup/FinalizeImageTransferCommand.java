//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package org.apache.cloudstack.backup;

import com.cloud.agent.api.Command;
import com.cloud.storage.Storage;

public class FinalizeImageTransferCommand extends Command {
    private String transferId;
    private String convertSource;
    private Storage.StoragePoolType convertPoolType;
    private String convertPoolUuid;
    private String convertVolumePath;

    public FinalizeImageTransferCommand() {
    }

    public FinalizeImageTransferCommand(String transferId) {
        this.transferId = transferId;
    }

    public String getTransferId() {
        return transferId;
    }

    /**
     * Has the host convert the qcow2 image uploaded to a temporary device into the volume, see
     * {@code PrimaryDataStoreDriver.createUploadStaging}.
     * @param source the temporary device
     */
    public void setConvertInto(String source, Storage.StoragePoolType poolType, String poolUuid, String volumePath) {
        this.convertSource = source;
        this.convertPoolType = poolType;
        this.convertPoolUuid = poolUuid;
        this.convertVolumePath = volumePath;
    }

    public String getConvertSource() {
        return convertSource;
    }

    public Storage.StoragePoolType getConvertPoolType() {
        return convertPoolType;
    }

    public String getConvertPoolUuid() {
        return convertPoolUuid;
    }

    public String getConvertVolumePath() {
        return convertVolumePath;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }
}
