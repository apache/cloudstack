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

DELETE FROM `cloud`.`configuration` WHERE name like 'saml%' and component='management-server';

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

-- Quota

ALTER TABLE `cloud_usage`.`cloud_usage` ADD COLUMN `quota_calculated` tinyint(1) DEFAULT 0 COMMENT "quota calculation status";

DROP TABLE IF EXISTS `cloud_usage`.`quota_account`;
CREATE TABLE `quota_account` (
      `account_id` int(11) NOT NULL,
      `quota_balance` decimal(15,2) NULL,
      `quota_balance_date` datetime NULL,
      `quota_enforce` int(1) DEFAULT NULL,
      `quota_min_balance` decimal(15,2) DEFAULT NULL,
      `quota_alert_date` datetime DEFAULT NULL,
      `quota_alert_type` int(11) DEFAULT NULL,
      PRIMARY KEY (`account_id`),
  CONSTRAINT `account_id` FOREIGN KEY (`account_id`) REFERENCES `cloud_usage`.`account` (`quota_enforce`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


CREATE TABLE IF NOT EXISTS `cloud_usage`.`quota_tariff` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `usage_type` int(2) unsigned DEFAULT NULL,
  `usage_name` varchar(255) NOT NULL COMMENT 'usage type',
  `usage_unit` varchar(255) NOT NULL COMMENT 'usage type',
  `usage_discriminator` varchar(255) NOT NULL COMMENT 'usage type',
  `currency_value` decimal(15,2) NOT NULL COMMENT 'usage type',
  `effective_on` datetime NOT NULL COMMENT 'date time on which this quota values will become effective',
  `updated_on` datetime NOT NULL COMMENT 'date this entry was updated on',
  `updated_by` bigint unsigned NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


LOCK TABLES `cloud_usage`.`quota_tariff` WRITE;
INSERT INTO `cloud_usage`.`quota_tariff` (`usage_type`, `usage_name`, `usage_unit`, `usage_discriminator`, `currency_value`, `include`, `effective_on`,  `updated_on`, `updated_by`) VALUES
 (1,'RUNNING_VM','Compute-Month','',0.00,1,'2010-05-04', '2010-05-04',1),
 (2,'ALLOCATED_VM','Compute-Month','',0.00,1,'2010-05-04', '2010-05-04',1),
 (3,'IP_ADDRESS','IP-Month','',0.00,1,'2010-05-04', '2010-05-04',1),
 (4,'NETWORK_BYTES_SENT','GB','',0.00,1,'2010-05-04', '2010-05-04',1),
 (5,'NETWORK_BYTES_RECEIVED','GB','',0.00,1,'2010-05-04', '2010-05-04',1),
 (6,'VOLUME','GB-Month','',0.00,1,'2010-05-04', '2010-05-04',1),
 (7,'TEMPLATE','GB-Month','',0.00,1,'2010-05-04', '2010-05-04',1),
 (8,'ISO','GB-Month','',0.00,1,'2010-05-04', '2010-05-04',1),
 (9,'SNAPSHOT','GB-Month','',0.00,1,'2010-05-04', '2010-05-04',1),
 (10,'SECURITY_GROUP','Policy-Month','',0.00,1,'2010-05-04', '2010-05-04',1),
 (11,'LOAD_BALANCER_POLICY','Policy-Month','',0.00,1,'2010-05-04', '2010-05-04',1),
 (12,'PORT_FORWARDING_RULE','Policy-Month','',0.00,1,'2010-05-04', '2010-05-04',1),
 (13,'NETWORK_OFFERING','Policy-Month','',0.00,1,'2010-05-04', '2010-05-04',1),
 (14,'VPN_USERS','Policy-Month','',0.00,1,'2010-05-04', '2010-05-04',1),
 (15,'CPU_SPEED','Compute-Month','100MHz',0.00,1,'2010-05-04', '2010-05-04',1),
 (16,'vCPU','Compute-Month','1VCPU',0.00,1,'2010-05-04', '2010-05-04',1),
 (17,'MEMORY','Compute-Month','1MB',0.00,1,'2010-05-04', '2010-05-04',1),
 (21,'VM_DISK_IO_READ','GB','1',0.00,1,'2010-05-04', '2010-05-04',1),
 (22,'VM_DISK_IO_WRITE','GB','1',0.00,1,'2010-05-04', '2010-05-04',1),
 (23,'VM_DISK_BYTES_READ','GB','1',0.00,1,'2010-05-04', '2010-05-04',1),
 (24,'VM_DISK_BYTES_WRITE','GB','1',0.00,1,'2010-05-04', '2010-05-04',1),
 (25,'VM_SNAPSHOT','GB-Month','',0.00,1,'2010-05-04', '2010-05-04',1);
UNLOCK TABLES;

CREATE TABLE IF NOT EXISTS `cloud_usage`.`quota_credits` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `account_id` bigint unsigned NOT NULL,
  `domain_id` bigint(20) unsigned NOT NULL,
  `credit` decimal(15,4) COMMENT 'amount credited',
  `updated_on` datetime NOT NULL COMMENT 'date created',
  `updated_by` bigint unsigned NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud_usage`.`quota_usage` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `usage_item_id` bigint(20) unsigned NOT NULL,
  `zone_id` bigint(20) unsigned NOT NULL,
  `account_id` bigint(20) unsigned NOT NULL,
  `domain_id` bigint(20) unsigned NOT NULL,
  `usage_type` varchar(64) DEFAULT NULL,
  `quota_used` decimal(15,8) unsigned NOT NULL,
  `start_date` datetime NOT NULL COMMENT 'start time for this usage item',
  `end_date` datetime NOT NULL COMMENT 'end time for this usage item',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;


CREATE TABLE IF NOT EXISTS `cloud_usage`.`quota_balance` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `account_id` bigint unsigned NOT NULL,
  `domain_id` bigint(20) unsigned NOT NULL,
  `credit_balance` decimal(15,8) COMMENT 'amount of credits remaining',
  `credits_id`  bigint unsigned COMMENT 'if not null then this entry corresponds to credit change quota_credits',
  `updated_on` datetime NOT NULL COMMENT 'date updated on',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE IF NOT EXISTS `cloud_usage`.`quota_email_templates` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `template_name` varchar(64) NOT NULL UNIQUE,
  `template_subject` longtext,
  `template_body` longtext,
  `locale` varchar(25) DEFAULT 'en_US',
  `updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

LOCK TABLES `cloud_usage`.`quota_email_templates` WRITE;
INSERT INTO `cloud_usage`.`quota_email_templates` (`template_name`, `template_subject`, `template_body`) VALUES
 ('QUOTA_LOW', 'Quota Usage Threshold crossed by your account ${accountName}', 'Your account ${accountName} in the domain ${domainName} has reached quota usage threshold, your current quota balance is ${quotaBalance}.'),
 ('QUOTA_EMPTY', 'Quota Exhausted, account ${accountName} is locked now', 'Your account ${accountName} in the domain ${domainName} has exhausted allocated quota and has been locked now, please contact the administrator.'),
 ('QUOTA_UNLOCK_ACCOUNT', 'Quota credits added, account ${accountName} is unlocked now', 'Your account ${accountName} in the domain ${domainName} has enough quota credits now with the current balance of ${quotaBalance}. Your account has been unlocked now.'),
 ('QUOTA_STATEMENT', 'Quota Statement for your account ${accountName}', 'Monthly quota statement of your account ${accountName} in the domain ${domainName}:<br>Balance = ${quotaBalance}<br>Total Usage = ${quotaUsage}.');
UNLOCK TABLES;

CREATE TABLE IF NOT EXISTS `cloud_usage`.`quota_sent_emails` (
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
