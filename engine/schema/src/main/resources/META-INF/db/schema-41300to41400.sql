-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.

--;
-- Schema upgrade from 4.13.0.0 to 4.14.0.0
--;

-- KVM: enable storage data motion on KVM hypervisor_capabilities
UPDATE `cloud`.`hypervisor_capabilities` SET `storage_motion_supported` = 1 WHERE `hypervisor_capabilities`.`hypervisor_type` = 'KVM';

-- Fix OS category for some Ubuntu and RedHat OS-es
UPDATE `cloud`.`guest_os` SET `category_id`='10' WHERE `id`=277 AND display_name="Ubuntu 17.04";
UPDATE `cloud`.`guest_os` SET `category_id`='10' WHERE `id`=278 AND display_name="Ubuntu 17.10";
UPDATE `cloud`.`guest_os` SET `category_id`='10' WHERE `id`=279 AND display_name="Ubuntu 18.04 LTS";
UPDATE `cloud`.`guest_os` SET `category_id`='10' WHERE `id`=280 AND display_name="Ubuntu 18.10";
UPDATE `cloud`.`guest_os` SET `category_id`='10' WHERE `id`=281 AND display_name="Ubuntu 19.04";
UPDATE `cloud`.`guest_os` SET `category_id`='4' WHERE `id`=282 AND display_name="Red Hat Enterprise Linux 7.3";
UPDATE `cloud`.`guest_os` SET `category_id`='4' WHERE `id`=283 AND display_name="Red Hat Enterprise Linux 7.4";
UPDATE `cloud`.`guest_os` SET `category_id`='4' WHERE `id`=284 AND display_name="Red Hat Enterprise Linux 7.5";
UPDATE `cloud`.`guest_os` SET `category_id`='4' WHERE `id`=285 AND display_name="Red Hat Enterprise Linux 7.6";
UPDATE `cloud`.`guest_os` SET `category_id`='4' WHERE `id`=286 AND display_name="Red Hat Enterprise Linux 8.0";

