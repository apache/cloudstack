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
-- Schema upgrade from 4.22.0.0 to 4.22.1.0
--;

-- Add vm_id column to usage_event table for volume usage events
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.usage_event','vm_id', 'bigint UNSIGNED NULL COMMENT "VM ID associated with volume usage events"');
CALL `cloud_usage`.`IDEMPOTENT_ADD_COLUMN`('cloud_usage.usage_event','vm_id', 'bigint UNSIGNED NULL COMMENT "VM ID associated with volume usage events"');

-- Add vm_id column to cloud_usage.usage_volume table
CALL `cloud_usage`.`IDEMPOTENT_ADD_COLUMN`('cloud_usage.usage_volume','vm_id', 'bigint UNSIGNED NULL COMMENT "VM ID associated with the volume usage"');

ALTER TABLE `cloud`.`template_store_ref` MODIFY COLUMN `download_url` varchar(2048);

UPDATE `cloud`.`alert` SET type = 33 WHERE name = 'ALERT.VR.PUBLIC.IFACE.MTU';
UPDATE `cloud`.`alert` SET type = 34 WHERE name = 'ALERT.VR.PRIVATE.IFACE.MTU';
UPDATE `cloud`.`alert` SET type = 35 WHERE name = 'ALERT.NETWORK.IPV6SUBNET';
UPDATE `cloud`.`alert` SET type = 36 WHERE name = 'ALERT.TYPE.EXTENSION.PATH.NOT.READY';
UPDATE `cloud`.`alert` SET type = 37 WHERE name = 'ALERT.STORAGE.BACKUP';
UPDATE `cloud`.`alert` SET type = 38 WHERE name = 'ALERT.STORAGE.OBJECT';

-- Update configuration 'kvm.ssh.to.agent' description and is_dynamic fields
UPDATE `cloud`.`configuration` SET description = 'True if the management server will restart the agent service via SSH into the KVM hosts after or during maintenance operations', is_dynamic = 1 WHERE name = 'kvm.ssh.to.agent';

-- Sanitize legacy network-level addressing fields for Public networks
UPDATE `cloud`.`networks`
SET `broadcast_uri` = NULL,
	`gateway` = NULL,
	`cidr` = NULL,
	`ip6_gateway` = NULL,
	`ip6_cidr` = NULL
WHERE `traffic_type` = 'Public';

UPDATE `cloud`.`vm_template` SET guest_os_id = 99 WHERE name = 'kvm-default-vm-import-dummy-template';

-- Update existing vm_template records with NULL type to "USER"
UPDATE `cloud`.`vm_template` SET `type` = 'USER' WHERE `type` IS NULL;

-- remove unused config item
DELETE FROM `cloud`.`configuration` WHERE name = 'consoleproxy.cmd.port';

-- Drops the unused "backup_interval_type" column of the "cloud.backups" table
ALTER TABLE `cloud`.`backups` DROP COLUMN `backup_interval_type`;

-- Update `user.password.reset.mail.template` configuration value to match new logic
UPDATE `cloud`.`configuration`
SET value = CONCAT_WS('\n', 'Hello {{username}}!', 'You have requested to reset your password. Please click the following link to reset your password:', '{{{resetLink}}}', 'If you did not request a password reset, please ignore this email.', '', 'Regards,', 'The CloudStack Team')
WHERE name = 'user.password.reset.mail.template'
  AND value IN (CONCAT_WS('\n', 'Hello {{username}}!', 'You have requested to reset your password. Please click the following link to reset your password:', 'http://{{{resetLink}}}', 'If you did not request a password reset, please ignore this email.', '', 'Regards,', 'The CloudStack Team'), CONCAT_WS('\n', 'Hello {{username}}!', 'You have requested to reset your password. Please click the following link to reset your password:', '{{{domainUrl}}}{{{resetLink}}}', 'If you did not request a password reset, please ignore this email.', '', 'Regards,', 'The CloudStack Team'));
