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

import com.cloud.network.TungstenProvider;
import com.cloud.network.dao.TungstenProviderDao;
import com.cloud.network.element.TungstenProviderVO;
import com.cloud.utils.exception.CloudRuntimeException;
import net.juniper.tungsten.api.ApiConnector;
import net.juniper.tungsten.api.ApiPropertyBase;
import net.juniper.tungsten.api.ObjectReference;
import net.juniper.tungsten.api.types.Domain;
import net.juniper.tungsten.api.types.Project;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

public class TungstenDomainManagerImpl implements TungstenDomainManager {

    private static final Logger s_logger = Logger.getLogger(TungstenDomainManagerImpl.class);

    @Inject
    TungstenResourceManager tungstenResourceManager;
    @Inject
    TungstenProviderDao _tungstenProviderDao;

    public static final String TUNGSTEN_DEFAULT_DOMAIN = "default-domain";
    public static final String TUNGSTEN_DEFAULT_PROJECT = "default-project";

    @Override
    public List<TungstenProviderVO> getTungstenProviders() {
        return _tungstenProviderDao.findAll();
    }

    public void createDomainInTungsten(TungstenProvider tungstenProvider, String domainName, String domainUuid){
        try {
            ApiConnector _api = tungstenResourceManager.getApiConnector(tungstenProvider);
            //check if the domain exists in tungsten
            if(_api.findById(Domain.class, domainUuid) != null)
                return;
            //create tungsten domain
            Domain tungstenDomain = new Domain();
            tungstenDomain.setDisplayName(domainName);
            tungstenDomain.setName(domainName);
            tungstenDomain.setUuid(domainUuid);
            _api.create(tungstenDomain);
            // create default project in tungsten for this newly created domain
            Project tungstenDefaultProject = new Project();
            tungstenDefaultProject.setDisplayName(TUNGSTEN_DEFAULT_PROJECT);
            tungstenDefaultProject.setName(TUNGSTEN_DEFAULT_PROJECT);
            tungstenDefaultProject.setParent(tungstenDomain);
            _api.create(tungstenDefaultProject);
        } catch (IOException e) {
            s_logger.error("Failed creating domain resource in tungsten.");
            throw new CloudRuntimeException("Failed creating domain resource in tungsten.");
        }
    }

    public void deleteDomainFromTungsten(TungstenProvider tungstenProvider, String domainUuid){
        try {
            ApiConnector _api = tungstenResourceManager.getApiConnector(tungstenProvider);
            Domain domain = (Domain) _api.findById(Domain.class, domainUuid);
            //delete the projects of this domain
            for(ObjectReference<ApiPropertyBase> project : domain.getProjects()){
                _api.delete(Project.class, project.getUuid());
            }
            _api.delete(Domain.class, domainUuid);
        } catch (IOException e) {
            s_logger.error("Failed deleting domain resource from tungsten.");
            throw new CloudRuntimeException("Failed deleting domain resource from tungsten.");
        }
    }

    public Domain getDefaultTungstenDomain(TungstenProvider tungstenProvider) throws IOException {
        ApiConnector _api = tungstenResourceManager.getApiConnector(tungstenProvider);
        Domain domain = (Domain) _api.findByFQN(Domain.class, TUNGSTEN_DEFAULT_DOMAIN);
        return domain;
    }

    public Domain getTungstenDomainByUuid(TungstenProvider tungstenProvider, String domainUuid) throws IOException {
        ApiConnector _api = tungstenResourceManager.getApiConnector(tungstenProvider);
        return (Domain) _api.findById(Domain.class, domainUuid);
    }
}
