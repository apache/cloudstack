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


CREATE TABLE IF NOT EXISTS `quota_mapping` (
  `id` bigint(20) unsigned NOT NULL COMMENT 'id',
  `usage_type` int(2) unsigned DEFAULT NULL,
  `usage_name` varchar(255) NOT NULL COMMENT 'usage type',
  `usage_unit` varchar(255) NOT NULL COMMENT 'usage type',
  `usage_discriminator` varchar(255) NOT NULL COMMENT 'usage type',
  `currency_value` decimal(15,2) NOT NULL COMMENT 'usage type',
  `include` tinyint(1) NOT NULL COMMENT 'usage type',
  `description` varchar(255) NOT NULL COMMENT 'usage type',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


LOCK TABLES `quota_mapping` WRITE;
INSERT INTO `quota_mapping` VALUES 
 (1,1,'RUNNING_VM','Compute-Month','',5.00,1,'Quota mapping for running VM'),
 (2,2,'ALLOCATED_VM','Compute-Month','',10.00,1,'Quota mapping for allocsated VM'),
 (3,3,'IP_ADDRESS','IP-Month','',5.12,1,'Quota mapping for IP address in use'),
 (4,4,'NETWORK_BYTES_SENT','GB','',1.00,1,'Quota mapping for network bytes sent'),
 (5,5,'NETWORK_BYTES_RECEIVED','GB','',1.00,1,'Quota mapping for network bytes received'),
 (6,6,'VOLUME','GB-Month','',5.00,1,'Quota mapping for volume usage per month'),
 (7,7,'TEMPLATE','GB-Month','',5.00,1,'Quota mapping for template usage per month'),
 (8,8,'ISO','GB-Month','',5.00,1,'Quota mapping for ISO storage per month'),
 (9,9,'SNAPSHOT','GB-Month','',5.00,1,'Quota mapping for snapshot usage per month'),
 (10,10,'SECURITY_GROUP','Policy-Month','',5.00,1,'Quota mapping for Security groups'),
 (11,11,'LOAD_BALANCER_POLICY','Policy-Month','',5.00,1,'Quota mapping load balancer policy use per hour'),
 (12,12,'PORT_FORWARDING_RULE','Policy-Month','',5.00,1,'Quota mapping port forwarding rule useper hour'),
 (13,13,'NETWORK_OFFERING','Policy-Month','',5.00,1,'Quota mapping for network offering usage per hour'),
 (14,14,'VPN_USERS','Policy-Month','',5.00,1,'Quota mapping for using VPN'),
 (15,15,'CPU_SPEED','Compute-Month','100MHz',5.00,1,'Quota mapping for 100 MHz of CPU running for an hour'),
 (16,16,'vCPU','Compute-Month','1VCPU',5.00,1,'Quota mapping for running VM that has 1vCPU'),
 (17,17,'MEMORY','Compute-Month','1MB',5.00,1,'Quota mapping for usign 1MB or RAM for 1 hour'),
 (18,21,'VM_DISK_IO_READ','GB','1',5.00,1,'Quota mapping for 1GB of disk IO read'),
 (19,22,'VM_DISK_IO_WRITE','GB','1',5.00,1,'Quota mapping for 1GB of disk data write'),
 (20,23,'VM_DISK_BYTES_READ','GB','1',5.00,1,'Quota mapping for disk bytes read'),
 (21,24,'VM_DISK_BYTES_WRITE','GB','1',5.00,1,'Quota mapping for disk bytes write'),
 (22,25,'VM_SNAPSHOT','GB-Month','',5.00,1,'Quota mapping for running VM');
UNLOCK TABLES;


CREATE TABLE IF NOT EXISTS `cloud_usage`.`quota_credits` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `account_id` bigint unsigned NOT NULL,
  `domain_id` bigint(20) unsigned NOT NULL,
  `credit` decimal(15,2) COMMENT 'amount credited',
  `updated_on` datetime NOT NULL COMMENT 'date created',
  `updated_by` bigint unsigned NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE IF NOT EXISTS `cloud_usage`.`quota_usage` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `usage_item_id` bigint unsigned NOT NULL,
  `usage_type` varchar(64) DEFAULT NULL,
  `quota_used` int unsigned NOT NULL,
  `start_date` datetime NOT NULL COMMENT 'start time for this usage item',
  `end_date` datetime NOT NULL COMMENT 'end time for this usage item',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE IF NOT EXISTS `cloud_usage`.`quota_balance` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `account_id` bigint unsigned NOT NULL,
  `domain_id` bigint(20) unsigned NOT NULL,
  `credit_balance` decimal(15,2) COMMENT 'amount of credits remaining',
  `updated_on` datetime NOT NULL COMMENT 'date updated on',
  `previous_update_id` bigint unsigned NOT NULL COMMENT 'id of last update',
  `previous_update_on` datetime NOT NULL COMMENT 'date of last update',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE IF NOT EXISTS `cloud_usage.quota_email_templates` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `template_name` varchar(64) DEFAULT NULL,
  `template_text` longtext,
  `category` int(10) unsigned NOT NULL DEFAULT '0',
  `last_updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `locale` varchar(25) DEFAULT 'en_US',
  `version` int(11) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE IF NOT EXISTS `cloud_usage.quota_sent_emails` (
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
