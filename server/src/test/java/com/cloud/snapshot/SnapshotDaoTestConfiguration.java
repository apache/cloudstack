// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.cloud.snapshot;

import com.cloud.cluster.agentlb.dao.HostTransferMapDaoImpl;
import com.cloud.dc.dao.ClusterDaoImpl;
import com.cloud.dc.dao.HostPodDaoImpl;
import com.cloud.host.dao.HostDaoImpl;
import com.cloud.host.dao.HostDetailsDaoImpl;
import com.cloud.host.dao.HostTagsDaoImpl;
import com.cloud.storage.dao.SnapshotDaoImpl;
import com.cloud.storage.dao.VolumeDaoImpl;
import com.cloud.tags.dao.ResourceTagsDaoImpl;
import com.cloud.vm.dao.NicDaoImpl;
import com.cloud.vm.dao.VMInstanceDaoImpl;
import org.apache.cloudstack.test.utils.SpringUtils;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import java.io.IOException;

@Configuration
@ComponentScan(basePackageClasses = {SnapshotDaoImpl.class, ResourceTagsDaoImpl.class, VMInstanceDaoImpl.class, VolumeDaoImpl.class, NicDaoImpl.class, HostDaoImpl.class,
    HostDetailsDaoImpl.class, HostTagsDaoImpl.class, HostTransferMapDaoImpl.class, ClusterDaoImpl.class, HostPodDaoImpl.class},
               includeFilters = {@Filter(value = SnapshotDaoTestConfiguration.Library.class, type = FilterType.CUSTOM)},
               useDefaultFilters = false)
public class SnapshotDaoTestConfiguration {

    public static class Library implements TypeFilter {

        @Override
        public boolean match(MetadataReader mdr, MetadataReaderFactory arg1) throws IOException {
            mdr.getClassMetadata().getClassName();
            ComponentScan cs = SnapshotDaoTestConfiguration.class.getAnnotation(ComponentScan.class);
            return SpringUtils.includedInBasePackageClasses(mdr.getClassMetadata().getClassName(), cs);
        }

    }
}
