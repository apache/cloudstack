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
package com.cloud.ha;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;

import com.cloud.alert.AlertManager;
import com.cloud.usage.dao.UsageJobDao;
import com.cloud.utils.db.TransactionLegacy;

public class HighAvailabilityManagerExtImpl extends HighAvailabilityManagerImpl {

    @Inject
    UsageJobDao _usageJobDao;

    @Inject
    ConfigurationDao configDao;

    @Override
    public boolean configure(final String name, final Map<String, Object> xmlParams) throws ConfigurationException {
        super.configure(name, xmlParams);
        return true;
    }

    @Override
    public boolean start() {
        super.start();

        boolean enableUsage = new Boolean(configDao.getValue("enable.usage.server"));

        //By default, usage is enabled for production
        //Devs might override this value to disable usage in their setup
        if (enableUsage) {
            _executor.scheduleAtFixedRate(new UsageServerMonitorTask(), 60 * 60, 10 * 60, TimeUnit.SECONDS); // schedule starting in one hour to execute every 10 minutes
        }

        return true;
    }

    protected class UsageServerMonitorTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            if (logger.isInfoEnabled()) {
                logger.info("checking health of usage server");
            }

            try {
                boolean isRunning = false;
                TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
                try {
                    Date lastHeartbeat = _usageJobDao.getLastHeartbeat();
                    if (lastHeartbeat != null) {
                        long sinceLastHeartbeat = System.currentTimeMillis() - lastHeartbeat.getTime();
                        if (sinceLastHeartbeat <= (10 * 60 * 1000)) {
                            // if it's been less than 10 minutes since the last heartbeat, then it appears to be running, otherwise send an alert
                            isRunning = true;
                        }
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("usage server running? " + isRunning + ", heartbeat: " + lastHeartbeat);
                    }
                } finally {
                    txn.close();

                    // switch back to VMOPS db
                    TransactionLegacy swap = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
                    swap.close();
                }

                if (!isRunning) {
                    _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_USAGE_SERVER, 0, new Long(0), "No usage server process running",
                        "No usage server process has been detected, some attention is required");
                } else {
                    _alertMgr.clearAlert(AlertManager.AlertType.ALERT_TYPE_USAGE_SERVER, 0, 0);
                }
            } catch (Exception ex) {
                logger.warn("Error while monitoring usage job", ex);
            }
        }
    }
}
