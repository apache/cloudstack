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
-- Schema upgrade from 4.20.0.0 to 4.20.1.0
--;

-- Add column api_key_access to user and account tables
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.user', 'api_key_access', 'boolean DEFAULT NULL COMMENT "is api key access allowed for the user" AFTER `secret_key`');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.account', 'api_key_access', 'boolean DEFAULT NULL COMMENT "is api key access allowed for the account" ');
CALL `cloud_usage`.`IDEMPOTENT_ADD_COLUMN`('cloud_usage.account', 'api_key_access', 'boolean DEFAULT NULL COMMENT "is api key access allowed for the account" ');

-- Create a new group for Usage Server related configurations
INSERT INTO `cloud`.`configuration_group` (`name`, `description`, `precedence`) VALUES ('Usage Server', 'Usage Server related configuration', 9);
UPDATE `cloud`.`configuration_subgroup` set `group_id` = (SELECT `id` FROM `cloud`.`configuration_group` WHERE `name` = 'Usage Server'), `precedence` = 1 WHERE `name`='Usage';
UPDATE `cloud`.`configuration` SET `group_id` = (SELECT `id` FROM `cloud`.`configuration_group` WHERE `name` = 'Usage Server') where `subgroup_id` = (SELECT `id` FROM `cloud`.`configuration_subgroup` WHERE `name` = 'Usage');

-- Update the description to indicate this setting applies only to volume snapshots on running instances
UPDATE `cloud`.`configuration` SET `description`='whether volume snapshot is enabled on running instances on KVM hosts' WHERE `name`='kvm.snapshot.enabled';

-- Modify index for mshost_peer
DELETE FROM `cloud`.`mshost_peer`;
CALL `cloud`.`IDEMPOTENT_DROP_FOREIGN_KEY`('cloud.mshost_peer','fk_mshost_peer__owner_mshost');
CALL `cloud`.`IDEMPOTENT_DROP_INDEX`('i_mshost_peer__owner_peer_runid','mshost_peer');
CALL `cloud`.`IDEMPOTENT_ADD_UNIQUE_KEY`('cloud.mshost_peer', 'i_mshost_peer__owner_peer', '(owner_mshost, peer_mshost)');
CALL `cloud`.`IDEMPOTENT_ADD_FOREIGN_KEY`('cloud.mshost_peer', 'fk_mshost_peer__owner_mshost', '(owner_mshost)', '`mshost`(`id`)');

-- Add last_id to the volumes table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.volumes', 'last_id', 'bigint(20) unsigned DEFAULT NULL');

-- Add used_iops column to support IOPS data in storage stats
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.storage_pool', 'used_iops', 'bigint unsigned DEFAULT NULL COMMENT "IOPS currently in use for this storage pool" ');

-- Add reason column for op_ha_work
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.op_ha_work', 'reason', 'varchar(32) DEFAULT NULL COMMENT "Reason for the HA work"');

-- Support for XCP-ng 8.3.0 and XenServer 8.4 by adding hypervisor capabilities
-- https://docs.xenserver.com/en-us/xenserver/8/system-requirements/configuration-limits.html
-- https://docs.xenserver.com/en-us/citrix-hypervisor/system-requirements/configuration-limits.html
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid, hypervisor_type, hypervisor_version, max_guests_limit, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported) VALUES (UUID(), 'XenServer', '8.3.0', 1000, 254, 64, 1);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid, hypervisor_type, hypervisor_version, max_guests_limit, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported) VALUES (UUID(), 'XenServer', '8.4.0', 1000, 240, 64, 1);

-- Add missing and new Guest OS mappings
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (10, 'Ubuntu 24.04 LTS', 'KVM', 'default', 'Ubuntu 24.04 LTS');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (2, 'Debian GNU/Linux 10 (64-bit)', 'XenServer', '8.2.1', 'Debian Buster 10');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (10, 'Ubuntu 20.04 LTS', 'XenServer', '8.2.1', 'Ubuntu Focal Fossa 20.04');

-- Copy XS 8.2.1 hypervisor guest OS mappings to XS 8.3 and 8.3 mappings to 8.4
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'Xenserver', '8.3.0', guest_os_name, guest_os_id, utc_timestamp(), 0 FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='Xenserver' AND hypervisor_version='8.2.1';

CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Rocky Linux 9', 'XenServer', '8.3.0', 'Rocky Linux 9');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Rocky Linux 8', 'XenServer', '8.3.0', 'Rocky Linux 8');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'AlmaLinux 9', 'XenServer', '8.3.0', 'AlmaLinux 9');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'AlmaLinux 8', 'XenServer', '8.3.0', 'AlmaLinux 8');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (3, 'Oracle Linux 9', 'XenServer', '8.3.0', 'Oracle Linux 9');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (3, 'Oracle Linux 8', 'XenServer', '8.3.0', 'Oracle Linux 8');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (4, 'Red Hat Enterprise Linux 8.0', 'XenServer', '8.3.0', 'Red Hat Enterprise Linux 8');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (4, 'Red Hat Enterprise Linux 9.0', 'XenServer', '8.3.0', 'Red Hat Enterprise Linux 9');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (2, 'Debian GNU/Linux 12 (64-bit)', 'XenServer', '8.3.0', 'Debian Bookworm 12');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (10, 'Ubuntu 22.04 LTS', 'XenServer', '8.3.0', 'Ubuntu Jammy Jellyfish 22.04');

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'Xenserver', '8.4.0', guest_os_name, guest_os_id, utc_timestamp(), 0 FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='Xenserver' AND hypervisor_version='8.3.0';
