
package com.cloud.conf.meta_inf.cloudstack.server_network;

import com.cloud.network.element.ConfigDriveNetworkElement;
import com.cloud.network.element.SecurityGroupElement;
import com.cloud.network.element.VirtualRouterElement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Generated Java based configuration
 * 
 */
@Configuration
public class SpringServerNetworkContext {


    @Bean("PodBasedNetworkGuru")
    public com.cloud.network.guru.PodBasedNetworkGuru PodBasedNetworkGuru() {
        com.cloud.network.guru.PodBasedNetworkGuru bean = new com.cloud.network.guru.PodBasedNetworkGuru();
        bean.setName("PodBasedNetworkGuru");
        return bean;
    }

    @Bean("ControlNetworkGuru")
    public com.cloud.network.guru.ControlNetworkGuru ControlNetworkGuru() {
        com.cloud.network.guru.ControlNetworkGuru bean = new com.cloud.network.guru.ControlNetworkGuru();
        bean.setName("ControlNetworkGuru");
        return bean;
    }

    @Bean("StorageNetworkGuru")
    public com.cloud.network.guru.StorageNetworkGuru StorageNetworkGuru() {
        com.cloud.network.guru.StorageNetworkGuru bean = new com.cloud.network.guru.StorageNetworkGuru();
        bean.setName("StorageNetworkGuru");
        return bean;
    }

    @Bean("PrivateNetworkGuru")
    public com.cloud.network.guru.PrivateNetworkGuru PrivateNetworkGuru() {
        com.cloud.network.guru.PrivateNetworkGuru bean = new com.cloud.network.guru.PrivateNetworkGuru();
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
    public com.cloud.network.guru.ExternalGuestNetworkGuru ExternalGuestNetworkGuru() {
        com.cloud.network.guru.ExternalGuestNetworkGuru bean = new com.cloud.network.guru.ExternalGuestNetworkGuru();
        bean.setName("ExternalGuestNetworkGuru");
        return bean;
    }

    @Bean("DirectNetworkGuru")
    public com.cloud.network.guru.DirectNetworkGuru DirectNetworkGuru() {
        com.cloud.network.guru.DirectNetworkGuru bean = new com.cloud.network.guru.DirectNetworkGuru();
        bean.setName("DirectNetworkGuru");
        return bean;
    }

    @Bean("PublicNetworkGuru")
    public com.cloud.network.guru.PublicNetworkGuru PublicNetworkGuru() {
        com.cloud.network.guru.PublicNetworkGuru bean = new com.cloud.network.guru.PublicNetworkGuru();
        bean.setName("PublicNetworkGuru");
        return bean;
    }

    @Bean("DirectPodBasedNetworkGuru")
    public com.cloud.network.guru.DirectPodBasedNetworkGuru DirectPodBasedNetworkGuru() {
        com.cloud.network.guru.DirectPodBasedNetworkGuru bean = new com.cloud.network.guru.DirectPodBasedNetworkGuru();
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
