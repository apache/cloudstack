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
-- Schema upgrade from 4.11.2.0 to 4.12.0.0
--;

-- [CLOUDSTACK-10314] Add reason column to ACL rule table
ALTER TABLE `cloud`.`network_acl_item` ADD COLUMN `reason` VARCHAR(2500) AFTER `display`;

-- [CLOUDSTACK-9846] Make provision to store content and subject for Alerts in separate columns.
ALTER TABLE `cloud`.`alert` ADD COLUMN `content` VARCHAR(5000);

-- Fix the name of the column used to hold IPv4 range in 'vlan' table.
ALTER TABLE `vlan` CHANGE `description` `ip4_range` varchar(255);

-- [CLOUDSTACK-10344] bug when moving ACL rules (change order with drag and drop)
-- We are only adding the permission to the default rules. Any custom rule must be configured by the root admin.
INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 2, 'moveNetworkAclItem', 'ALLOW', 100) ON DUPLICATE KEY UPDATE rule=rule;
INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 3, 'moveNetworkAclItem', 'ALLOW', 302) ON DUPLICATE KEY UPDATE rule=rule;
INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 4, 'moveNetworkAclItem', 'ALLOW', 260) ON DUPLICATE KEY UPDATE rule=rule;

UPDATE `cloud`.`async_job` SET `removed` = now() WHERE `removed` IS NULL;

-- PR#1448 update description of 'execute.in.sequence.network.element.commands' parameter to reflect an unused command that has been removed. The removed class command is 'UserDataCommand'.
update `cloud`.`configuration` set description = 'If set to true, DhcpEntryCommand, SavePasswordCommand, VmDataCommand will be synchronized on the agent side. If set to false, these commands become asynchronous. Default value is false.' where name = 'execute.in.sequence.network.element.commands'; 

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Storage', 'DEFAULT', 'StorageManager', 'kvm.storage.offline.migration.wait', '10800', 'Timeout in seconds for offline (non-live) storage migration to complete on KVM', '10800', null, 'Global', 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Storage', 'DEFAULT', 'StorageManager', 'kvm.storage.online.migration.wait', '10800', 'Timeout in seconds for online (live) storage migration to complete on KVM (migrateVirtualMachineWithVolume)', '10800', null, 'Global', 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Storage', 'DEFAULT', 'StorageManager', 'max.number.managed.clustered.file.systems', '200', 'XenServer and VMware only: Maximum number of managed SRs or datastores per compute cluster', '200', null, 'Cluster', 0);

-- add KVM Guest OS mapping for Windows Server 2019
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (276, UUID(), 6, 'Windows Server 2019 (64-bit)', now());
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'KVM', 'default', 'Windows Server 2019', 276, now(), 0);

-- changed fingerprint type to TEXT, it avoids db exception when creating the certificate issue #3123
ALTER TABLE `cloud`.`sslcerts` MODIFY `fingerprint` TEXT;