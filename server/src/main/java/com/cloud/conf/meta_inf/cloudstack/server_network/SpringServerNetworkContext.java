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
package com.cloud.conf.meta_inf.cloudstack.server_network;

import com.cloud.network.element.ConfigDriveNetworkElement;
import com.cloud.network.element.SecurityGroupElement;
import com.cloud.network.element.VirtualRouterElement;
import com.cloud.network.guru.ControlNetworkGuru;
import com.cloud.network.guru.DirectNetworkGuru;
import com.cloud.network.guru.DirectPodBasedNetworkGuru;
import com.cloud.network.guru.ExternalGuestNetworkGuru;
import com.cloud.network.guru.PodBasedNetworkGuru;
import com.cloud.network.guru.PrivateNetworkGuru;
import com.cloud.network.guru.PublicNetworkGuru;
import com.cloud.network.guru.StorageNetworkGuru;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Generated Java based configuration
 * 
 */
@Configuration
public class SpringServerNetworkContext {


    @Bean("PodBasedNetworkGuru")
    public PodBasedNetworkGuru PodBasedNetworkGuru() {
        PodBasedNetworkGuru bean = new PodBasedNetworkGuru();
        bean.setName("PodBasedNetworkGuru");
        return bean;
    }

    @Bean("ControlNetworkGuru")
    public ControlNetworkGuru ControlNetworkGuru() {
        ControlNetworkGuru bean = new ControlNetworkGuru();
        bean.setName("ControlNetworkGuru");
        return bean;
    }

    @Bean("StorageNetworkGuru")
    public StorageNetworkGuru StorageNetworkGuru() {
        StorageNetworkGuru bean = new StorageNetworkGuru();
        bean.setName("StorageNetworkGuru");
        return bean;
    }

    @Bean("PrivateNetworkGuru")
    public PrivateNetworkGuru PrivateNetworkGuru() {
        PrivateNetworkGuru bean = new PrivateNetworkGuru();
        bean.setName("PrivateNetworkGuru");
        return bean;
    }

    @Bean("ConfigDrive")
    public ConfigDriveNetworkElement ConfigDrive() {
        ConfigDriveNetworkElement bean = new ConfigDriveNetworkElement();
        bean.setName("ConfigDrive");
        return bean;
    }

    @Bean("ExternalGuestNetworkGuru")
    public ExternalGuestNetworkGuru ExternalGuestNetworkGuru() {
        ExternalGuestNetworkGuru bean = new ExternalGuestNetworkGuru();
        bean.setName("ExternalGuestNetworkGuru");
        return bean;
    }

    @Bean("DirectNetworkGuru")
    public DirectNetworkGuru DirectNetworkGuru() {
        DirectNetworkGuru bean = new DirectNetworkGuru();
        bean.setName("DirectNetworkGuru");
        return bean;
    }

    @Bean("PublicNetworkGuru")
    public PublicNetworkGuru PublicNetworkGuru() {
        PublicNetworkGuru bean = new PublicNetworkGuru();
        bean.setName("PublicNetworkGuru");
        return bean;
    }

    @Bean("DirectPodBasedNetworkGuru")
    public DirectPodBasedNetworkGuru DirectPodBasedNetworkGuru() {
        DirectPodBasedNetworkGuru bean = new DirectPodBasedNetworkGuru();
        bean.setName("DirectPodBasedNetworkGuru");
        return bean;
    }

    @Bean("SecurityGroupProvider")
    public SecurityGroupElement SecurityGroupProvider() {
        SecurityGroupElement bean = new SecurityGroupElement();
        bean.setName("SecurityGroupProvider");
        return bean;
    }

    @Bean("VirtualRouter")
    public VirtualRouterElement VirtualRouter() {
        VirtualRouterElement bean = new VirtualRouterElement();
        bean.setName("VirtualRouter");
        return bean;
    }

}
