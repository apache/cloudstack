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
package com.cloud.hypervisor.vmware.mo;

import com.cloud.hypervisor.vmware.util.VmwareContext;

import com.cloud.utils.exception.CloudRuntimeException;
import com.vmware.pbm.PbmCapabilityProfile;
import com.vmware.pbm.PbmProfile;
import com.vmware.pbm.PbmProfileCategoryEnum;
import com.vmware.pbm.PbmProfileId;
import com.vmware.pbm.PbmProfileResourceType;
import com.vmware.pbm.PbmProfileResourceTypeEnum;
import com.vmware.vim25.ManagedObjectReference;

import com.vmware.vim25.VirtualMachineDefinedProfileSpec;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PbmProfileManagerMO extends BaseMO {

    private static final Logger LOGGER = Logger.getLogger(PbmProfileManagerMO.class);

    public PbmProfileManagerMO (VmwareContext context) {
        super(context, context.getPbmServiceContent().getProfileManager());
    }

    public PbmProfileManagerMO (VmwareContext context, ManagedObjectReference morProfileMgr) {
        super(context, morProfileMgr);
    }

    public PbmProfileManagerMO (VmwareContext context, String morType, String morValue) {
        super(context, morType, morValue);
    }

    public List<PbmProfileId> getStorageProfileIds() throws Exception {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Querying vCenter " + _context.getServerAddress() + " for profiles");
        }
        List<PbmProfileId> profileIds = _context.getPbmService().pbmQueryProfile(_mor, getStorageResourceType(), null);
        return profileIds;
    }

    public List<PbmProfile> getStorageProfiles() throws Exception {
        List<PbmProfileId> profileIds = getStorageProfileIds();
        List<PbmProfile> profiles = _context.getPbmService().pbmRetrieveContent(_mor, profileIds);

        List<PbmProfile> requirementCategoryProfiles = profiles.stream()
                .filter(x -> ((PbmCapabilityProfile)x).getProfileCategory().equals(PbmProfileCategoryEnum.REQUIREMENT.toString()))
                .collect(Collectors.toList());
        return requirementCategoryProfiles;
    }

    public PbmProfile getStorageProfile(String storageProfileId) throws Exception {
        List<PbmProfileId> profileIds = getStorageProfileIds();

        PbmProfileId profileId = profileIds.stream()
                .filter(x -> x.getUniqueId().equals(storageProfileId))
                .findFirst().orElse(null);

        if (profileId == null) {
            String errMsg = String.format("Storage profile with id %s not found", storageProfileId);
            LOGGER.debug(errMsg);
            throw new CloudRuntimeException(errMsg);
        }

        List<PbmProfile> profile = _context.getPbmService().pbmRetrieveContent(_mor, Collections.singletonList(profileId));
        return profile.get(0);
    }

    private PbmProfileResourceType getStorageResourceType() {
        PbmProfileResourceType resourceType = new PbmProfileResourceType();
        resourceType.setResourceType(PbmProfileResourceTypeEnum.STORAGE.value());
        return resourceType;
    }


    public VirtualMachineDefinedProfileSpec getProfileSpec(String profileId) throws Exception {
        VirtualMachineDefinedProfileSpec profileSpec = new VirtualMachineDefinedProfileSpec();
        PbmProfile profile = getStorageProfile(profileId);
        profileSpec.setProfileId(profile.getProfileId().getUniqueId());
        return profileSpec;
    }

}
