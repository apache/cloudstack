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
import com.vmware.pbm.PbmPlacementCompatibilityResult;
import com.vmware.pbm.PbmPlacementHub;
import com.vmware.pbm.PbmProfile;
import com.vmware.pbm.PbmProfileId;
import com.vmware.vim25.ManagedObjectReference;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class PbmPlacementSolverMO extends BaseMO {

    private static final Logger LOGGER = Logger.getLogger(PbmPlacementSolverMO.class);

    public PbmPlacementSolverMO (VmwareContext context) {
        super(context, context.getPbmServiceContent().getPlacementSolver());
    }

    public PbmPlacementSolverMO(VmwareContext context, ManagedObjectReference morPlacementSolver) {
        super(context, morPlacementSolver);
    }

    public PbmPlacementSolverMO(VmwareContext context, String morType, String morValue) {
        super(context, morType, morValue);
    }

    public boolean isDatastoreCompatibleWithStorageProfile(ManagedObjectReference dsMor, PbmProfile profile) throws Exception {
        boolean isDatastoreCompatibleWithStorageProfile = false;

        PbmPlacementHub placementHub = new PbmPlacementHub();
        placementHub.setHubId(dsMor.getValue());
        placementHub.setHubType(dsMor.getType());

        List<PbmPlacementHub> placementHubList = new ArrayList<PbmPlacementHub>();
        placementHubList.add(placementHub);
        PbmProfileId profileId = profile.getProfileId();
        List<PbmPlacementCompatibilityResult> placementCompatibilityResultList = _context.getPbmService().pbmCheckCompatibility(_mor, placementHubList, profileId);
        if (CollectionUtils.isNotEmpty(placementCompatibilityResultList)) {
            for (PbmPlacementCompatibilityResult placementResult : placementCompatibilityResultList) {
                // Check for error and warning
                if (CollectionUtils.isEmpty(placementResult.getError()) && CollectionUtils.isEmpty(placementResult.getWarning())) {
                    isDatastoreCompatibleWithStorageProfile = true;
                }
            }
        }
        return isDatastoreCompatibleWithStorageProfile;
    }
}
