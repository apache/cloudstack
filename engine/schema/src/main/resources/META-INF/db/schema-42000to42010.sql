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
