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

package org.apache.cloudstack.network.contrail.model;

import net.juniper.contrail.api.ApiConnector;

import org.apache.cloudstack.network.contrail.management.ContrailManager;

import com.cloud.dc.dao.VlanDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;

/**
 * Collection of state necessary for model object to update the Contrail API server.
 *
 */
public class ModelController {
    ApiConnector _api;
    ContrailManager _manager;
    UserVmDao _vmDao;
    NetworkDao _networkDao;
    NicDao _nicDao;
    VlanDao _vlanDao;
    IPAddressDao _ipAddressDao;

    public ModelController(ContrailManager manager, ApiConnector api, UserVmDao vmDao, NetworkDao networkDao, NicDao nicDao, VlanDao vlanDao, IPAddressDao ipAddressDao) {
        _manager = manager;
        assert api != null;
        _api = api;
        assert vmDao != null;
        _vmDao = vmDao;
        assert networkDao != null;
        _networkDao = networkDao;
        assert nicDao != null;
        _nicDao = nicDao;
        assert vlanDao != null;
        _vlanDao = vlanDao;
        assert ipAddressDao != null;
        _ipAddressDao = ipAddressDao;
    }

    ApiConnector getApiAccessor() {
        return _api;
    }

    ContrailManager getManager() {
        return _manager;
    }

    UserVmDao getVmDao() {
        return _vmDao;
    }

    NetworkDao getNetworkDao() {
        return _networkDao;
    }

    NicDao getNicDao() {
        return _nicDao;
    }

    VlanDao getVlanDao() {
        return _vlanDao;
    }

    IPAddressDao getIPAddressDao() {
        return _ipAddressDao;
    }
}
