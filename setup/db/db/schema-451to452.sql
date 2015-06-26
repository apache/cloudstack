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
-- Schema upgrade from 4.5.1 to 4.5.2;
--;

-- SAML

DELETE FROM `cloud`.`configuration` WHERE name like 'saml%';

ALTER TABLE `cloud`.`user` ADD COLUMN `external_entity` text DEFAULT NULL COMMENT "reference to external federation entity";

DROP TABLE IF EXISTS `cloud`.`saml_token`;
CREATE TABLE `cloud`.`saml_token` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(255) UNIQUE NOT NULL COMMENT 'The Authn Unique Id',
  `domain_id` bigint unsigned DEFAULT NULL,
  `entity` text NOT NULL COMMENT 'Identity Provider Entity Id',
  `created` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_saml_token__domain_id` FOREIGN KEY(`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Quota Configuration

INSERT IGNORE INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `default_value`, `description`) VALUES ('Advanced', 'DEFAULT', 'QuotaService', 'quota.enable.service' , 'true', 'false', 'Enable or Disable Quota service');
INSERT IGNORE INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `default_value`, `description`) VALUES ('Advanced', 'DEFAULT', 'QuotaService', 'quota.period.type' , '2', '2', 'Quota period type: 1 for every x days, 2 for certain day of the month, 3 for yearly on activation day - default quota usage reporting cycle');
INSERT IGNORE INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `default_value`, `description`) VALUES ('Advanced', 'DEFAULT', 'QuotaService', 'quota.period.config' , '15', '15', 'The period config in number of days for the quota period type');
INSERT IGNORE INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `default_value`, `description`) VALUES ('Advanced', 'DEFAULT', 'QuotaService', 'quota.activity.generate' , 'true', 'false', 'Set true to enable a detailed log of the quota usage, rating and billing activity, on daily basis. Valid values (true, false)');
INSERT IGNORE INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `default_value`, `description`) VALUES ('Advanced', 'DEFAULT', 'QuotaService', 'quota.email.outgoing.record' , 'true', 'false', 'true means all the emails sent out will be stored in local DB, by default it is false');
INSERT IGNORE INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `default_value`, `description`) VALUES ('Advanced', 'DEFAULT', 'QuotaService', 'quota.enable.enforcement' , 'true', 'false', ' Enable the usage quota enforcement, i.e. on true exceeding quota the respective account will be locked.');
INSERT IGNORE INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `default_value`, `description`) VALUES ('Advanced', 'DEFAULT', 'QuotaService', 'quota.currency.symbol' , 'R', 'C', ' The symbol for the currency in use to measure usage.');
INSERT IGNORE INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `default_value`, `description`) VALUES ('Advanced', 'DEFAULT', 'QuotaService', 'quota.limit.critical' , '80', '80', 'A percentage limit for quota when it is reached user is sent and alert.');
INSERT IGNORE INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `default_value`, `description`) VALUES ('Advanced', 'DEFAULT', 'QuotaService', 'quota.limit.increment' , '5', '5', 'A percentage incremental limit that is added to criticalLimit in this increments, when breached a email is send to the user with details');

-- Quota Emailer

INSERT IGNORE INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `default_value`, `description`) VALUES ('Advanced', 'DEFAULT', 'QuotaService', 'quota.usage.smtp.host' , '', '', 'SMTP host to for email');
INSERT IGNORE INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `default_value`, `description`) VALUES ('Advanced', 'DEFAULT', 'QuotaService', 'quota.usage.smtp.connection.timeout' , '60', '60', 'SMTP server timeout in seconds');
INSERT IGNORE INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `default_value`, `description`) VALUES ('Advanced', 'DEFAULT', 'QuotaService', 'quota.usage.smtp.user' , '', '', 'SMTP user');
INSERT IGNORE INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `default_value`, `description`) VALUES ('Advanced', 'DEFAULT', 'QuotaService', 'quota.usage.smtp.password' , '', '', 'SMTP Password');
INSERT IGNORE INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `default_value`, `description`) VALUES ('Advanced', 'DEFAULT', 'QuotaService', 'quota.usage.smtp.port' , '', '', 'SMTP port');
INSERT IGNORE INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `default_value`, `description`) VALUES ('Advanced', 'DEFAULT', 'QuotaService', 'quota.usage.smtp.useAuth' , '', '', 'SMTP Auth type');

CREATE TABLE `cloud`.`quota_configuration` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `usage_type` varchar(255) NOT NULL COMMENT 'usage type',
  `usage_unit` varchar(255) NOT NULL COMMENT 'usage type',
  `usage_discriminator` varchar(255) NOT NULL COMMENT 'usage type',
  `currency_value` varchar(255) NOT NULL COMMENT 'usage type',
  `include` BOOLEAN NOT NULL COMMENT 'usage type',
  `description` varchar(255) NOT NULL COMMENT 'usage type',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


INSERT IGNORE INTO `cloud`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('RUNNING_VM', 'Compute-Hours', '', '5' , '1', 'Quota mapping for running VM');
INSERT IGNORE INTO `cloud`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('ALLOCATED_VM', 'Compute-Hours', '', '1' , '1', 'Quota mapping for running VM');
INSERT IGNORE INTO `cloud`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('IP_ADDRESS', 'IP-Hours', '', '0.1' , '1', 'Quota mapping for running VM');
INSERT IGNORE INTO `cloud`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('NETWORK_BYTES_SENT', 'GB', '', '1' , '1', 'Quota mapping for running VM');
INSERT IGNORE INTO `cloud`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('NETWORK_BYTES_RECEIVED', 'GB', '', '1' , '1', 'Quota mapping for running VM');
INSERT IGNORE INTO `cloud`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('VOLUME', 'GB-Month', '', '5' , '1', 'Quota mapping for running VM');
INSERT IGNORE INTO `cloud`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('TEMPLATE', 'GB-Month', '', '5' , '1', 'Quota mapping for running VM');
INSERT IGNORE INTO `cloud`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('ISO', 'GB-Month', '', '5' , '1', 'Quota mapping for running VM');
INSERT IGNORE INTO `cloud`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('SNAPSHOT', 'GB-Month', '', '5' , '1', 'Quota mapping for running VM');
INSERT IGNORE INTO `cloud`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('LOAD_BALANCER_POLICY', 'Policy-Hours', '', '5' , '0', 'Quota mapping for running VM');
INSERT IGNORE INTO `cloud`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('PORT_FORWARDING_RULE', 'Policy-Hours', '', '5' , '0', 'Quota mapping for running VM');
INSERT IGNORE INTO `cloud`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('NETWORK_OFFERING', 'Policy-Hours', '', '5' , '0', 'Quota mapping for running VM');
