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
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.to.VolumeObjectTO;

import java.util.List;
import java.util.stream.Collectors;

public class ConsolidateVolumesCommand extends Command {

    private List<VolumeObjectTO> volumesToConsolidate;

    private List<String> secondaryStorageUuids;

    private String vmName;

    public ConsolidateVolumesCommand(List<VolumeInfo> volumesToConsolidate, List<String> secondaryStorageUuids, String vmName) {
        this.volumesToConsolidate = volumesToConsolidate.stream().map(vol -> (VolumeObjectTO)vol.getTO()).collect(Collectors.toList());
        this.secondaryStorageUuids = secondaryStorageUuids;
        this.vmName = vmName;
    }

    public List<VolumeObjectTO> getVolumesToConsolidate() {
        return volumesToConsolidate;
    }

    public List<String> getSecondaryStorageUuids() {
        return secondaryStorageUuids;
    }

    public String getVmName() {
        return vmName;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
