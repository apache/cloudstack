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
-- Schema upgrade from 4.10.0.0 to 4.11.0.0;
--;

use cloud;

CREATE TABLE IF NOT EXISTS `cloud`.`application_cluster` (
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `uuid` varchar(40),
    `name` varchar(255) NOT NULL,
    `description` varchar(4096) COMMENT 'display text for this container cluster',
    `zone_id` bigint unsigned NOT NULL COMMENT 'zone id',
    `service_offering_id` bigint unsigned COMMENT 'service offering id for the cluster VM',
    `template_id` bigint unsigned COMMENT 'vm_template.id',
    `network_id` bigint unsigned COMMENT 'network this container cluster uses',
    `node_count` bigint NOT NULL default '0',
    `account_id` bigint unsigned NOT NULL COMMENT 'owner of this cluster',
    `domain_id` bigint unsigned NOT NULL COMMENT 'owner of this cluster',
    `state` char(32) NOT NULL COMMENT 'current state of this cluster',
    `key_pair` varchar(40),
    `cores` bigint unsigned NOT NULL COMMENT 'number of cores',
    `memory` bigint unsigned NOT NULL COMMENT 'total memory',
    `endpoint` varchar(255) COMMENT 'url endpoint of the container cluster manager api access',
    `console_endpoint` varchar(255) COMMENT 'url for the container cluster manager dashbaord',
    `created` datetime NOT NULL COMMENT 'date created',
    `removed` datetime COMMENT 'date removed if not null',
    `gc` tinyint unsigned NOT NULL DEFAULT 1 COMMENT 'gc this container cluster or not',

    CONSTRAINT `fk_cluster__zone_id` FOREIGN KEY `fk_cluster__zone_id` (`zone_id`) REFERENCES `data_center` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cluster__service_offering_id` FOREIGN KEY `fk_cluster__service_offering_id` (`service_offering_id`) REFERENCES `service_offering`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cluster__template_id` FOREIGN KEY `fk_cluster__template_id`(`template_id`) REFERENCES `vm_template`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cluster__network_id` FOREIGN KEY `fk_cluster__network_id`(`network_id`) REFERENCES `networks`(`id`) ON DELETE CASCADE,

    PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`application_cluster_vm_map` (
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `cluster_id` bigint unsigned NOT NULL COMMENT 'cluster id',
    `vm_id` bigint unsigned NOT NULL COMMENT 'vm id',

    PRIMARY KEY(`id`),
    CONSTRAINT `application_cluster_vm_map_cluster__id` FOREIGN KEY `application_cluster_vm_map_cluster__id`(`cluster_id`) REFERENCES `application_cluster`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`application_cluster_details` (
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `cluster_id` bigint unsigned NOT NULL COMMENT 'cluster id',
    `username` varchar(255) NOT NULL,
    `password` varchar(255) NOT NULL,
    `registry_username` varchar(255),
    `registry_password` varchar(255),
    `registry_url` varchar(255),
    `registry_email` varchar(255),
    `network_cleanup` tinyint unsigned NOT NULL DEFAULT 1 COMMENT 'true if network needs to be clean up on deletion of container cluster. Should be false if user specfied network for the cluster',

    PRIMARY KEY(`id`),
    CONSTRAINT `application_cluster_details_cluster__id` FOREIGN KEY `application_cluster_details_cluster__id`(`cluster_id`) REFERENCES `application_cluster`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server',
'cloud.application.cluster.template.name', "ShapeBlue-ACS-Template", 'template name', '-1', NULL, NULL, 0);

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server',
'cloud.application.cluster.master.cloudconfig', '/etc/cloudstack/management/k8s-master.yml' , 'file location path of the cloud config used for creating container cluster master node', '/etc/cloudstack/management/k8s-master.yml', NULL , NULL, 0);

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server',
'cloud.application.cluster.node.cloudconfig', '/etc/cloudstack/management/k8s-node.yml', 'file location path of the cloud config used for creating container cluster node', '/etc/cloudstack/management/k8s-node.yml', NULL , NULL, 0);

INSERT IGNORE INTO `cloud`.`network_offerings` (name, uuid, unique_name, display_text, nw_rate, mc_rate, traffic_type, tags, system_only, specify_vlan, service_offering_id, conserve_mode, created,availability, dedicated_lb_service, shared_source_nat_service, sort_key, redundant_router_service, state, guest_type, elastic_ip_service, eip_associate_public_ip, elastic_lb_service, specify_ip_ranges, inline,is_persistent,internal_lb, public_lb, egress_default_policy, concurrent_connections, keep_alive_enabled, supports_streched_l2, `default`, removed) VALUES ('DefaultNetworkOfferingforClusterService', UUID(), 'DefaultNetworkOfferingforClusterService', 'Network Offering used for CloudStack container service', NULL,NULL,'Guest',NULL,0,0,NULL,1,now(),'Required',1,0,0,0,'Enabled','Isolated',0,1,0,0,0,0,0,1,1,NULL,0,0,0,NULL);

UPDATE `cloud`.`network_offerings` SET removed=NULL WHERE unique_name='DefaultNetworkOfferingforClusterService';

SET @ccsntwk = (select id from network_offerings where name='DefaultNetworkOfferingforClusterService' and removed IS NULL);
INSERT IGNORE INTO ntwk_offering_service_map (network_offering_id, service, provider, created) VALUES (@ccsntwk, 'Dhcp','VirtualRouter',now());
INSERT IGNORE INTO ntwk_offering_service_map (network_offering_id, service, provider, created) VALUES (@ccsntwk, 'Dns','VirtualRouter',now());
INSERT IGNORE INTO ntwk_offering_service_map (network_offering_id, service, provider, created) VALUES (@ccsntwk, 'Firewall','VirtualRouter',now());
INSERT IGNORE INTO ntwk_offering_service_map (network_offering_id, service, provider, created) VALUES (@ccsntwk, 'Gateway','VirtualRouter',now());
INSERT IGNORE INTO ntwk_offering_service_map (network_offering_id, service, provider, created) VALUES (@ccsntwk, 'Lb','VirtualRouter',now());
INSERT IGNORE INTO ntwk_offering_service_map (network_offering_id, service, provider, created) VALUES (@ccsntwk, 'PortForwarding','VirtualRouter',now());
INSERT IGNORE INTO ntwk_offering_service_map (network_offering_id, service, provider, created) VALUES (@ccsntwk, 'SourceNat','VirtualRouter',now());
INSERT IGNORE INTO ntwk_offering_service_map (network_offering_id, service, provider, created) VALUES (@ccsntwk, 'StaticNat','VirtualRouter',now());
INSERT IGNORE INTO ntwk_offering_service_map (network_offering_id, service, provider, created) VALUES (@ccsntwk, 'UserData','VirtualRouter',now());
INSERT IGNORE INTO ntwk_offering_service_map (network_offering_id, service, provider, created) VALUES (@ccsntwk, 'Vpn','VirtualRouter',now());

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server',
'cloud.application.cluster.network.offering', 'DefaultNetworkOfferingforClusterService' , 'Network Offering used for CloudStack container service', 'DefaultNetworkOfferingforClusterService', NULL , NULL, 0);

INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (255, UUID(), 7, 'CoreOS', utc_timestamp());
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'XenServer', 'default', 'CoreOS', 255, utc_timestamp(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VmWare', 'default', 'coreos64Guest', 255, utc_timestamp(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'KVM', 'default', 'CoreOS', 255, utc_timestamp(), 0);

INSERT INTO `cloud`.`vm_template` (uuid, unique_name, name, public, featured, created, state, type, hvm, bits, account_id, url, enable_password, display_text, format, guest_os_id, cross_zones, hypervisor_type, extractable)  VALUES (UUID(), 'ACS Template KVM', 'ACS Template KVM', 1, 1, now(), 'Active', 'BUILTIN', 0, 64, 1, 'http:--dl.openvm.eu/cloudstack/coreos/x86_64/coreos_production_cloudstack_image-kvm.qcow2.bz2',  0, 'Cloudstack Application Cluster Service Template (KVM)', 'QCOW2', 99, 1, 'KVM',1);
