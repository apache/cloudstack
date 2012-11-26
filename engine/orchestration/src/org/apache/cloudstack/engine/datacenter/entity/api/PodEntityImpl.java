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
package org.apache.cloudstack.engine.datacenter.entity.api;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.cloud.org.Cluster;

public class PodEntityImpl implements PodEntity {
    String _uuid;
    String _name;

    public PodEntityImpl(String uuid, String name) {
        _uuid = uuid;
        _name = name;
    }

    @Override
    public boolean enable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean disable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deactivate() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean reactivate() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public State getState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getUuid() {
        return _uuid;
    }

    @Override
    public long getId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getCurrentState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDesiredState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getCreatedTime() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getLastUpdatedTime() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getOwner() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> getDetails(String source) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getDetailSources() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addDetail(String source, String name, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delDetail(String source, String name, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateDetail(String source, String name, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<Method> getApplicableActions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getCidrAddress() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getCidrSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getGateway() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getDataCenterId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public AllocationState getAllocationState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean getExternalDhcp() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<Cluster> listClusters() {
        // TODO Auto-generated method stub
        return null;
    }

}
