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
package org.apache.cloudstack.storage.test;

import java.io.IOException;

import org.apache.cloudstack.acl.APIChecker;
import org.apache.cloudstack.engine.service.api.OrchestrationService;
import org.apache.cloudstack.storage.HostEndpointRpcServer;
import org.apache.cloudstack.storage.endpoint.EndPointSelector;
import org.apache.cloudstack.storage.test.ChildTestConfiguration.Library;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import com.cloud.agent.AgentManager;
import com.cloud.alert.AlertManager;
import com.cloud.cluster.ClusteredAgentRebalanceService;
import com.cloud.cluster.agentlb.dao.HostTransferMapDao;
import com.cloud.cluster.agentlb.dao.HostTransferMapDaoImpl;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.configuration.dao.ConfigurationDaoImpl;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.ClusterDaoImpl;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterDaoImpl;
import com.cloud.dc.dao.DataCenterIpAddressDaoImpl;
import com.cloud.dc.dao.DataCenterLinkLocalIpAddressDaoImpl;
import com.cloud.dc.dao.DataCenterVnetDaoImpl;
import com.cloud.dc.dao.DcDetailsDaoImpl;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.HostPodDaoImpl;
import com.cloud.dc.dao.PodVlanDaoImpl;
import com.cloud.domain.dao.DomainDao;
import com.cloud.domain.dao.DomainDaoImpl;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.host.dao.HostDetailsDaoImpl;
import com.cloud.host.dao.HostTagsDao;
import com.cloud.host.dao.HostTagsDaoImpl;
import com.cloud.server.auth.UserAuthenticator;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.StoragePoolHostDaoImpl;
import com.cloud.storage.dao.VMTemplateDaoImpl;
import com.cloud.storage.dao.VMTemplateDetailsDao;
import com.cloud.storage.dao.VMTemplateDetailsDaoImpl;
import com.cloud.storage.dao.VMTemplateHostDaoImpl;
import com.cloud.storage.dao.VMTemplatePoolDaoImpl;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.dao.VMTemplateZoneDaoImpl;
import com.cloud.storage.dao.VolumeDaoImpl;
import com.cloud.storage.dao.VolumeHostDaoImpl;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.tags.dao.ResourceTagsDaoImpl;
import com.cloud.utils.component.SpringComponentScanUtils;
import com.cloud.vm.dao.NicDaoImpl;
import com.cloud.vm.dao.VMInstanceDaoImpl;
@Configuration
@ComponentScan(basePackageClasses={
        NicDaoImpl.class,
        VMInstanceDaoImpl.class,
        VMTemplateHostDaoImpl.class,
        VolumeHostDaoImpl.class,
        VolumeDaoImpl.class,
        VMTemplatePoolDaoImpl.class,
        ResourceTagsDaoImpl.class,
        VMTemplateDaoImpl.class,
        MockStorageMotionStrategy.class
},
includeFilters={@Filter(value=Library.class, type=FilterType.CUSTOM)},
useDefaultFilters=false
)
public class ChildTestConfiguration extends TestConfiguration {
	
	@Override
	@Bean
	public HostDao hostDao() {
		HostDao dao = super.hostDao();
		HostDao nDao = Mockito.spy(dao);
		return nDao;
	}
	
	@Bean
	public EndPointSelector selector() {
	    return Mockito.mock(EndPointSelector.class);
	}
	@Bean
	public DataCenterDao dcDao() {
	    return new DataCenterDaoImpl();
	}
	@Bean
	public HostDetailsDao hostDetailsDao() {
	    return new HostDetailsDaoImpl();
	}
	
	@Bean
	public HostTagsDao hostTagsDao() {
	    return new HostTagsDaoImpl();
	}
	
	@Bean ClusterDao clusterDao() {
	    return new ClusterDaoImpl();
	}
	
	@Bean HostTransferMapDao hostTransferDao() {
	    return new HostTransferMapDaoImpl();
	}
	@Bean DataCenterIpAddressDaoImpl dataCenterIpAddressDaoImpl() {
	    return new DataCenterIpAddressDaoImpl();
	}
	@Bean DataCenterLinkLocalIpAddressDaoImpl dataCenterLinkLocalIpAddressDaoImpl() {
	    return new DataCenterLinkLocalIpAddressDaoImpl();
	}
	@Bean DataCenterVnetDaoImpl dataCenterVnetDaoImpl() {
	    return new DataCenterVnetDaoImpl();
	}
	@Bean PodVlanDaoImpl podVlanDaoImpl() {
	    return new PodVlanDaoImpl();
	}
	@Bean DcDetailsDaoImpl dcDetailsDaoImpl() {
	    return new DcDetailsDaoImpl();
	}
	@Bean HostPodDao hostPodDao() {
	    return new HostPodDaoImpl();
	}
	@Bean StoragePoolHostDao storagePoolHostDao() {
	    return new StoragePoolHostDaoImpl();
	}
	@Bean VMTemplateZoneDao templateZoneDao() {
	    return new VMTemplateZoneDaoImpl();
	}
	@Bean VMTemplateDetailsDao templateDetailsDao() {
	    return new VMTemplateDetailsDaoImpl();
	}
	@Bean ConfigurationDao configDao() {
	    return new ConfigurationDaoImpl();
	}
	@Bean
	public AgentManager agentMgr() {
		return new DirectAgentManagerSimpleImpl();
	}
	@Bean DomainDao domainDao() {
	    return new DomainDaoImpl();
	}
	
    @Bean
    public HostEndpointRpcServer rpcServer() {
        return new MockHostEndpointRpcServerDirectCallResource();
    }
    @Bean
    public ClusteredAgentRebalanceService _rebalanceService() {
        return Mockito.mock(ClusteredAgentRebalanceService.class);
    }
    @Bean
    public UserAuthenticator authenticator() {
        return Mockito.mock(UserAuthenticator.class);
    }
    @Bean
    public OrchestrationService orchSrvc() {
        return Mockito.mock(OrchestrationService.class);
    }
    @Bean
    public APIChecker apiChecker() {
        return Mockito.mock(APIChecker.class);
    }
    
    @Bean
    public SnapshotManager snapshotMgr() {
        return Mockito.mock(SnapshotManager.class);
    }
    
    @Bean
    public AlertManager alertMgr() {
        return Mockito.mock(AlertManager.class);
    }

    public static class Library implements TypeFilter {

        @Override
        public boolean match(MetadataReader mdr, MetadataReaderFactory arg1) throws IOException {
            mdr.getClassMetadata().getClassName();
            ComponentScan cs = ChildTestConfiguration.class.getAnnotation(ComponentScan.class);
            return SpringComponentScanUtils.includedInBasePackageClasses(mdr.getClassMetadata().getClassName(), cs);
        }

    }
/*	@Override
	@Bean
	public PrimaryDataStoreDao primaryDataStoreDao() {
		return Mockito.mock(PrimaryDataStoreDaoImpl.class);
	}*/
}
