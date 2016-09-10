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
package com.cloud.naming;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.log4j.Logger;

import com.cloud.uservm.UserVm;
import com.cloud.vm.UserVmVO;

public class DefaultUserVMNamingPolicy extends AbstractResourceNamingPolicy implements UserVMNamingPolicy {

    private static final Logger s_logger = Logger.getLogger(DefaultUserVMNamingPolicy.class);
    @Inject
    private ConfigurationDao _configDao;

    String instanceName;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        Map<String, String> configs = _configDao.getConfiguration("management-server", new HashMap<String, Object>());
        instanceName = configs.get("instance.name");
        return true;
    }

    @Override
    public void finalizeIdentifiers(UserVmVO vo) {
        if (!checkUuidSimple(vo.getUuid(), UserVm.class)) {
            String oldUuid = vo.getUuid();
            vo.setUuid(generateUuid(vo.getId(), vo.getAccountId(), null));
            s_logger.warn("Invalid uuid for resource: '" + oldUuid + "'; changed to " + vo.getUuid());
        }
    }


    @Override
    public void checkCustomUuid(String uuid) {
        super.checkUuid(uuid, UserVm.class);
    }

    @Override
    public String generateUuid(Long resourceId, Long userId, String customUuid) {
        return super.generateUuid(UserVm.class, resourceId, userId, customUuid);
    }

    @Override
    public String getHostName(Long resourceId, Long userId, String uuid) {
        return instanceName + "-" + uuid;
    }

    @Override
    public String getInstanceName(Long resourceId, Long userId, String customUuid) {
        StringBuilder vmName = new StringBuilder("i");
        vmName.append("-").append(userId).append("-").append(resourceId);
        vmName.append("-").append(instanceName);
        return vmName.toString();
    }

    @Override
    public String getServiceVmName(Long resourceId, Long userId) {
        StringBuilder vmName = new StringBuilder("i");
        vmName.append("-").append(resourceId).append("-").append(resourceId);
        vmName.append("-").append("SRV");
        return vmName.toString();
    }

}
