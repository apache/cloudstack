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
-- Schema upgrade from 4.5.2 to 4.5.3;
--;

-- Dynamic roles
CREATE TABLE IF NOT EXISTS `cloud`.`roles` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(255) UNIQUE,
  `name` varchar(255) COMMENT 'unique name of the dynamic role',
  `role_type` varchar(255) NOT NULL COMMENT 'the type of the role',
  `removed` datetime COMMENT 'date removed',
  `description` text COMMENT 'description of the role',
  PRIMARY KEY (`id`),
  KEY `i_roles__name` (`name`),
  KEY `i_roles__role_type` (`role_type`),
  UNIQUE KEY (`name`, `role_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`role_permissions` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(255) UNIQUE,
  `role_id` bigint(20) unsigned NOT NULL COMMENT 'id of the role',
  `rule` varchar(255) NOT NULL COMMENT 'rule for the role, api name or wildcard',
  `permission` varchar(255) NOT NULL COMMENT 'access authority, allow or deny',
  `description` text COMMENT 'description of the rule',
  `sort_order` bigint(20) unsigned NOT NULL DEFAULT 0 COMMENT 'permission sort order',
  PRIMARY KEY (`id`),
  KEY `fk_role_permissions__role_id` (`role_id`),
  KEY `i_role_permissions__sort_order` (`sort_order`),
  UNIQUE KEY (`role_id`, `rule`),
  CONSTRAINT `fk_role_permissions__role_id` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Default CloudStack roles
INSERT INTO `cloud`.`roles` (`id`, `uuid`, `name`, `role_type`, `description`) values (1, UUID(), 'Root Admin', 'Admin', 'Default root admin role') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO `cloud`.`roles` (`id`, `uuid`, `name`, `role_type`, `description`) values (2, UUID(), 'Resource Admin', 'ResourceAdmin', 'Default resource admin role') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO `cloud`.`roles` (`id`, `uuid`, `name`, `role_type`, `description`) values (3, UUID(), 'Domain Admin', 'DomainAdmin', 'Default domain admin role') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO `cloud`.`roles` (`id`, `uuid`, `name`, `role_type`, `description`) values (4, UUID(), 'User', 'User', 'Default Root Admin role') ON DUPLICATE KEY UPDATE name=name;

CREATE TABLE IF NOT EXISTS `cloud`.`oobm` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `host_id` bigint(20) unsigned DEFAULT NULL COMMENT 'foreign key to host',
  `enabled` int(1) unsigned DEFAULT '0' COMMENT 'is out-of-band management enabled for host',
  `power_state` varchar(32) DEFAULT 'Disabled' COMMENT 'out-of-band management power status',
  `driver` varchar(32) DEFAULT NULL COMMENT 'out-of-band management driver',
  `address` varchar(255) DEFAULT NULL COMMENT 'out-of-band management interface address',
  `port` int(10) unsigned DEFAULT NULL COMMENT 'out-of-band management interface port',
  `username` varchar(255) DEFAULT NULL COMMENT 'out-of-band management interface username',
  `password` varchar(255) DEFAULT NULL COMMENT 'out-of-band management interface password',
  `update_count` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT 'atomic increase count making status update operation atomical',
  `update_time` datetime COMMENT 'last power state update datetime',
  `mgmt_server_id` bigint(20) unsigned DEFAULT NULL COMMENT 'management server id which owns out-of-band management for the host',
  PRIMARY KEY (`id`),
  KEY `fk_oobm__host_id` (`host_id`),
  KEY `i_oobm__enabled` (`enabled`),
  KEY `i_oobm__power_state` (`power_state`),
  KEY `i_oobm__update_time` (`update_time`),
  KEY `i_oobm__mgmt_server_id` (`mgmt_server_id`),
  CONSTRAINT `fk_oobm__host_id` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP VIEW IF EXISTS `cloud`.`host_view`;
CREATE VIEW `cloud`.`host_view` AS
    select
        host.id,
        host.uuid,
        host.name,
        host.status,
        host.disconnected,
        host.type,
        host.private_ip_address,
        host.version,
        host.hypervisor_type,
        host.hypervisor_version,
        host.capabilities,
        host.last_ping,
        host.created,
        host.removed,
        host.resource_state,
        host.mgmt_server_id,
        host.cpu_sockets,
        host.cpus,
        host.speed,
        host.ram,
        cluster.id cluster_id,
        cluster.uuid cluster_uuid,
        cluster.name cluster_name,
        cluster.cluster_type,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        data_center.networktype data_center_type,
        host_pod_ref.id pod_id,
        host_pod_ref.uuid pod_uuid,
        host_pod_ref.name pod_name,
        host_tags.tag,
        guest_os_category.id guest_os_category_id,
        guest_os_category.uuid guest_os_category_uuid,
        guest_os_category.name guest_os_category_name,
        mem_caps.used_capacity memory_used_capacity,
        mem_caps.reserved_capacity memory_reserved_capacity,
        cpu_caps.used_capacity cpu_used_capacity,
        cpu_caps.reserved_capacity cpu_reserved_capacity,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id,
        oobm.enabled AS `oobm_enabled`,
        oobm.power_state AS `oobm_power_state`
    from
        `cloud`.`host`
            left join
        `cloud`.`cluster` ON host.cluster_id = cluster.id
            left join
        `cloud`.`data_center` ON host.data_center_id = data_center.id
            left join
        `cloud`.`host_pod_ref` ON host.pod_id = host_pod_ref.id
            left join
        `cloud`.`host_details` ON host.id = host_details.host_id
            and host_details.name = 'guest.os.category.id'
            left join
        `cloud`.`guest_os_category` ON guest_os_category.id = CONVERT( host_details.value , UNSIGNED)
            left join
        `cloud`.`host_tags` ON host_tags.host_id = host.id
            left join
        `cloud`.`op_host_capacity` mem_caps ON host.id = mem_caps.host_id
            and mem_caps.capacity_type = 0
            left join
        `cloud`.`op_host_capacity` cpu_caps ON host.id = cpu_caps.host_id
            and cpu_caps.capacity_type = 1
            left join
        `cloud`.`async_job` ON async_job.instance_id = host.id
            and async_job.instance_type = 'Host'
            and async_job.job_status = 0
            left join
        `cloud`.`oobm` ON oobm.host_id = host.id;
