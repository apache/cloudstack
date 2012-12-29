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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;


@Service("zoneService")
public class ZoneEntityImpl implements ZoneEntity {
    String _id;
    String _name;

    // This is a test constructor
    public ZoneEntityImpl(String id, String name) {
        _id = id;
        _name = name;
    }

    public ZoneEntityImpl() {
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
    public String getUuid() {
        return _id;
    }

    @Override
    public long getId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getCurrentState() {
        // TODO Auto-generated method stub
        return "state";
    }

    @Override
    public String getDesiredState() {
        // TODO Auto-generated method stub
        return "desired_state";
    }

    @Override
    public Date getCreatedTime() {
        // TODO Auto-generated method stub
        return new Date();
    }

    @Override
    public Date getLastUpdatedTime() {
        // TODO Auto-generated method stub
        return new Date();
    }

    @Override
    public String getOwner() {
        // TODO Auto-generated method stub
        return "owner";
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
    public State getState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDns1() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDns2() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getGuestNetworkCidr() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public Long getDomainId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDomain() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NetworkType getNetworkType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getInternalDns1() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getInternalDns2() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDnsProvider() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getGatewayProvider() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getFirewallProvider() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDhcpProvider() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getLoadBalancerProvider() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getUserDataProvider() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getVpnProvider() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isSecurityGroupEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Map<String, String> getDetails() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setDetails(Map<String, String> details) {
        // TODO Auto-generated method stub

    }

    @Override
    public AllocationState getAllocationState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getZoneToken() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isLocalStorageEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<PodEntity> listPods() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> listPodIds() {
        List<String> podIds = new ArrayList<String>();
        podIds.add("pod-uuid-1");
        podIds.add("pod-uuid-2");
        return podIds;
    }
}
