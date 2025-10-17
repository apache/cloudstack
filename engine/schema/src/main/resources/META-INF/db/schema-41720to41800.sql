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
-- Schema upgrade from 4.17.2.0 to 4.18.0.0
--;

-- Add el9 guest OS mappings
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'AlmaLinux 9', 'KVM', 'default', 'AlmaLinux 9');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'CentOS 9', 'KVM', 'default', 'CentOS 9');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Oracle Linux 9', 'KVM', 'default', 'Oracle Linux 9');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Red Hat Enterprise Linux 9', 'KVM', 'default', 'Red Hat Enterprise Linux 9');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Rocky Linux 9', 'KVM', 'default', 'Rocky Linux 9');

CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'AlmaLinux 9', 'VMware', '7.0', 'otherLinux64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Oracle Linux 9', 'VMware', '7.0', 'otherLinux64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Red Hat Enterprise Linux 9', 'VMware', '7.0', 'rhel9_64Guest,');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Rocky Linux 9', 'VMware', '7.0', 'otherLinux64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'AlmaLinux 9', 'VMware', '7.0.1.0', 'otherLinux64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Oracle Linux 9', 'VMware', '7.0.1.0', 'otherLinux64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Red Hat Enterprise Linux 9', 'VMware', '7.0.1.0', 'rhel9_64Guest,');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Rocky Linux 9', 'VMware', '7.0.1.0', 'otherLinux64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'AlmaLinux 9', 'VMware', '7.0.2.0', 'otherLinux64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Oracle Linux 9', 'VMware', '7.0.2.0', 'otherLinux64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Red Hat Enterprise Linux 9', 'VMware', '7.0.2.0', 'rhel9_64Guest,');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Rocky Linux 9', 'VMware', '7.0.2.0', 'otherLinux64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'AlmaLinux 9', 'VMware', '7.0.3.0', 'otherLinux64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Oracle Linux 9', 'VMware', '7.0.3.0', 'otherLinux64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Red Hat Enterprise Linux 9', 'VMware', '7.0.3.0', 'rhel9_64Guest,');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Rocky Linux 9', 'VMware', '7.0.3.0', 'otherLinux64Guest');

-- Add support for VMware 8.0 and 8.0a (8.0.0.1)
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities` (uuid, hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported, vm_snapshot_enabled) values (UUID(), 'VMware', '8.0', 1024, 0, 59, 64, 1, 1);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities` (uuid, hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported, vm_snapshot_enabled) values (UUID(), 'VMware', '8.0.0.1', 1024, 0, 59, 64, 1, 1);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'VMware', '8.0', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='VMware' AND hypervisor_version='7.0.3.0';
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'VMware', '8.0.0.1', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='VMware' AND hypervisor_version='7.0.3.0';

-- Enable CPU cap for default system offerings;
UPDATE `cloud`.`service_offering` so
SET so.limit_cpu_use = 1
WHERE so.default_use = 1 AND so.vm_type IN ('domainrouter', 'secondarystoragevm', 'consoleproxy', 'internalloadbalancervm', 'elasticloadbalancervm');

ALTER TABLE `cloud`.`networks` ADD COLUMN `public_mtu` bigint unsigned comment "MTU for VR public interface" ;
ALTER TABLE `cloud`.`networks` ADD COLUMN `private_mtu` bigint unsigned comment "MTU for VR private interfaces" ;
ALTER TABLE `cloud`.`vpc` ADD COLUMN `public_mtu` bigint unsigned comment "MTU for VPC VR public interface" ;
ALTER TABLE `cloud`.`nics` ADD COLUMN `mtu` bigint unsigned comment "MTU for the VR interface" ;

UPDATE `cloud`.`networks` SET public_mtu = 1500, private_mtu = 1500 WHERE removed IS NULL AND guest_type='Isolated';
UPDATE `cloud`.`vpc` SET public_mtu = 1500 WHERE removed IS NULL;
UPDATE `cloud`.`nics` SET mtu = 1500 WHERE vm_type='DomainRouter' AND removed IS NULL AND reserver_name IN ('PublicNetworkGuru', 'ExternalGuestNetworkGuru');

-- Add type column to data_center table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.data_center', 'type', 'varchar(32) DEFAULT ''Core'' COMMENT ''the type of the zone'' ');

