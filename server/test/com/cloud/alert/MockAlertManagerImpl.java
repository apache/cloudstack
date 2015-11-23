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

package com.cloud.alert;

import java.util.Map;

import javax.naming.ConfigurationException;

import com.cloud.utils.component.ManagerBase;

public class MockAlertManagerImpl extends ManagerBase implements AlertManager {

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#configure(java.lang.String, java.util.Map)
     */
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#start()
     */
    @Override
    public boolean start() {
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#stop()
     */
    @Override
    public boolean stop() {
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#getName()
     */
    @Override
    public String getName() {
        return "MockAlertManagerImpl";
    }

    /* (non-Javadoc)
     * @see com.cloud.alert.AlertManager#clearAlert(short, long, long)
     */
    @Override
    public void clearAlert(AlertType alertType, long dataCenterId, long podId) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.alert.AlertManager#sendAlert(short, long, java.lang.Long, java.lang.String, java.lang.String)
     */
    @Override
    public void sendAlert(AlertType alertType, long dataCenterId, Long podId, String subject, String body) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.alert.AlertManager#recalculateCapacity()
     */
    @Override
    public void recalculateCapacity() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean generateAlert(AlertType alertType, long dataCenterId, Long podId, String msg) {
        // TODO Auto-generated method stub
        return false;
    }

}
