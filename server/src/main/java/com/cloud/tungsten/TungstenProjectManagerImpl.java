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
package com.cloud.tungsten;

import com.cloud.domain.Domain;
import com.cloud.network.TungstenProvider;
import com.cloud.network.dao.TungstenProviderDao;
import com.cloud.network.element.TungstenProviderVO;
import com.cloud.utils.exception.CloudRuntimeException;
import net.juniper.tungsten.api.ApiConnector;
import net.juniper.tungsten.api.types.Project;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

@Component
public class TungstenProjectManagerImpl implements TungstenProjectManager {

    private static final Logger s_logger = Logger.getLogger(TungstenProjectManagerImpl.class);

    @Inject
    TungstenDomainManager tungstenDomainManager;
    @Inject
    TungstenResourceManager tungstenResourceManager;
    @Inject
    TungstenProviderDao _tungstenProviderDao;

    @Override
    public List<TungstenProviderVO> getTungstenProviders() {
        return _tungstenProviderDao.findAll();
    }

    @Override
    public void createProjectInTungsten(TungstenProvider tungstenProvider, String projectUuid, String projectName, Domain domain) {
        try {
            ApiConnector _api = tungstenResourceManager.getApiConnector(tungstenProvider);
            //check if the project already exists in tungsten
            if(_api.findById(Project.class, projectUuid) != null)
                return;
            //Create tungsten project
            Project tungstenProject = new Project();
            tungstenProject.setDisplayName(projectName);
            tungstenProject.setName(projectName);
            tungstenProject.setUuid(projectUuid);
            net.juniper.tungsten.api.types.Domain tungstenDomain;

            if (domain == null)
                tungstenDomain = tungstenDomainManager.getDefaultTungstenDomain(tungstenProvider);
            else {
                tungstenDomain = tungstenDomainManager.getTungstenDomainByUuid(tungstenProvider, domain.getUuid());
                if (tungstenDomain == null)
                    tungstenDomainManager.createDomainInTungsten(tungstenProvider, domain.getName(), domain.getUuid());
            }
            tungstenProject.setParent(tungstenDomain);
            _api.create(tungstenProject);
        } catch (IOException e) {
            s_logger.error("Failed creating project resource in tungsten.");
            throw new CloudRuntimeException("Failed creating project resource in tungsten.");
        }
    }

    public void deleteProjectFromTungsten(TungstenProvider tungstenProvider, String projectUuid) {
        ApiConnector _api = tungstenResourceManager.getApiConnector(tungstenProvider);
        try {
            Project project = (Project) _api.findById(Project.class, projectUuid);
            if (project != null)
                _api.delete(Project.class, projectUuid);
        } catch (IOException e) {
            s_logger.error("Failed deleting project resource from tungsten.");
            throw new CloudRuntimeException("Failed deleting project resource from tungsten.");
        }
    }
}
