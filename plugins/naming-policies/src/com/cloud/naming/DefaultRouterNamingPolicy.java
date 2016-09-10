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

import com.cloud.network.router.VirtualRouter;
import com.cloud.vm.DomainRouterVO;


public class DefaultRouterNamingPolicy extends AbstractResourceNamingPolicy implements RouterNamingPolicy {

    @Inject
    private ConfigurationDao _configDao;

    private static final Logger s_logger = Logger.getLogger(DefaultRouterNamingPolicy.class);

    String instanceName;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        Map<String, String> configs = _configDao.getConfiguration("management-server", new HashMap<String, Object>());
        instanceName = configs.get("instance.name");
        return true;
    }

    @Override
    public void finalizeIdentifiers(DomainRouterVO vo) {
        if (!checkUuidSimple(vo.getUuid(), VirtualRouter.class)) {
            String oldUuid = vo.getUuid();
            vo.setUuid(generateUuid(vo.getId(), vo.getAccountId(), null));
            s_logger.warn("Invalid uuid for resource: '" + oldUuid + "'; changed to " + vo.getUuid());
        }
    }

    @Override
    public void checkCustomUuid(String uuid) {
        super.checkUuid(uuid, VirtualRouter.class);
    }

    @Override
    public String generateUuid(Long reasourceId, Long userId, String customUuid) {
        return super.generateUuid(VirtualRouter.class, reasourceId, userId, customUuid);
    }

    private String createName(Long resourceId, String prefix) {
        StringBuilder name = new StringBuilder();
        name.append(prefix);
        name.append(SEPARATOR).append(resourceId).append(SEPARATOR).append(instanceName);
        return name.toString();
    }

    @Override
    public String getElasticLBName(Long resourceId) {
        return createName(resourceId, "l");
    }

    @Override
    public String getInternalLBName(Long resourceId) {
        return createName(resourceId, "b");
    }

    @Override
    public String getRouterName(Long resourceId) {
        return createName(resourceId, "r");
    }

    @Override
    public boolean isValidRouterName(String routerName) {
        String[] tokens = routerName.split(SEPARATOR);
        if (tokens.length != 3 && tokens.length != 4) {
            return false;
        }

        if (!tokens[0].equals("r")) {
            return false;
        }

        try {
            Long.parseLong(tokens[1]);
        } catch (NumberFormatException ex) {
            return false;
        }

        return true;
    }


}
