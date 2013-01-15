/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.datastore.lifecycle;

import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.storage.command.AttachPrimaryDataStoreCmd;
import org.apache.cloudstack.storage.command.CreatePrimaryDataStoreCmd;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;

import com.cloud.agent.api.Answer;
import com.cloud.utils.exception.CloudRuntimeException;

public class DefaultXenPrimaryDataStoreLifeCycle extends DefaultPrimaryDataStoreLifeCycleImpl {

    /**
     * @param dataStoreDao
     * @param dataStore
     */
    public DefaultXenPrimaryDataStoreLifeCycle(PrimaryDataStoreDao dataStoreDao) {
        super(dataStoreDao);
        // TODO Auto-generated constructor stub
    }
    
    @Override
    public void attachCluster() {
        String result = null;
        //send one time is enough, as xenserver is clustered
        /*CreatePrimaryDataStoreCmd cmd = new CreatePrimaryDataStoreCmd(this.dataStore.getDataStoreTO());
        String result = null;
        for (EndPoint ep : dataStore.getEndPoints()) {
            Answer answer = ep.sendMessage(cmd);
            if (answer.getResult()) {
                return;
            }
            result = answer.getDetails();
        }*/
        
        if (result != null)
            throw new CloudRuntimeException("AttachPrimaryDataStoreCmd failed: " + result);
        
        super.attachCluster();
    }
}
