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

package org.apache.cloudstack.vm.storagefsvm;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.apache.cloudstack.storage.fileshare.StorageFsVm;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.vm.VMInstanceVO;

@Entity
@Table(name = "storagefsvm")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue(value = "StorageFsVm")
public class StorageFsVmVO extends VMInstanceVO implements StorageFsVm {
    @Column(name = "fileshare_id")
    private long fileShareId;

    public StorageFsVmVO() {
    }

    public StorageFsVmVO(final long id, final long serviceOfferingId, final String name, final long templateId,
            final Hypervisor.HypervisorType hypervisorType, final long guestOSId, final long domainId,
            final long accountId, final long userId, final long fileShareId) {
        super(id, serviceOfferingId, name, name, Type.StorageFsVm, templateId, hypervisorType, guestOSId, domainId,
                accountId, userId, true);
        this.fileShareId = fileShareId;
    }

    public long getFileShareId() {
        return fileShareId;
    }
}
