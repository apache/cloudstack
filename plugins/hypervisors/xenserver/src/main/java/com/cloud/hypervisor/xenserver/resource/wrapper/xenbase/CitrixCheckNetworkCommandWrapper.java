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

package com.cloud.hypervisor.xenserver.resource.wrapper.xenbase;

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckNetworkAnswer;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.network.PhysicalNetworkSetupInfo;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Types.XenAPIException;

@ResourceWrapper(handles =  CheckNetworkCommand.class)
public final class CitrixCheckNetworkCommandWrapper extends CommandWrapper<CheckNetworkCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixCheckNetworkCommandWrapper.class);

    @Override
    public Answer execute(final CheckNetworkCommand command, final CitrixResourceBase citrixResourceBase) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Checking if network name setup is done on the resource");
        }

        final List<PhysicalNetworkSetupInfo> infoList = command.getPhysicalNetworkInfoList();

        try {
            boolean errorout = false;
            String msg = "";
            for (final PhysicalNetworkSetupInfo info : infoList) {
                if (!citrixResourceBase.isNetworkSetupByName(info.getGuestNetworkName())) {
                    msg =
                            "For Physical Network id:" + info.getPhysicalNetworkId() + ", Guest Network is not configured on the backend by name " +
                                    info.getGuestNetworkName();
                    errorout = true;
                    break;
                }
                if (!citrixResourceBase.isNetworkSetupByName(info.getPrivateNetworkName())) {
                    msg =
                            "For Physical Network id:" + info.getPhysicalNetworkId() + ", Private Network is not configured on the backend by name " +
                                    info.getPrivateNetworkName();
                    errorout = true;
                    break;
                }
                if (!citrixResourceBase.isNetworkSetupByName(info.getPublicNetworkName())) {
                    msg =
                            "For Physical Network id:" + info.getPhysicalNetworkId() + ", Public Network is not configured on the backend by name " +
                                    info.getPublicNetworkName();
                    errorout = true;
                    break;
                }
                /*if(!isNetworkSetupByName(info.getStorageNetworkName())){
                    msg = "For Physical Network id:"+ info.getPhysicalNetworkId() + ", Storage Network is not configured on the backend by name " + info.getStorageNetworkName();
                    errorout = true;
                    break;
                }*/
            }
            if (errorout) {
                s_logger.error(msg);
                return new CheckNetworkAnswer(command, false, msg);
            } else {
                return new CheckNetworkAnswer(command, true, "Network Setup check by names is done");
            }

        } catch (final XenAPIException e) {
            final String msg = "CheckNetworkCommand failed with XenAPIException:" + e.toString() + " host:" + citrixResourceBase.getHost().getUuid();
            s_logger.warn(msg, e);
            return new CheckNetworkAnswer(command, false, msg);
        } catch (final Exception e) {
            final String msg = "CheckNetworkCommand failed with Exception:" + e.getMessage() + " host:" + citrixResourceBase.getHost().getUuid();
            s_logger.warn(msg, e);
            return new CheckNetworkAnswer(command, false, msg);
        }
    }
}
