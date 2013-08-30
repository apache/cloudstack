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
package org.apache.cloudstack.framework.config;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.test.utils.SpringUtils;

import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.EntityManager;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class ConfigDepotAdminTest {
    private final ConfigKey<Integer> DynamicIntCK = new ConfigKey<Integer>(Integer.class, "dynIntKey", "Advance", "10", "Test Key", true);
    private final ConfigKey<Integer> StaticIntCK = new ConfigKey<Integer>(Integer.class, "statIntKey", "Advance", "10", "Test Key", false);

    @Inject
    Configurable configurable;

    @Inject
    ConfigDepot _configDepot;

    @Inject
    ConfigDepotAdmin _depotAdmin;

    @Inject
    EntityManager _entityMgr;

    @Inject
    ConfigurationDao _configDao;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        ComponentContext.initComponentsLifeCycle();  // NOTE #3
    }

    @Test
    public void testAutoPopulation() {
        ConfigurationVO dynamicIntCV = new ConfigurationVO("UnitTestComponent", DynamicIntCK);
        dynamicIntCV.setValue("100");
        ConfigurationVO staticIntCV = new ConfigurationVO("UnitTestComponent", StaticIntCK);
        dynamicIntCV.setValue("200");
        
        when(configurable.getConfigComponentName()).thenReturn("UnitTestComponent");
        when(configurable.getConfigKeys()).thenReturn(new ConfigKey<?>[] {DynamicIntCK, StaticIntCK});
        when(_entityMgr.findById(org.apache.cloudstack.config.Configuration.class, DynamicIntCK.key())).thenReturn(dynamicIntCV);
        when(_entityMgr.findById(org.apache.cloudstack.config.Configuration.class, StaticIntCK.key())).thenReturn(staticIntCV);
        when(_configDao.findById(StaticIntCK.key())).thenReturn(null);
        when(_configDao.findById(DynamicIntCK.key())).thenReturn(dynamicIntCV);
        when(_configDao.persist(any(ConfigurationVO.class))).thenReturn(dynamicIntCV);

        _depotAdmin.populateConfigurations();

        // This is once because DynamicIntCK is returned.
        verify(_configDao, times(1)).persist(any(ConfigurationVO.class));

        when(_configDao.findById(DynamicIntCK.key())).thenReturn(dynamicIntCV);
        _depotAdmin.populateConfigurations();
        // This is two because DynamicIntCK also returns null.
        verify(_configDao, times(2)).persist(any(ConfigurationVO.class));
    }
    
    @Configuration
    @ComponentScan(basePackageClasses = {ConfigDepotImpl.class}, includeFilters = {@Filter(value = TestConfiguration.Library.class, type = FilterType.CUSTOM)}, useDefaultFilters = false)
    static class TestConfiguration extends SpringUtils.CloudStackTestConfiguration {
        @Bean
        public Configurable configurable() {
            return mock(Configurable.class);
        }

        @Bean
        public EntityManager entityMgr() {
            return mock(EntityManager.class);
        }

        @Bean
        public ConfigurationDao configurationDao() {
            return mock(ConfigurationDao.class);
        }

        public static class Library implements TypeFilter {
            @Override
            public boolean match(MetadataReader mdr, MetadataReaderFactory arg1) throws IOException {
                ComponentScan cs = TestConfiguration.class.getAnnotation(ComponentScan.class);
                return SpringUtils.includedInBasePackageClasses(mdr.getClassMetadata().getClassName(), cs);
            }
        }
    }
}
