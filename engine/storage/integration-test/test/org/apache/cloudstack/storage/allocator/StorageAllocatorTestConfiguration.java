package org.apache.cloudstack.storage.allocator;

import java.io.IOException;

import org.apache.cloudstack.storage.allocator.StorageAllocatorTestConfiguration.Library;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import com.cloud.cluster.agentlb.dao.HostTransferMapDaoImpl;
import com.cloud.configuration.dao.ConfigurationDaoImpl;
import com.cloud.dc.dao.ClusterDaoImpl;
import com.cloud.dc.dao.DataCenterDaoImpl;
import com.cloud.domain.dao.DomainDaoImpl;
import com.cloud.host.dao.HostDaoImpl;
import com.cloud.host.dao.HostDetailsDaoImpl;
import com.cloud.host.dao.HostTagsDaoImpl;
import com.cloud.storage.StorageManager;
import com.cloud.storage.dao.StoragePoolDaoImpl;
import com.cloud.storage.dao.StoragePoolDetailsDaoImpl;
import com.cloud.storage.dao.VMTemplateDaoImpl;
import com.cloud.utils.component.SpringComponentScanUtils;
import com.cloud.vm.UserVmManager;


@Configuration
@ComponentScan(basePackageClasses={
		StoragePoolDetailsDaoImpl.class,
		StoragePoolDaoImpl.class,
		VMTemplateDaoImpl.class,
		HostDaoImpl.class,
		DomainDaoImpl.class,
		DataCenterDaoImpl.class,
      	},
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