-- Kubernetes service
CREATE TABLE IF NOT EXISTS `cloud`.`kubernetes_supported_version` (
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `uuid` varchar(40) DEFAULT NULL COMMENT 'uuid',
    `name` varchar(255) NOT NULL COMMENT 'kubernetes version name',
    `kubernetes_version` varchar(32) NOT NULL COMMENT 'kubernetes semantic version',
    `iso_id` bigint unsigned NOT NULL COMMENT 'kubernetes version binary ISO id',
    `zone_id` bigint unsigned DEFAULT NULL COMMENT 'zone id in which kubernetes version is available',
    `created` datetime NOT NULL COMMENT 'date created',
    `removed` datetime COMMENT 'date removed if not null',

    PRIMARY KEY(`id`),
    CONSTRAINT `fk_kubernetes_supported_version__iso_id` FOREIGN KEY `fk_kubernetes_supported_version__iso_id`(`iso_id`) REFERENCES `vm_template`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_kubernetes_supported_version__zone_id` FOREIGN KEY `fk_kubernetes_supported_version__zone_id`(`zone_id`) REFERENCES `data_center` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`kubernetes_cluster` (
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `uuid` varchar(40) DEFAULT NULL,
    `name` varchar(255) NOT NULL,
    `description` varchar(4096) COMMENT 'display text for this kubernetes cluster',
    `zone_id` bigint unsigned NOT NULL COMMENT 'zone id',
    `kubernetes_version_id` bigint unsigned NOT NULL COMMENT 'kubernetes version id for the cluster',
    `service_offering_id` bigint unsigned COMMENT 'service offering id for the cluster VM',
    `template_id` bigint unsigned COMMENT 'vm_template.id',
    `network_id` bigint unsigned COMMENT 'network this kubernetes cluster uses',
    `master_node_count` bigint NOT NULL default '0',
    `node_count` bigint NOT NULL default '0',
    `account_id` bigint unsigned NOT NULL COMMENT 'owner of this cluster',
    `domain_id` bigint unsigned NOT NULL COMMENT 'owner of this cluster',
    `state` char(32) NOT NULL COMMENT 'current state of this cluster',
    `key_pair` varchar(40),
    `cores` bigint unsigned NOT NULL COMMENT 'number of cores',
    `memory` bigint unsigned NOT NULL COMMENT 'total memory',
    `node_root_disk_size` bigint(20) unsigned DEFAULT 0 COMMENT 'root disk size of root disk for each node',
    `endpoint` varchar(255) COMMENT 'url endpoint of the kubernetes cluster manager api access',
    `console_endpoint` varchar(255) COMMENT 'url for the kubernetes cluster manager dashbaord',
    `created` datetime NOT NULL COMMENT 'date created',
    `removed` datetime COMMENT 'date removed if not null',
    `gc` tinyint unsigned NOT NULL DEFAULT 1 COMMENT 'gc this kubernetes cluster or not',

    PRIMARY KEY(`id`),
    CONSTRAINT `fk_cluster__zone_id` FOREIGN KEY `fk_cluster__zone_id`(`zone_id`) REFERENCES `data_center` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cluster__kubernetes_version_id` FOREIGN KEY `fk_cluster__kubernetes_version_id`(`kubernetes_version_id`) REFERENCES `kubernetes_supported_version` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cluster__service_offering_id` FOREIGN KEY `fk_cluster__service_offering_id`(`service_offering_id`) REFERENCES `service_offering`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cluster__template_id` FOREIGN KEY `fk_cluster__template_id`(`template_id`) REFERENCES `vm_template`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cluster__network_id` FOREIGN KEY `fk_cluster__network_id`(`network_id`) REFERENCES `networks`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`kubernetes_cluster_vm_map` (
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `cluster_id` bigint unsigned NOT NULL COMMENT 'cluster id',
    `vm_id` bigint unsigned NOT NULL COMMENT 'vm id',

    PRIMARY KEY(`id`),
    CONSTRAINT `fk_kubernetes_cluster_vm_map__cluster_id` FOREIGN KEY `fk_kubernetes_cluster_vm_map__cluster_id`(`cluster_id`) REFERENCES `kubernetes_cluster`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`kubernetes_cluster_details` (
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `cluster_id` bigint unsigned NOT NULL COMMENT 'kubernetes cluster id',
    `name` varchar(255) NOT NULL,
    `value` varchar(10240) NOT NULL,
    `display` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'True if the detail can be displayed to the end user',

    PRIMARY KEY(`id`),
    CONSTRAINT `fk_kubernetes_cluster_details__cluster_id` FOREIGN KEY `fk_kubernetes_cluster_details__cluster_id`(`cluster_id`) REFERENCES `kubernetes_cluster`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server',
'cloud.kubernetes.cluster.template.name', "Kubernetes-Service-Template", 'Name of the template to be used for creating Kubernetes cluster nodes', 'Kubernetes-Service-Template', NULL, NULL, 0);

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server',
'cloud.kubernetes.cluster.binaries.iso.name', 'Kubernetes-Service-Binaries-ISO' , 'Name of the ISO that contains Kubernetes binaries and docker images for offline installation.', 'Kubernetes-Service-Binaries-ISO', NULL , NULL, 0);

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server',
'cloud.kubernetes.cluster.master.cloudconfig', '/etc/cloudstack/management/k8s-master.yml' , 'file location path of the cloud config used for creating kubernetes cluster master node', '/etc/cloudstack/management/k8s-master.yml', NULL , NULL, 0);

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server',
'cloud.kubernetes.cluster.node.cloudconfig', '/etc/cloudstack/management/k8s-node.yml', 'file location path of the cloud config used for creating kubernetes cluster node', '/etc/cloudstack/management/k8s-node.yml', NULL , NULL, 0);

INSERT IGNORE INTO `cloud`.`network_offerings` (name, uuid, unique_name, display_text, nw_rate, mc_rate, traffic_type, tags, system_only, specify_vlan, service_offering_id, conserve_mode, created,availability, dedicated_lb_service, shared_source_nat_service, sort_key, redundant_router_service, state, guest_type, elastic_ip_service, eip_associate_public_ip, elastic_lb_service, specify_ip_ranges, inline,is_persistent,internal_lb, public_lb, egress_default_policy, concurrent_connections, keep_alive_enabled, supports_streched_l2, `default`, removed) VALUES ('DefaultNetworkOfferingforKubernetesService', UUID(), 'DefaultNetworkOfferingforKubernetesService', 'Network Offering used for CloudStack kubernetes service', NULL,NULL,'Guest',NULL,0,0,NULL,1,now(),'Required',1,0,0,0,'Enabled','Isolated',0,1,0,0,0,0,0,1,1,NULL,0,0,0,NULL);

UPDATE `cloud`.`network_offerings` SET removed=NULL WHERE unique_name='DefaultNetworkOfferingforKubernetesService';

SET @k8snetwork = (select id from network_offerings where name='DefaultNetworkOfferingforKubernetesService' and removed IS NULL);
INSERT IGNORE INTO ntwk_offering_service_map (network_offering_id, service, provider, created) VALUES (@k8snetwork, 'Dhcp','VirtualRouter',now());
INSERT IGNORE INTO ntwk_offering_service_map (network_offering_id, service, provider, created) VALUES (@k8snetwork, 'Dns','VirtualRouter',now());
INSERT IGNORE INTO ntwk_offering_service_map (network_offering_id, service, provider, created) VALUES (@k8snetwork, 'Firewall','VirtualRouter',now());
INSERT IGNORE INTO ntwk_offering_service_map (network_offering_id, service, provider, created) VALUES (@k8snetwork, 'Gateway','VirtualRouter',now());
INSERT IGNORE INTO ntwk_offering_service_map (network_offering_id, service, provider, created) VALUES (@k8snetwork, 'Lb','VirtualRouter',now());
INSERT IGNORE INTO ntwk_offering_service_map (network_offering_id, service, provider, created) VALUES (@k8snetwork, 'PortForwarding','VirtualRouter',now());
INSERT IGNORE INTO ntwk_offering_service_map (network_offering_id, service, provider, created) VALUES (@k8snetwork, 'SourceNat','VirtualRouter',now());
INSERT IGNORE INTO ntwk_offering_service_map (network_offering_id, service, provider, created) VALUES (@k8snetwork, 'StaticNat','VirtualRouter',now());
INSERT IGNORE INTO ntwk_offering_service_map (network_offering_id, service, provider, created) VALUES (@k8snetwork, 'UserData','VirtualRouter',now());
INSERT IGNORE INTO ntwk_offering_service_map (network_offering_id, service, provider, created) VALUES (@k8snetwork, 'Vpn','VirtualRouter',now());

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server',
'cloud.kubernetes.cluster.network.offering', 'DefaultNetworkOfferingforKubernetesService' , 'Network Offering used for CloudStack kubernetes service', 'DefaultNetworkOfferingforKubernetesService', NULL , NULL, 0);
