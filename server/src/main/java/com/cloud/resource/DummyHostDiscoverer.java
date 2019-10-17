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
package com.cloud.resource;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.utils.component.AdapterBase;

@Component
public class DummyHostDiscoverer extends AdapterBase implements Discoverer {
    private static final Logger s_logger = Logger.getLogger(DummyHostDiscoverer.class);

    @Override
    public Map<ServerResource, Map<String, String>> find(long dcId, Long podId, Long clusterId, URI url, String username, String password, List<String> hostTags) {
        if (!url.getScheme().equals("dummy")) {
            return null;
        }

        Map<ServerResource, Map<String, String>> resources = new HashMap<ServerResource, Map<String, String>>();
        Map<String, Object> params = new HashMap<String, Object>();
        Map<String, String> details = new HashMap<String, String>();

        details.put("url", url.toString());
        details.put("username", username);
        details.put("password", password);

        params.put("url", url.toString());
        params.put("username", username);
        params.put("password", password);
        params.put("zone", Long.toString(dcId));
        params.put("guid", UUID.randomUUID().toString());
        params.put("pod", Long.toString(podId));

        DummyHostServerResource resource = new DummyHostServerResource();
        try {
            resource.configure("Dummy Host Server", params);
        } catch (ConfigurationException e) {
            s_logger.warn("Unable to instantiate dummy host server resource");
        }
        resource.start();
        resources.put(resource, details);
        return resources;
    }

    @Override
    public boolean matchHypervisor(String hypervisor) {
        return false;
    }

    @Override
    public Hypervisor.HypervisorType getHypervisorType() {
        return Hypervisor.HypervisorType.None;
    }

    @Override
    public void postDiscovery(List<HostVO> hosts, long msId) {
        //do nothing
    }

    @Override
    public void putParam(Map<String, String> params) {
        // TODO Auto-generated method stub

    }

    @Override
    public ServerResource reloadResource(HostVO host) {
        // TODO Auto-generated method stub
        return null;
    }
}