-- Add passphrase table
CREATE TABLE IF NOT EXISTS `cloud`.`passphrase` (
    `id` bigint unsigned NOT NULL auto_increment,
    `passphrase` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Add passphrase column to volumes table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.volumes', 'passphrase_id', 'bigint unsigned DEFAULT NULL COMMENT "encryption passphrase id" ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.volumes', 'encrypt_format', 'varchar(64) DEFAULT NULL COMMENT "encryption format" ');

-- Add encrypt column to disk_offering
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.disk_offering', 'encrypt', 'tinyint(1) DEFAULT 0 COMMENT "volume encrypt requested" ');

-- Add cidr_list column to load_balancing_rules
ALTER TABLE `cloud`.`load_balancing_rules`
ADD cidr_list VARCHAR(4096);

-- safely add resources in parallel
-- PR#5984 Create table to persist VM stats.
DROP TABLE IF EXISTS `cloud`.`resource_reservation`;
CREATE TABLE `cloud`.`resource_reservation` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `account_id` bigint unsigned NOT NULL,
  `domain_id` bigint unsigned NOT NULL,
  `resource_type` varchar(255) NOT NULL,
  `amount` bigint unsigned NOT NULL,
  PRIMARY KEY (`id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Alter networks table to add ip6dns1 and ip6dns2
ALTER TABLE `cloud`.`networks`
    ADD COLUMN `ip6dns1` varchar(255) DEFAULT NULL COMMENT 'first IPv6 DNS for the network' AFTER `dns2`,
    ADD COLUMN `ip6dns2` varchar(255) DEFAULT NULL COMMENT 'second IPv6 DNS for the network' AFTER `ip6dns1`;
-- Alter vpc table to add dns1, dns2, ip6dns1 and ip6dns2
ALTER TABLE `cloud`.`vpc`
    ADD COLUMN `dns1` varchar(255) DEFAULT NULL COMMENT 'first IPv4 DNS for the vpc' AFTER `network_domain`,
    ADD COLUMN `dns2` varchar(255) DEFAULT NULL COMMENT 'second IPv4 DNS for the vpc' AFTER `dns1`,
    ADD COLUMN `ip6dns1` varchar(255) DEFAULT NULL COMMENT 'first IPv6 DNS for the vpc' AFTER `dns2`,
    ADD COLUMN `ip6dns2` varchar(255) DEFAULT NULL COMMENT 'second IPv6 DNS for the vpc' AFTER `ip6dns1`;

-- Fix migrateVolume permissions #6224.
DELETE role_perm
FROM role_permissions role_perm
INNER JOIN roles ON role_perm.role_id = roles.id
WHERE roles.role_type != 'Admin' AND roles.is_default = 1 AND role_perm.rule = 'migrateVolume';

-- VM autoscaling


-- Add column 'supports_vm_autoscaling' to 'network_offerings' table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.network_offerings', 'supports_vm_autoscaling', 'boolean default false');

-- Add column 'name' to 'autoscale_vmgroups' table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.autoscale_vmgroups', 'name', 'VARCHAR(255) DEFAULT NULL COMMENT "name of the autoscale vm group" AFTER `load_balancer_id`');
UPDATE `cloud`.`autoscale_vmgroups` SET `name` = CONCAT('AutoScale-VmGroup-',id) WHERE `name` IS NULL;

-- Add column 'next_vm_seq' to 'autoscale_vmgroups' table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.autoscale_vmgroups', 'next_vm_seq', 'BIGINT UNSIGNED NOT NULL DEFAULT 1');

-- Add column 'user_data' to 'autoscale_vmprofiles' table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.autoscale_vmprofiles', 'user_data', 'TEXT(32768) AFTER `counter_params`');

-- Add column 'name' to 'autoscale_policies' table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.autoscale_policies', 'name', 'VARCHAR(255) DEFAULT NULL COMMENT "name of the autoscale policy" AFTER `uuid`');

-- Add column 'provider' and update values
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.counter', 'provider', 'VARCHAR(255) NOT NULL COMMENT "Network provider name" AFTER `uuid`');
UPDATE `cloud`.`counter` SET provider = 'Netscaler' WHERE `provider` IS NULL OR `provider` = '';

CALL `cloud`.`IDEMPOTENT_ADD_UNIQUE_KEY`('cloud.counter', 'uc_counter__provider__source__value', '(provider, source, value)');

-- Add new counters for VM autoscaling

INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VirtualRouter', 'cpu', 'VM CPU - average percentage', 'vm.cpu.average.percentage', NOW());
INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VirtualRouter', 'memory', 'VM Memory - average percentage', 'vm.memory.average.percentage', NOW());
INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VirtualRouter', 'virtualrouter', 'Public Network - mbps received per vm', 'public.network.received.average.mbps', NOW());
INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VirtualRouter', 'virtualrouter', 'Public Network - mbps transmit per vm', 'public.network.transmit.average.mbps', NOW());
INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VirtualRouter', 'virtualrouter', 'Load Balancer - average connections per vm', 'virtual.network.lb.average.connections', NOW());

INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VpcVirtualRouter', 'cpu', 'VM CPU - average percentage', 'vm.cpu.average.percentage', NOW());
INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VpcVirtualRouter', 'memory', 'VM Memory - average percentage', 'vm.memory.average.percentage', NOW());
INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VpcVirtualRouter', 'virtualrouter', 'Public Network - mbps received per vm', 'public.network.received.average.mbps', NOW());
INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VpcVirtualRouter', 'virtualrouter', 'Public Network - mbps transmit per vm', 'public.network.transmit.average.mbps', NOW());
INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VpcVirtualRouter', 'virtualrouter', 'Load Balancer - average connections per vm', 'virtual.network.lb.average.connections', NOW());

INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'None', 'cpu', 'VM CPU - average percentage', 'vm.cpu.average.percentage', NOW());
INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'None', 'memory', 'VM Memory - average percentage', 'vm.memory.average.percentage', NOW());

-- Update autoscale_vmgroups to new state

UPDATE `cloud`.`autoscale_vmgroups` SET state= UPPER(state);

-- Update autoscale_vmgroups so records will not be removed when LB rule is removed

CALL `cloud`.`IDEMPOTENT_DROP_FOREIGN_KEY`('cloud.autoscale_vmgroups', 'fk_autoscale_vmgroup__load_balancer_id');

-- Update autoscale_vmprofiles to make autoscale_user_id optional

ALTER TABLE `cloud`.`autoscale_vmprofiles` MODIFY COLUMN `autoscale_user_id` bigint unsigned;

-- Update autoscale_vmprofiles to rename destroy_vm_grace_period

CALL `cloud`.`IDEMPOTENT_CHANGE_COLUMN`('cloud.autoscale_vmprofiles', 'destroy_vm_grace_period', 'expunge_vm_grace_period', 'int unsigned COMMENT "the time allowed for existing connections to get closed before a vm is expunged"');

-- Create table for VM autoscaling historic data

CREATE TABLE IF NOT EXISTS `cloud`.`autoscale_vmgroup_statistics` (
  `id` bigint unsigned NOT NULL auto_increment,
  `vmgroup_id` bigint unsigned NOT NULL,
  `policy_id` bigint unsigned NOT NULL,
  `counter_id` bigint unsigned NOT NULL,
  `resource_id` bigint unsigned DEFAULT NULL,
  `resource_type` varchar(255) NOT NULL,
  `raw_value` double NOT NULL,
  `value_type` varchar(255) NOT NULL,
  `created` datetime NOT NULL COMMENT 'Date this data is created',
  `state` varchar(255) NOT NULL COMMENT 'State of the data',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_autoscale_vmgroup_statistics__vmgroup_id` FOREIGN KEY `fk_autoscale_vmgroup_statistics__vmgroup_id` (`vmgroup_id`) REFERENCES `autoscale_vmgroups` (`id`) ON DELETE CASCADE,
  INDEX `i_autoscale_vmgroup_statistics__vmgroup_id`(`vmgroup_id`),
  INDEX `i_autoscale_vmgroup_statistics__policy_id`(`policy_id`),
  INDEX `i_autoscale_vmgroup_statistics__counter_id`(`counter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Update column 'supports_vm_autoscaling' to 1 if network offerings support Lb
UPDATE `cloud`.`network_offerings`
JOIN `cloud`.`ntwk_offering_service_map`
ON network_offerings.id = ntwk_offering_service_map.network_offering_id
JOIN (
    SELECT COUNT(id) AS count FROM `cloud`.`network_offerings` WHERE supports_vm_autoscaling = 1
) AS cnt
SET network_offerings.supports_vm_autoscaling = 1
WHERE ntwk_offering_service_map.service = 'Lb'
    AND ntwk_offering_service_map.provider IN ('VirtualRouter', 'VpcVirtualRouter', 'Netscaler')
    AND network_offerings.removed IS NULL
    AND cnt.count = 0;

-- UserData as first class resource (PR #6202)
CREATE TABLE IF NOT EXISTS `cloud`.`user_data` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40) NOT NULL COMMENT 'UUID of the user data',
  `name` varchar(256) NOT NULL COMMENT 'name of the user data',
  `account_id` bigint unsigned NOT NULL COMMENT 'owner, foreign key to account table',
  `domain_id` bigint unsigned NOT NULL COMMENT 'domain, foreign key to domain table',
  `user_data` mediumtext COMMENT 'value of the userdata',
  `params` mediumtext COMMENT 'value of the comma-separated list of parameters',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_userdata__account_id` FOREIGN KEY(`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_userdata__domain_id` FOREIGN KEY(`domain_id`) REFERENCES `domain` (`id`) ON DELETE CASCADE,
  CONSTRAINT `uc_userdata__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.user_vm', 'user_data_id', 'bigint unsigned DEFAULT NULL COMMENT "id of the user data" AFTER `user_data`');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.user_vm', 'user_data_details', 'mediumtext DEFAULT NULL COMMENT "value of the comma-separated list of parameters" AFTER `user_data_id`');

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vm_template', 'user_data_id', 'bigint unsigned DEFAULT NULL COMMENT "id of the user data"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vm_template', 'user_data_link_policy', 'varchar(255) DEFAULT NULL COMMENT "user data link policy with template"');

-- Improve alert.email.addresses description #6806.
UPDATE  cloud.configuration
SET     description = 'Comma separated list of email addresses which are going to receive alert emails.'
WHERE   name = 'alert.email.addresses';

-- Improve description of configuration `secstorage.encrypt.copy` #6811.
UPDATE  cloud.configuration
SET     description = "Use SSL method used to encrypt copy traffic between zones. Also ensures that the certificate assigned to the zone is used when
generating links for external access."
WHERE   name = 'secstorage.encrypt.copy';

-- Create table to persist volume stats.
DROP TABLE IF EXISTS `cloud`.`volume_stats`;
CREATE TABLE `cloud`.`volume_stats` (
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `volume_id` bigint unsigned NOT NULL,
    `mgmt_server_id` bigint unsigned NOT NULL,
    `timestamp` datetime NOT NULL,
    `volume_stats_data` text NOT NULL,
    PRIMARY KEY(`id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- allow isolated networks without services to be used as is.
UPDATE `cloud`.`networks` ntwk
  SET ntwk.state = 'Implemented'
  WHERE ntwk.network_offering_id in
    (SELECT id FROM `cloud`.`network_offerings` ntwkoff
      WHERE (SELECT count(*) FROM `cloud`.`ntwk_offering_service_map` ntwksrvcmp WHERE ntwksrvcmp.network_offering_id = ntwkoff.id) = 0
        AND ntwkoff.is_persistent = 1) AND
    ntwk.state = 'Setup' AND
    ntwk.removed is NULL AND
    ntwk.guest_type = 'Isolated';

----- PR Quota custom tariffs #5909---
-- Create column 'uuid'
ALTER TABLE cloud_usage.quota_tariff
    ADD COLUMN  `uuid` varchar(40);

UPDATE  cloud_usage.quota_tariff
SET     uuid = UUID()
WHERE   uuid is null;

ALTER TABLE cloud_usage.quota_tariff
    MODIFY      `uuid` varchar(40) NOT NULL;


-- Create column 'name'
ALTER TABLE cloud_usage.quota_tariff
    ADD COLUMN  `name` text
    COMMENT     'A name, deﬁned by the user, to the tariff. This column will be used as identiﬁer along the tariff updates.';

UPDATE  cloud_usage.quota_tariff
SET     name = case when effective_on <= now() then usage_name else concat(usage_name, '-', id) end
WHERE   name is null;

ALTER TABLE cloud_usage.quota_tariff
    MODIFY      `name` text NOT NULL;


-- Create column 'description'
ALTER TABLE cloud_usage.quota_tariff
    ADD COLUMN  `description` text DEFAULT NULL
    COMMENT     'The description of the tariff.';


-- Create column 'activation_rule'
ALTER TABLE cloud_usage.quota_tariff
    ADD COLUMN  `activation_rule` text DEFAULT NULL
    COMMENT     'JS expression that defines when the tariff should be activated.';


-- Create column 'removed'
ALTER TABLE cloud_usage.quota_tariff
    ADD COLUMN  `removed` datetime DEFAULT NULL;


-- Create column 'end_date'
ALTER TABLE cloud_usage.quota_tariff
    ADD COLUMN  `end_date` datetime DEFAULT NULL
    COMMENT     'Defines the end date of the tariff.';


-- Change usage unit to right unit
UPDATE  cloud_usage.quota_tariff
SET     usage_unit = 'Compute*Month'
WHERE   usage_unit = 'Compute-Month';

UPDATE  cloud_usage.quota_tariff
SET     usage_unit = 'IP*Month'
WHERE   usage_unit = 'IP-Month';

UPDATE  cloud_usage.quota_tariff
SET     usage_unit = 'GB*Month'
WHERE   usage_unit = 'GB-Month';

UPDATE  cloud_usage.quota_tariff
SET     usage_unit = 'Policy*Month'
WHERE   usage_unit = 'Policy-Month';

----- PR Quota custom tariffs #5909 -----

-- delete configuration task.cleanup.retry.interval #6910
DELETE FROM `cloud`.`configuration` WHERE name='task.cleanup.retry.interval';

-- Tungsten Fabric Plugin --
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.network_offerings','for_tungsten', 'int(1) unsigned DEFAULT "0" COMMENT "is tungsten enabled for the resource"');

CREATE TABLE IF NOT EXISTS `cloud`.`tungsten_providers` (
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `zone_id` bigint unsigned NOT NULL COMMENT 'Zone ID',
    `uuid` varchar(40),
    `host_id` bigint unsigned NOT NULL,
    `provider_name` varchar(40),
    `port` varchar(40),
    `hostname` varchar(40),
    `gateway` varchar(40),
    `vrouter_port` varchar(40),
    `introspect_port` varchar(40),
    PRIMARY KEY  (`id`),
    CONSTRAINT `fk_tungsten_providers__host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_tungsten_providers__zone_id` FOREIGN KEY (`zone_id`) REFERENCES `data_center`(`id`) ON DELETE CASCADE,
    CONSTRAINT `uc_tungsten_providers__uuid` UNIQUE (`uuid`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`tungsten_guest_network_ip_address` (
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `network_id` bigint unsigned NOT NULL COMMENT 'network id',
    `public_ip_address` varchar(15) COMMENT 'ip public_ip_address',
    `guest_ip_address` varchar(15) NOT NULL COMMENT 'ip guest_ip_address',
    `logical_router_uuid` varchar(40) COMMENT 'logical router uuid',
    PRIMARY KEY  (`id`),
    CONSTRAINT `fk_tungsten_guest_network_ip_address__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`tungsten_security_group_rule` (
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `uuid` varchar(40) NOT NULL COMMENT 'rule uuid',
    `zone_id` bigint unsigned NOT NULL COMMENT 'Zone ID',
    `security_group_id` bigint unsigned NOT NULL COMMENT 'security group id',
    `rule_type` varchar(40) NOT NULL COMMENT 'rule type',
    `rule_target` varchar(40) NOT NULL COMMENT 'rule target',
    `ether_type` varchar(40) NOT NULL COMMENT 'ether type',
    `default_rule` int(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 if security group is default',
    PRIMARY KEY  (`id`),
    CONSTRAINT `fk_tungsten_security_group_rule__security_group_id` FOREIGN KEY (`security_group_id`) REFERENCES `security_group`(`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`tungsten_lb_health_monitor` (
    `id` bigint unsigned NOT NULL auto_increment,
    `uuid` varchar(40),
    `load_balancer_id` bigint unsigned NOT NULL,
    `type` varchar(40) NOT NULL,
    `retry` bigint unsigned NOT NULL,
    `timeout` bigint unsigned NOT NULL,
    `interval` bigint unsigned NOT NULL,
    `http_method` varchar(40),
    `expected_code` varchar(40),
    `url_path` varchar(255),
    PRIMARY KEY  (`id`),
    CONSTRAINT `fk_tungsten_lb_health_monitor__load_balancer_id` FOREIGN KEY(`load_balancer_id`) REFERENCES `load_balancing_rules`(`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--- #6888 add index to speed up querying IPs in the network-tab

CALL `cloud`.`IDEMPOTENT_ADD_KEY`('i_user_ip_address_state','user_ip_address', '(state)');

UPDATE  `cloud`.`role_permissions`
SET     sort_order = sort_order + 2
WHERE   rule = '*'
AND     permission = 'DENY'
AND     role_id in (SELECT id FROM `cloud`.`roles` WHERE name = 'Read-Only Admin - Default');

INSERT  INTO `cloud`.`role_permissions` (uuid, role_id, rule, permission, sort_order)
SELECT  UUID(), role_id, 'quotaStatement', 'ALLOW', MAX(sort_order)-1
FROM    `cloud`.`role_permissions` RP
WHERE   role_id = (SELECT id FROM `cloud`.`roles` WHERE name = 'Read-Only Admin - Default');

INSERT  INTO `cloud`.`role_permissions` (uuid, role_id, rule, permission, sort_order)
SELECT  UUID(), role_id, 'quotaBalance', 'ALLOW', MAX(sort_order)-2
FROM    `cloud`.`role_permissions` RP
WHERE   role_id = (SELECT id FROM `cloud`.`roles` WHERE name = 'Read-Only Admin - Default');

UPDATE  `cloud`.`role_permissions`
SET     sort_order = sort_order + 2
WHERE   rule = '*'
AND     permission = 'DENY'
AND     role_id in (SELECT id FROM `cloud`.`roles` WHERE name = 'Read-Only User - Default');

INSERT  INTO `cloud`.`role_permissions` (uuid, role_id, rule, permission, sort_order)
SELECT  UUID(), role_id, 'quotaStatement', 'ALLOW', MAX(sort_order)-1
FROM    `cloud`.`role_permissions` RP
WHERE   role_id = (SELECT id FROM `cloud`.`roles` WHERE name = 'Read-Only User - Default');

INSERT  INTO `cloud`.`role_permissions` (uuid, role_id, rule, permission, sort_order)
SELECT  UUID(), role_id, 'quotaBalance', 'ALLOW', MAX(sort_order)-2
FROM    `cloud`.`role_permissions` RP
WHERE   role_id = (SELECT id FROM `cloud`.`roles` WHERE name = 'Read-Only User - Default');

-- Add permission for domain admins to call isAccountAllowedToCreateOfferingsWithTags API

INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`)
SELECT UUID(), `roles`.`id`, 'isAccountAllowedToCreateOfferingsWithTags', 'ALLOW'
FROM `cloud`.`roles` WHERE `role_type` = 'DomainAdmin';

--
-- Update Configuration Groups and Subgroups
--
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.configuration', 'group_id', 'bigint unsigned DEFAULT 1 COMMENT "group id this configuration belongs to" ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.configuration', 'subgroup_id', 'bigint unsigned DEFAULT 1 COMMENT "subgroup id this configuration belongs to" ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.configuration', 'parent', 'VARCHAR(255) DEFAULT NULL COMMENT "name of the parent configuration if this depends on it" ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.configuration', 'display_text', 'VARCHAR(255) DEFAULT NULL COMMENT "Short text about configuration to display to the users" ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.configuration', 'kind', 'VARCHAR(255) DEFAULT NULL COMMENT "kind of the value such as order, csv, etc" ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.configuration', 'options', 'VARCHAR(255) DEFAULT NULL COMMENT "possible options for the value" ');

CREATE TABLE `cloud`.`configuration_group` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(255) NOT NULL COMMENT 'name of the configuration group',
  `description` varchar(1024) DEFAULT NULL COMMENT 'description of the configuration group',
  `precedence` bigint(20) unsigned DEFAULT '999' COMMENT 'precedence for the configuration group',
  PRIMARY KEY (`id`),
  UNIQUE KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`configuration_subgroup` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(255) NOT NULL COMMENT 'name of the configuration subgroup',
  `keywords` varchar(4096) DEFAULT NULL COMMENT 'comma-separated keywords for the configuration subgroup',
  `precedence` bigint(20) unsigned DEFAULT '999' COMMENT 'precedence for the configuration subgroup',
  `group_id` bigint(20) unsigned NOT NULL COMMENT 'configuration group id',
  PRIMARY KEY (`id`),
  UNIQUE KEY (`name`, `group_id`),
  CONSTRAINT `fk_configuration_subgroup__group_id` FOREIGN KEY (`group_id`) REFERENCES `configuration_group` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`configuration_group` AUTO_INCREMENT=1;

INSERT INTO `cloud`.`configuration_group` (`name`, `description`, `precedence`) VALUES ('Miscellaneous', 'Miscellaneous configuration', 999);
INSERT INTO `cloud`.`configuration_group` (`name`, `description`, `precedence`) VALUES ('Access', 'Identity and Access management configuration', 1);
INSERT INTO `cloud`.`configuration_group` (`name`, `description`, `precedence`) VALUES ('Compute', 'Compute configuration', 2);
INSERT INTO `cloud`.`configuration_group` (`name`, `description`, `precedence`) VALUES ('Storage', 'Storage configuration', 3);
INSERT INTO `cloud`.`configuration_group` (`name`, `description`, `precedence`) VALUES ('Network', 'Network configuration', 4);
INSERT INTO `cloud`.`configuration_group` (`name`, `description`, `precedence`) VALUES ('Hypervisor', 'Hypervisor specific configuration', 5);
INSERT INTO `cloud`.`configuration_group` (`name`, `description`, `precedence`) VALUES ('Management Server', 'Management Server configuration', 6);
INSERT INTO `cloud`.`configuration_group` (`name`, `description`, `precedence`) VALUES ('System VMs', 'System VMs related configuration', 7);
INSERT INTO `cloud`.`configuration_group` (`name`, `description`, `precedence`) VALUES ('Infrastructure', 'Infrastructure configuration', 8);

ALTER TABLE `cloud`.`configuration_subgroup` AUTO_INCREMENT=1;

INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Others', NULL, 999, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Miscellaneous'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Account', NULL, 1, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Access'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Domain', NULL, 2, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Access'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Project', NULL, 3, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Access'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('LDAP', NULL, 4, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Access'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('SAML', 'saml2', 5, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Access'));

INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Virtual Machine', 'vm,instance,cpu,ssh,affinity', 1, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Compute'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Kubernetes', 'kubernetes', 2, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Compute'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('High Availability', 'ha', 3, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Compute'));

INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Images', 'template,iso', 1, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Storage'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Volume', 'disk,diskoffering', 2, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Storage'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Snapshot', NULL, 3, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Storage'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('VM Snapshot', 'vmsnapshot', 4, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Storage'));

INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Network', 'firewall,vlan,dns,globodns,ipaddress,cidr', 1, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Network'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('DHCP', 'externaldhcp', 2, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Network'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('VPC', NULL, 3, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Network'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('LoadBalancer', 'lb,gslb', 4, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Network'));

INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('API', NULL, 1, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Management Server'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Alerts', 'alert', 2, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Management Server'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Events', 'event', 3, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Management Server'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Security', 'secure,password,authenticators', 4, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Management Server'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Usage', NULL, 5, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Management Server'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Limits', 'capacity,delay,interval,workers', 6, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Management Server'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Jobs', 'job', 7, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Management Server'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Agent', NULL, 8, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Management Server'));

INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Hypervisor', 'host', 1, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Hypervisor'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('KVM', 'libvirt', 2, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Hypervisor'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('VMware', 'vcenter', 3, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Hypervisor'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('XenServer', 'xen,xapi,XCP', 4, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Hypervisor'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('OVM', 'ovm3', 5, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Hypervisor'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Baremetal', NULL, 6, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Hypervisor'));

INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('ConsoleProxyVM', 'cpvm,consoleproxy,novnc', 1, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'System VMs'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('SecStorageVM', 'ssvm,secondary', 2, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'System VMs'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('VirtualRouter', 'vr,router,vrouter', 3, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'System VMs'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Diagnostics', NULL, 4, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'System VMs'));

INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Primary Storage', 'storage,pool,primary', 1, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Infrastructure'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Secondary Storage', 'image,secstorage', 2, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Infrastructure'));

INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Backup & Recovery', 'backup,recovery,veeam', 1, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Miscellaneous'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Certificate Authority', 'CA', 2, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Miscellaneous'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Quota', NULL, 3, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Miscellaneous'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Cloudian', NULL, 4, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Miscellaneous'));

UPDATE `cloud`.`configuration` SET parent = 'agent.lb.enabled' WHERE name IN ('agent.load.threshold');
UPDATE `cloud`.`configuration` SET parent = 'indirect.agent.lb.check.interval' WHERE name IN ('indirect.agent.lb.algorithm');
UPDATE `cloud`.`configuration` SET parent = 'alert.purge.delay' WHERE name IN ('alert.purge.interval');
UPDATE `cloud`.`configuration` SET parent = 'api.throttling.enabled' WHERE name IN ('api.throttling.cachesize', 'api.throttling.interval', 'api.throttling.max');
UPDATE `cloud`.`configuration` SET parent = 'backup.framework.enabled' WHERE name IN ('backup.framework.provider.plugin', 'backup.framework.sync.interval');
UPDATE `cloud`.`configuration` SET parent = 'cloud.kubernetes.service.enabled' WHERE name IN ('cloud.kubernetes.cluster.max.size', 'cloud.kubernetes.cluster.network.offering', 'cloud.kubernetes.cluster.scale.timeout', 'cloud.kubernetes.cluster.start.timeout', 'cloud.kubernetes.cluster.upgrade.timeout', 'cloud.kubernetes.cluster.experimental.features.enabled');
UPDATE `cloud`.`configuration` SET parent = 'diagnostics.data.gc.enable' WHERE name IN ('diagnostics.data.gc.interval', 'diagnostics.data.max.file.age');
UPDATE `cloud`.`configuration` SET parent = 'enable.additional.vm.configuration' WHERE name IN ('allow.additional.vm.configuration.list.kvm', 'allow.additional.vm.configuration.list.vmware', 'allow.additional.vm.configuration.list.xenserver');
UPDATE `cloud`.`configuration` SET parent = 'event.purge.delay' WHERE name IN ('event.purge.interval');
UPDATE `cloud`.`configuration` SET parent = 'network.loadbalancer.basiczone.elb.enabled' WHERE name IN ('network.loadbalancer.basiczone.elb.network', 'network.loadbalancer.basiczone.elb.vm.cpu.mhz', 'network.loadbalancer.basiczone.elb.vm.ram.size', 'network.loadbalancer.basiczone.elb.vm.vcpu.num', 'network.loadbalancer.basiczone.elb.gc.interval.minutes');
UPDATE `cloud`.`configuration` SET parent = 'prometheus.exporter.enable' WHERE name IN ('prometheus.exporter.port', 'prometheus.exporter.allowed.ips');
UPDATE `cloud`.`configuration` SET parent = 'router.health.checks.enable' WHERE name IN ('router.health.checks.basic.interval', 'router.health.checks.advanced.interval', 'router.health.checks.config.refresh.interval', 'router.health.checks.results.fetch.interval', 'router.health.checks.to.exclude', 'router.health.checks.failures.to.recreate.vr', 'router.health.checks.free.disk.space.threshold', 'router.health.checks.max.cpu.usage.threshold', 'router.health.checks.max.memory.usage.threshold');
UPDATE `cloud`.`configuration` SET parent = 'storage.cache.replacement.enabled' WHERE name IN ('storage.cache.replacement.interval', 'storage.cache.replacement.lru.interval');
UPDATE `cloud`.`configuration` SET parent = 'storage.cleanup.enabled' WHERE name IN ('storage.cleanup.interval', 'storage.cleanup.delay', 'storage.template.cleanup.enabled');
UPDATE `cloud`.`configuration` SET parent = 'vm.configdrive.primarypool.enabled' WHERE name IN ('vm.configdrive.use.host.cache.on.unsupported.pool');

UPDATE `cloud`.`configuration` SET display_text = CONCAT(UCASE(LEFT(REPLACE(name, ".", " "), 1)), LCASE(SUBSTRING(REPLACE(name, ".", " "), 2)));

UPDATE `cloud`.`configuration` SET
    `kind` = 'Order',
    `options` = 'HostAntiAffinityProcessor,ExplicitDedicationProcessor,HostAffinityProcessor'
where `name` = 'affinity.processors.order' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Order',
    `options` = 'FirstFitPlanner,UserDispersingPlanner,UserConcentratedPodPlanner,ImplicitDedicationPlanner,BareMetalPlanner'
    where `name` = 'deployment.planners.order' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Order',
    `options` = 'SimpleInvestigator,XenServerInvestigator,KVMInvestigator,HypervInvestigator,VMwareInvestigator,PingInvestigator,ManagementIPSysVMInvestigator,Ovm3Investigator'
where `name` = 'ha.investigators.order' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Order',
    `options` = 'FirstFitRouting'
where `name` = 'host.allocators.order' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Order',
    `options` = 'SAML2Auth'
where `name` = 'pluggableApi.authenticators.order' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Order',
    `options` = 'AffinityGroupAccessChecker,DomainChecker'
where `name` = 'security.checkers.order' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Order',
    `options` = 'LocalStorage,ClusterScopeStoragePoolAllocator,ZoneWideStoragePoolAllocator'
where `name` = 'storage.pool.allocators.order' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Order',
    `options` = 'PBKDF2,SHA256SALT,MD5,LDAP,SAML2,PLAINTEXT'
where `name` = 'user.authenticators.order' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Order',
    `options` = 'PBKDF2,SHA256SALT,MD5,LDAP,SAML2,PLAINTEXT'
where `name` = 'user.password.encoders.order' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'CSV'
where `name` like "%.list" ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'CSV'
where `name` like "%.defaults" ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'CSV'
where `name` like "%.details" ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'CSV'
where `name` like "%.exclude" ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'CSV'
where `name` IN ("alert.email.addresses", "allow.additional.vm.configuration.list.kvm", "allow.additional.vm.configuration.list.xenserver", "host",
    "network.dhcp.nondefaultnetwork.setgateway.guestos", "router.health.checks.failures.to.recreate.vr", "router.health.checks.to.exclude") ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'Error,Migration,ForceStop'
where `name` = 'host.maintenance.local.storage.strategy' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'SHA256withRSA'
where `name` = 'ca.framework.cert.signature.algorithm' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'firstfitleastconsumed,random'
where `name` = 'image.store.allocation.algorithm' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'static,roundrobin,shuffle'
where `name` = 'indirect.agent.lb.algorithm' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'random,firstfit,userdispersing,userconcentratedpod_random,userconcentratedpod_firstfit,firstfitleastconsumed'
where `name` = 'vm.allocation.algorithm' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'all,pod'
where `name` = 'network.dns.basiczone.updates' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'global,guest-network,link-local,disabled,all,default'
where `name` = 'network.loadbalancer.haproxy.stats.visibility' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'SHA1,SHA256,SHA384,SHA512'
where `name` = 'saml2.sigalg' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'FirstFitPlanner,UserDispersingPlanner,UserConcentratedPodPlanner'
where `name` = 'vm.deployment.planner' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'scsi,ide,osdefault'
where `name` = 'vmware.root.disk.controller' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'E1000,PCNet32,Vmxnet2,Vmxnet3'
where `name` = 'vmware.systemvm.nic.device.type' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'first,last,random'
where `name` = 'vrouter.redundant.tiers.placement' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'xenserver56,xenserver61'
where `name` = 'xenserver.pvdriver.version' ;

--- Create table for handling console sessions #7094

CREATE TABLE IF NOT EXISTS `cloud`.`console_session` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `uuid` varchar(40) NOT NULL COMMENT 'UUID generated for the session',
    `created` datetime NOT NULL COMMENT 'When the session was created',
    `account_id` bigint(20) unsigned NOT NULL COMMENT 'Account who generated the session',
    `user_id` bigint(20) unsigned NOT NULL COMMENT 'User who generated the session',
    `instance_id` bigint(20) unsigned NOT NULL COMMENT 'VM for which the session was generated',
    `host_id` bigint(20) unsigned NOT NULL COMMENT 'Host where the VM was when the session was generated',
    `acquired` int(1) NOT NULL DEFAULT 0 COMMENT 'True if the session was already used',
    `removed` datetime COMMENT 'When the session was removed/used',
    CONSTRAINT `fk_consolesession__account_id` FOREIGN KEY(`account_id`) REFERENCES `cloud`.`account` (`id`),
    CONSTRAINT `fk_consolesession__user_id` FOREIGN KEY(`user_id`) REFERENCES `cloud`.`user`(`id`),
    CONSTRAINT `fk_consolesession__instance_id` FOREIGN KEY(`instance_id`) REFERENCES `cloud`.`vm_instance`(`id`),
    CONSTRAINT `fk_consolesession__host_id` FOREIGN KEY(`host_id`) REFERENCES `cloud`.`host`(`id`),
    CONSTRAINT `uc_consolesession__uuid` UNIQUE (`uuid`)
);

-- Add assignVolume API permission to default resource admin and domain admin
INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`) VALUES (UUID(), 2, 'assignVolume', 'ALLOW');
INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`) VALUES (UUID(), 3, 'assignVolume', 'ALLOW');

-- Increases the precision of the column `quota_used` from 15 to 20, keeping the scale of 8.

ALTER TABLE `cloud_usage`.`quota_usage` MODIFY COLUMN quota_used decimal(20,8) unsigned NOT NULL;

ALTER TABLE `cloud`.`user` ADD COLUMN `is_user_2fa_enabled` tinyint NOT NULL DEFAULT 0;
ALTER TABLE `cloud`.`user` ADD COLUMN `key_for_2fa` varchar(255) default NULL;
ALTER TABLE `cloud`.`user` ADD COLUMN `user_2fa_provider` varchar(255) default NULL;

-- Change usage of VM_DISK_IO_WRITE to use right usage_type
UPDATE
  `cloud_usage`.`cloud_usage`
SET
  usage_type = 22
WHERE
  usage_type = 24 AND usage_display like '% io write';

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.guest_os', 'display', 'tinyint(1) DEFAULT ''1'' COMMENT ''should this guest_os be shown to the end user'' ');
