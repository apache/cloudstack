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

-- Delete user vm details for guest CPU mode/model which are root admin only
DELETE FROM `cloud`.`user_vm_details` WHERE `name` IN ('guest.cpu.mode','guest.cpu.model');

-- Delete template details for guest CPU mode/model which are root admin only
DELETE FROM `cloud`.`vm_template_details` WHERE `name` IN ('guest.cpu.mode','guest.cpu.model');

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
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (2, 'Debian GNU/Linux 10 (64-bit)', 'XenServer', '8.2.1', 'Debian Buster 10');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (5, 'SUSE Linux Enterprise Server 15 (64-bit)', 'XenServer', '8.2.1', 'SUSE Linux Enterprise 15 (64-bit)');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2022 (64-bit)', 'XenServer', '8.2.1', 'Windows Server 2022 (64-bit)');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows 11 (64-bit)', 'XenServer', '8.2.1', 'Windows 11');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (10, 'Ubuntu 20.04 LTS', 'XenServer', '8.2.1', 'Ubuntu Focal Fossa 20.04');

-- Copy XS 8.2.1 hypervisor guest OS mappings to XS 8.3 and 8.3 mappings to 8.4
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'Xenserver', '8.3.0', guest_os_name, guest_os_id, utc_timestamp(), 0 FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='Xenserver' AND hypervisor_version='8.2.1';

-- Add new and missing guest os mappings for XS 8.3
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Rocky Linux 9', 'XenServer', '8.3.0', 'Rocky Linux 9');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Rocky Linux 8', 'XenServer', '8.3.0', 'Rocky Linux 8');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'AlmaLinux 9', 'XenServer', '8.3.0', 'AlmaLinux 9');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'AlmaLinux 8', 'XenServer', '8.3.0', 'AlmaLinux 8');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (2, 'Debian GNU/Linux 12 (64-bit)', 'XenServer', '8.3.0', 'Debian Bookworm 12');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (3, 'Oracle Linux 9', 'XenServer', '8.3.0', 'Oracle Linux 9');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (3, 'Oracle Linux 8', 'XenServer', '8.3.0', 'Oracle Linux 8');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (4, 'Red Hat Enterprise Linux 8.0', 'XenServer', '8.3.0', 'Red Hat Enterprise Linux 8');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (4, 'Red Hat Enterprise Linux 9.0', 'XenServer', '8.3.0', 'Red Hat Enterprise Linux 9');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (10, 'Ubuntu 22.04 LTS', 'XenServer', '8.3.0', 'Ubuntu Jammy Jellyfish 22.04');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (5, 'SUSE Linux Enterprise Server 12 SP5 (64-bit)', 'XenServer', '8.3.0', 'SUSE Linux Enterprise Server 12 SP5 (64-bit');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (4, 'NeoKylin Linux Server 7', 'XenServer', '8.3.0', 'NeoKylin Linux Server 7');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'CentOS Stream 9', 'XenServer', '8.3.0', 'CentOS Stream 9');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (4, 'Scientific Linux 7', 'XenServer', '8.3.0', 'Scientific Linux 7');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (7, 'Generic Linux UEFI', 'XenServer', '8.3.0', 'Generic Linux UEFI');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (7, 'Generic Linux BIOS', 'XenServer', '8.3.0', 'Generic Linux BIOS');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (2, 'Gooroom Platform 2.0', 'XenServer', '8.3.0', 'Gooroom Platform 2.0');

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'Xenserver', '8.4.0', guest_os_name, guest_os_id, utc_timestamp(), 0 FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='Xenserver' AND hypervisor_version='8.3.0';

