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

UPDATE IGNORE `cloud`.`configuration` SET `default_value`='PBKDF2,SHA256SALT,MD5,LDAP,SAML2,PLAINTEXT' WHERE name='user.authenticators.order';
UPDATE IGNORE `cloud`.`configuration` SET `value`='PBKDF2,SHA256SALT,MD5,LDAP,SAML2,PLAINTEXT' WHERE name='user.authenticators.order';
UPDATE IGNORE `cloud`.`configuration` SET `default_value`='PBKDF2,SHA256SALT,MD5,LDAP,SAML2,PLAINTEXT' WHERE name='user.password.encoders.order';
UPDATE IGNORE `cloud`.`configuration` SET `value`='PBKDF2,SHA256SALT,MD5,LDAP,SAML2,PLAINTEXT' WHERE name='user.password.encoders.order';
UPDATE IGNORE `cloud`.`configuration` SET `value`="MD5,LDAP,PLAINTEXT" WHERE `name`="user.password.encoders.exclude";


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

CREATE TABLE `cloud_usage`.`quota_configuration` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `usage_type` varchar(255) NOT NULL COMMENT 'usage type',
  `usage_unit` varchar(255) NOT NULL COMMENT 'usage type',
  `usage_discriminator` varchar(255) NOT NULL COMMENT 'usage type',
  `currency_value` varchar(255) NOT NULL COMMENT 'usage type',
  `include` BOOLEAN NOT NULL COMMENT 'usage type',
  `description` varchar(255) NOT NULL COMMENT 'usage type',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


INSERT IGNORE INTO `cloud_usage`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('RUNNING_VM', 'Compute-Hours', '', '5' , '1', 'Quota mapping for running VM');
INSERT IGNORE INTO `cloud_usage`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('ALLOCATED_VM', 'Compute-Hours', '', '1' , '1', 'Quota mapping for running VM');
INSERT IGNORE INTO `cloud_usage`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('IP_ADDRESS', 'IP-Hours', '', '0.1' , '1', 'Quota mapping for running VM');
INSERT IGNORE INTO `cloud_usage`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('NETWORK_BYTES_SENT', 'GB', '', '1' , '1', 'Quota mapping for running VM');
INSERT IGNORE INTO `cloud_usage`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('NETWORK_BYTES_RECEIVED', 'GB', '', '1' , '1', 'Quota mapping for running VM');
INSERT IGNORE INTO `cloud_usage`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('VOLUME', 'GB-Month', '', '5' , '1', 'Quota mapping for running VM');
INSERT IGNORE INTO `cloud_usage`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('TEMPLATE', 'GB-Month', '', '5' , '1', 'Quota mapping for running VM');
INSERT IGNORE INTO `cloud_usage`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('ISO', 'GB-Month', '', '5' , '1', 'Quota mapping for running VM');
INSERT IGNORE INTO `cloud_usage`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('SNAPSHOT', 'GB-Month', '', '5' , '1', 'Quota mapping for running VM');
INSERT IGNORE INTO `cloud_usage`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('LOAD_BALANCER_POLICY', 'Policy-Hours', '', '5' , '0', 'Quota mapping for running VM');
INSERT IGNORE INTO `cloud_usage`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('PORT_FORWARDING_RULE', 'Policy-Hours', '', '5' , '0', 'Quota mapping for running VM');
INSERT IGNORE INTO `cloud_usage`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('NETWORK_OFFERING', 'Policy-Hours', '', '5' , '0', 'Quota mapping for running VM');
INSERT IGNORE INTO `cloud_usage`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('CPU', 'Compute-Hours', '100MHz', '5' , '1', 'Quota mapping for 100 MHz of CPU running for an hour');
INSERT IGNORE INTO `cloud_usage`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('vCPU', 'Compute-Hours', '1VCPU', '5' , '1', 'Quota mapping for running VM that has 1vCPU');
INSERT IGNORE INTO `cloud_usage`.`quota_configuration` (`usage_type`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `description`) VALUES ('MEMORY', 'Compute-Hours', '1MB', '5' , '1', 'Quota mapping for usign 1MB or RAM for 1 hour');


CREATE TABLE `cloud_usage`.`quota_credits` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `account_id` bigint unsigned NOT NULL,
  `domain_id` bigint(20) unsigned NOT NULL,
  `credit` decimal(15,2) COMMENT 'amount credited',
  `updated_on` datetime NOT NULL COMMENT 'date created',
  `updated_by` bigint unsigned NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `cloud_usage`.`quota_usage` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `usage_item_id` bigint unsigned NOT NULL,
  `usage_type` varchar(64) DEFAULT NULL,
  `quota_used` int unsigned NOT NULL,
  `start_date` datetime NOT NULL COMMENT 'start time for this usage item',
  `end_date` datetime NOT NULL COMMENT 'end time for this usage item',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `cloud_usage`.`quota_balance` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `account_id` bigint unsigned NOT NULL,
  `domain_id` bigint(20) unsigned NOT NULL,
  `credit_balance` decimal(15,2) COMMENT 'amount of credits remaining',
  `updated_on` datetime NOT NULL COMMENT 'date updated on',
  `previous_update_id` bigint unsigned NOT NULL COMMENT 'id of last update',
  `previous_update_on` datetime NOT NULL COMMENT 'date of last update',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `cloud_usage.quota_email_templates` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `template_name` varchar(64) DEFAULT NULL,
  `template_text` longtext,
  `category` int(10) unsigned NOT NULL DEFAULT '0',
  `last_updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `locale` varchar(25) DEFAULT 'en_US',
  `version` int(11) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `cloud_usage.quota_sent_emails` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `from_address` varchar(1024) NOT NULL,
  `to_address` varchar(1024) NOT NULL,
  `cc_address` varchar(1024) DEFAULT NULL,
  `bcc_address` varchar(1024) DEFAULT NULL,
  `send_date` datetime NOT NULL,
  `subject` varchar(1024) NOT NULL,
  `mail_text` longtext NOT NULL,
  `version` int(11) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
