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
package org.apache.cloudstack.storage.allocator;

import java.io.IOException;

import org.apache.cloudstack.storage.allocator.StorageAllocatorTestConfiguration.Library;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDaoImpl;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import com.cloud.dc.dao.DataCenterDaoImpl;
import com.cloud.domain.dao.DomainDaoImpl;
import com.cloud.host.dao.HostDaoImpl;
import com.cloud.storage.StorageManager;
import com.cloud.storage.dao.StoragePoolDetailsDaoImpl;
import com.cloud.storage.dao.VMTemplateDaoImpl;
import com.cloud.utils.component.SpringComponentScanUtils;
import com.cloud.vm.UserVmManager;


@Configuration
@ComponentScan(basePackageClasses={
		StoragePoolDetailsDaoImpl.class,
		PrimaryDataStoreDaoImpl.class,
		VMTemplateDaoImpl.class,
		HostDaoImpl.class,
		DomainDaoImpl.class,
		DataCenterDaoImpl.class},
        includeFilters={@Filter(value=Library.class, type=FilterType.CUSTOM)},
        useDefaultFilters=false
        )
public class StorageAllocatorTestConfiguration {
	@Bean
	public UserVmManager UserVmManager() {
		return Mockito.mock(UserVmManager.class);
	}
	@Bean
	public StorageManager StorageManager() {
		return Mockito.mock(StorageManager.class);
	}

	public static class Library implements TypeFilter {

		@Override
		public boolean match(MetadataReader mdr, MetadataReaderFactory arg1) throws IOException {
			mdr.getClassMetadata().getClassName();
			ComponentScan cs = StorageAllocatorTestConfiguration.class.getAnnotation(ComponentScan.class);
			return SpringComponentScanUtils.includedInBasePackageClasses(mdr.getClassMetadata().getClassName(), cs);
		}
	}
}