-- Add new guest os mappings for XS 8.4 and KVM
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2025', 'XenServer', '8.4.0', 'Windows Server 2025');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (10, 'Ubuntu 24.04 LTS', 'XenServer', '8.4.0', 'Ubuntu Noble Numbat 24.04');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (2, 'Debian GNU/Linux 10 (64-bit)', 'KVM', 'default', 'Debian GNU/Linux 10 (64-bit)');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (2, 'Debian GNU/Linux 11 (64-bit)', 'KVM', 'default', 'Debian GNU/Linux 11 (64-bit)');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (2, 'Debian GNU/Linux 12 (64-bit)', 'KVM', 'default', 'Debian GNU/Linux 12 (64-bit)');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows 11 (64-bit)', 'KVM', 'default', 'Windows 11');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2025', 'KVM', 'default', 'Windows Server 2025');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (10, 'Ubuntu 24.04 LTS', 'KVM', 'default', 'Ubuntu 24.04 LTS');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'CentOS Stream 10 (preview)', 'XenServer', '8.4.0', 'CentOS Stream 10 (preview)');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'CentOS Stream 9', 'XenServer', '8.4.0', 'CentOS Stream 9');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (4, 'Scientific Linux 7', 'XenServer', '8.4.0', 'Scientific Linux 7');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (4, 'NeoKylin Linux Server 7', 'XenServer', '8.4.0', 'NeoKylin Linux Server 7');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (5, 'SUSE Linux Enterprise Server 12 SP5 (64-bit)', 'XenServer', '8.4.0', 'SUSE Linux Enterprise Server 12 SP5 (64-bit');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (2, 'Gooroom Platform 2.0', 'XenServer', '8.4.0', 'Gooroom Platform 2.0');

-- Grant access to 2FA APIs for the "Read-Only User - Default" role

CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('Read-Only User - Default', 'setupUserTwoFactorAuthentication', 'ALLOW');
CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('Read-Only User - Default', 'validateUserTwoFactorAuthenticationCode', 'ALLOW');
CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('Read-Only User - Default', 'listUserTwoFactorAuthenticatorProviders', 'ALLOW');

-- Grant access to 2FA APIs for the "Support User - Default" role

CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('Support User - Default', 'setupUserTwoFactorAuthentication', 'ALLOW');
CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('Support User - Default', 'validateUserTwoFactorAuthenticationCode', 'ALLOW');
CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('Support User - Default', 'listUserTwoFactorAuthenticatorProviders', 'ALLOW');

-- Grant access to 2FA APIs for the "Read-Only Admin - Default" role

CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('Read-Only Admin - Default', 'setupUserTwoFactorAuthentication', 'ALLOW');
CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('Read-Only Admin - Default', 'validateUserTwoFactorAuthenticationCode', 'ALLOW');

-- Grant access to 2FA APIs for the "Support Admin - Default" role

CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('Support Admin - Default', 'setupUserTwoFactorAuthentication', 'ALLOW');
CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('Support Admin - Default', 'validateUserTwoFactorAuthenticationCode', 'ALLOW');

-- Re-apply VPC: update default network offering for vpc tier to conserve_mode=1 (#8309)
UPDATE `cloud`.`network_offerings` SET conserve_mode=1 WHERE name='DefaultIsolatedNetworkOfferingForVpcNetworks';

-- Add featured column for guest_os_category
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.guest_os_category', 'featured', 'tinyint(1) NOT NULL DEFAULT 0 COMMENT "whether the category is featured or not" AFTER `uuid`');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.guest_os_category', 'sort_key', 'int NOT NULL DEFAULT 0 COMMENT "sort key used for customising sort method" AFTER `featured`');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.guest_os_category', 'created', 'date COMMENT "date on which the category was created" AFTER `sort_key`');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.guest_os_category', 'removed', 'date COMMENT "date removed if not null" AFTER `created`');
UPDATE `cloud`.`guest_os_category` SET `featured` = 1 WHERE `name` NOT IN ('Novel', 'None');

-- Begin: Changes for Guest OS category cleanup
-- Add new OS categories if not present
DROP PROCEDURE IF EXISTS `cloud`.`INSERT_CATEGORY_IF_NOT_EXIST`;
CREATE PROCEDURE `cloud`.`INSERT_CATEGORY_IF_NOT_EXIST`(IN os_name VARCHAR(255))
BEGIN
    IF NOT EXISTS ((SELECT 1 FROM `cloud`.`guest_os_category` WHERE name = os_name))
    THEN
        INSERT INTO `cloud`.`guest_os_category` (name, uuid)
            VALUES (os_name, UUID())
;   END IF
; END;

CALL `cloud`.`INSERT_CATEGORY_IF_NOT_EXIST`('Fedora');
CALL `cloud`.`INSERT_CATEGORY_IF_NOT_EXIST`('Rocky Linux');
CALL `cloud`.`INSERT_CATEGORY_IF_NOT_EXIST`('Alma Linux');

