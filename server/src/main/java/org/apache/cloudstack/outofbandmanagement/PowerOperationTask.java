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

package org.apache.cloudstack.outofbandmanagement;

import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.host.Host;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

public class PowerOperationTask implements Runnable {
    public static final Logger LOG = Logger.getLogger(PowerOperationTask.class);

    final private OutOfBandManagementService service;
    final private Host host;
    final private OutOfBandManagement.PowerOperation powerOperation;

    public PowerOperationTask(OutOfBandManagementService service, Host host, OutOfBandManagement.PowerOperation powerOperation) {
        this.service = service;
        this.host = host;
        this.powerOperation = powerOperation;
    }

    @Override
    public String toString() {
        return String.format("[OOBM Task] Power operation:%s on Host:%d(%s)", powerOperation, host.getId(), host.getName());
    }

    @Override
    public void run() {
        try {
            service.executePowerOperation(host, powerOperation, null);
        } catch (Exception e) {
            LOG.warn(String.format("Out-of-band management background task operation=%s for host %s failed with: %s",
                    powerOperation.name(), host.getName(), e.getMessage()));

            String eventMessage = String
                    .format("Error while issuing out-of-band management action %s for host: %s", powerOperation.name(), host.getName());

            ActionEventUtils.onCreatedActionEvent(CallContext.current().getCallingUserId(), CallContext.current().getCallingAccountId(), EventVO.LEVEL_WARN,
                    EventTypes.EVENT_HOST_OUTOFBAND_MANAGEMENT_ACTION, true, eventMessage);
        }
    }
}