-- Move existing guest OS to new categories
DROP PROCEDURE IF EXISTS `cloud`.`UPDATE_CATEGORY_FOR_GUEST_OSES`;
CREATE PROCEDURE `cloud`.`UPDATE_CATEGORY_FOR_GUEST_OSES`(IN category_name VARCHAR(255), IN os_name VARCHAR(255))
BEGIN
    DECLARE category_id BIGINT
;   SELECT `id` INTO category_id
    FROM `cloud`.`guest_os_category`
    WHERE `name` = category_name
    LIMIT 1
;   IF category_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Category not found'
;   END IF
;   UPDATE `cloud`.`guest_os`
    SET `category_id` = category_id
    WHERE `display_name` LIKE CONCAT('%', os_name, '%')
; END;
CALL `cloud`.`UPDATE_CATEGORY_FOR_GUEST_OSES`('Rocky Linux', 'Rocky Linux');
CALL `cloud`.`UPDATE_CATEGORY_FOR_GUEST_OSES`('Alma Linux', 'Alma Linux');
CALL `cloud`.`UPDATE_CATEGORY_FOR_GUEST_OSES`('Fedora', 'Fedora');

-- Move existing guest OS whose category will be deleted to Other category
DROP PROCEDURE IF EXISTS `cloud`.`UPDATE_NEW_AND_DELETE_OLD_CATEGORY_FOR_GUEST_OS`;
CREATE PROCEDURE `cloud`.`UPDATE_NEW_AND_DELETE_OLD_CATEGORY_FOR_GUEST_OS`(IN to_category_name VARCHAR(255), IN from_category_name VARCHAR(255))
BEGIN
    DECLARE done INT DEFAULT 0
;   DECLARE to_category_id BIGINT
;   SELECT id INTO to_category_id
    FROM `cloud`.`guest_os_category`
    WHERE `name` = to_category_name
    LIMIT 1
;   IF to_category_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ToCategory not found'
;   END IF
;   UPDATE `cloud`.`guest_os`
    SET `category_id` = to_category_id
    WHERE `category_id` = (SELECT `id` FROM `cloud`.`guest_os_category` WHERE `name` = from_category_name)
;   UPDATE `cloud`.`guest_os_category` SET `removed``=now() WHERE `name` = from_category_name
; END;
CALL `cloud`.`UPDATE_NEW_AND_DELETE_OLD_CATEGORY_FOR_GUEST_OS`('Other', 'Novel');
CALL `cloud`.`UPDATE_NEW_AND_DELETE_OLD_CATEGORY_FOR_GUEST_OS`('Other', 'None');
CALL `cloud`.`UPDATE_NEW_AND_DELETE_OLD_CATEGORY_FOR_GUEST_OS`('Other', 'Unix');
CALL `cloud`.`UPDATE_NEW_AND_DELETE_OLD_CATEGORY_FOR_GUEST_OS`('Other', 'Mac');

-- Add featured column for cloud.guest_os_category
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.guest_os_category', 'featured', 'tinyint(1) NOT NULL DEFAULT 0 COMMENT "whether the category is featured or not" AFTER `uuid`');
UPDATE `cloud`.`guest_os_category` SET featured = 1;
-- Add sort_key column for cloud.guest_os_category
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.guest_os_category', 'sort_key', 'int NOT NULL DEFAULT 0 COMMENT "sort key used for customising sort method" AFTER `featured`');

-- Update sort order for all guest OS categories
UPDATE `cloud`.`guest_os_category`
SET `sort_key` = CASE
    WHEN `name` = 'Ubuntu' THEN 1
    WHEN `name` = 'Debian' THEN 2
    WHEN `name` = 'Fedora' THEN 3
    WHEN `name` = 'CentOS' THEN 4
    WHEN `name` = 'Rocky Linux' THEN 5
    WHEN `name` = 'Alma Linux' THEN 6
    WHEN `name` = 'Oracle' THEN 7
    WHEN `name` = 'RedHat' THEN 8
    WHEN `name` = 'SUSE' THEN 9
    WHEN `name` = 'Windows' THEN 10
    WHEN `name` = 'Other' THEN 11
    ELSE `sort_key`
END;
-- End: Changes for Guest OS category cleanup
