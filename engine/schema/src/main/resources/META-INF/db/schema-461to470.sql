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
-- Schema upgrade from 4.6.1 to 4.7.0;
--;

CREATE TABLE IF NOT EXISTS `cloud`.`domain_vlan_map` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `domain_id` bigint unsigned NOT NULL COMMENT 'domain id. foreign key to domain table',
  `vlan_db_id` bigint unsigned NOT NULL COMMENT 'database id of vlan. foreign key to vlan table',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_domain_vlan_map__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain` (`id`) ON DELETE CASCADE,
  INDEX `i_account_vlan_map__domain_id`(`domain_id`),
  CONSTRAINT `fk_domain_vlan_map__vlan_id` FOREIGN KEY (`vlan_db_id`) REFERENCES `vlan` (`id`) ON DELETE CASCADE,
  INDEX `i_account_vlan_map__vlan_id`(`vlan_db_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Quota

CREATE TABLE IF NOT EXISTS `cloud_usage`.`quota_account` (
      `account_id` int(11) NOT NULL,
      `quota_balance` decimal(15,2) NULL,
      `quota_balance_date` datetime NULL,
      `quota_enforce` int(1) DEFAULT NULL,
      `quota_min_balance` decimal(15,2) DEFAULT NULL,
      `quota_alert_date` datetime DEFAULT NULL,
      `quota_alert_type` int(11) DEFAULT NULL,
      `last_statement_date` datetime DEFAULT NULL,
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
INSERT IGNORE INTO `cloud_usage`.`quota_tariff` (`usage_type`, `usage_name`, `usage_unit`, `usage_discriminator`, `currency_value`, `effective_on`,  `updated_on`, `updated_by`) VALUES
 (1,'RUNNING_VM','Compute-Month','',0.00,'2010-05-04', '2010-05-04',1),
 (2,'ALLOCATED_VM','Compute-Month','',0.00,'2010-05-04', '2010-05-04',1),
 (3,'IP_ADDRESS','IP-Month','',0.00,'2010-05-04', '2010-05-04',1),
 (4,'NETWORK_BYTES_SENT','GB','',0.00,'2010-05-04', '2010-05-04',1),
 (5,'NETWORK_BYTES_RECEIVED','GB','',0.00,'2010-05-04', '2010-05-04',1),
 (6,'VOLUME','GB-Month','',0.00,'2010-05-04', '2010-05-04',1),
 (7,'TEMPLATE','GB-Month','',0.00,'2010-05-04', '2010-05-04',1),
 (8,'ISO','GB-Month','',0.00,'2010-05-04', '2010-05-04',1),
 (9,'SNAPSHOT','GB-Month','',0.00,'2010-05-04', '2010-05-04',1),
 (10,'SECURITY_GROUP','Policy-Month','',0.00,'2010-05-04', '2010-05-04',1),
 (11,'LOAD_BALANCER_POLICY','Policy-Month','',0.00,'2010-05-04', '2010-05-04',1),
 (12,'PORT_FORWARDING_RULE','Policy-Month','',0.00,'2010-05-04', '2010-05-04',1),
 (13,'NETWORK_OFFERING','Policy-Month','',0.00,'2010-05-04', '2010-05-04',1),
 (14,'VPN_USERS','Policy-Month','',0.00,'2010-05-04', '2010-05-04',1),
 (15,'CPU_SPEED','Compute-Month','100MHz',0.00,'2010-05-04', '2010-05-04',1),
 (16,'vCPU','Compute-Month','1VCPU',0.00,'2010-05-04', '2010-05-04',1),
 (17,'MEMORY','Compute-Month','1MB',0.00,'2010-05-04', '2010-05-04',1),
 (21,'VM_DISK_IO_READ','GB','1',0.00,'2010-05-04', '2010-05-04',1),
 (22,'VM_DISK_IO_WRITE','GB','1',0.00,'2010-05-04', '2010-05-04',1),
 (23,'VM_DISK_BYTES_READ','GB','1',0.00,'2010-05-04', '2010-05-04',1),
 (24,'VM_DISK_BYTES_WRITE','GB','1',0.00,'2010-05-04', '2010-05-04',1),
 (25,'VM_SNAPSHOT','GB-Month','',0.00,'2010-05-04', '2010-05-04',1);
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
INSERT IGNORE INTO `cloud_usage`.`quota_email_templates` (`template_name`, `template_subject`, `template_body`) VALUES
 ('QUOTA_LOW', 'Quota Usage Threshold crossed by your account ${accountName}', 'Your account ${accountName} in the domain ${domainName} has reached quota usage threshold, your current quota balance is ${quotaBalance}.'),
 ('QUOTA_EMPTY', 'Quota Exhausted, account ${accountName} has no quota left.', 'Your account ${accountName} in the domain ${domainName} has exhausted allocated quota, please contact the administrator.'),
 ('QUOTA_UNLOCK_ACCOUNT', 'Quota credits added, account ${accountName} is unlocked now, if it was locked', 'Your account ${accountName} in the domain ${domainName} has enough quota credits now with the current balance of ${quotaBalance}.'),
 ('QUOTA_STATEMENT', 'Quota Statement for your account ${accountName}', 'Monthly quota statement of your account ${accountName} in the domain ${domainName}:<br>Balance = ${quotaBalance}<br>Total Usage = ${quotaUsage}.');
UNLOCK TABLES;


DROP VIEW IF EXISTS `cloud`.`domain_router_view`;
CREATE VIEW `cloud`.`domain_router_view` AS
    select
        vm_instance.id id,
        vm_instance.name name,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        projects.id project_id,
        projects.uuid project_uuid,
        projects.name project_name,
        vm_instance.uuid uuid,
        vm_instance.created created,
        vm_instance.state state,
        vm_instance.removed removed,
        vm_instance.pod_id pod_id,
        vm_instance.instance_name instance_name,
        host_pod_ref.uuid pod_uuid,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        data_center.networktype data_center_type,
        data_center.dns1 dns1,
        data_center.dns2 dns2,
        data_center.ip6_dns1 ip6_dns1,
        data_center.ip6_dns2 ip6_dns2,
        host.id host_id,
        host.uuid host_uuid,
        host.name host_name,
        host.hypervisor_type,
        host.cluster_id cluster_id,
        vm_template.id template_id,
        vm_template.uuid template_uuid,
        service_offering.id service_offering_id,
        disk_offering.uuid service_offering_uuid,
        disk_offering.name service_offering_name,
        nics.id nic_id,
        nics.uuid nic_uuid,
        nics.network_id network_id,
        nics.ip4_address ip_address,
        nics.ip6_address ip6_address,
        nics.ip6_gateway ip6_gateway,
        nics.ip6_cidr ip6_cidr,
        nics.default_nic is_default_nic,
        nics.gateway gateway,
        nics.netmask netmask,
        nics.mac_address mac_address,
        nics.broadcast_uri broadcast_uri,
        nics.isolation_uri isolation_uri,
        vpc.id vpc_id,
        vpc.uuid vpc_uuid,
        vpc.name vpc_name,
        networks.uuid network_uuid,
        networks.name network_name,
        networks.network_domain network_domain,
        networks.traffic_type traffic_type,
        networks.guest_type guest_type,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id,
        domain_router.template_version template_version,
        domain_router.scripts_version scripts_version,
        domain_router.is_redundant_router is_redundant_router,
        domain_router.redundant_state redundant_state,
        domain_router.stop_pending stop_pending,
        domain_router.role role
    from
        `cloud`.`domain_router`
            inner join
        `cloud`.`vm_instance` ON vm_instance.id = domain_router.id
            inner join
        `cloud`.`account` ON vm_instance.account_id = account.id
            inner join
        `cloud`.`domain` ON vm_instance.domain_id = domain.id
            left join
        `cloud`.`host_pod_ref` ON vm_instance.pod_id = host_pod_ref.id
            left join
        `cloud`.`projects` ON projects.project_account_id = account.id
            left join
        `cloud`.`data_center` ON vm_instance.data_center_id = data_center.id
            left join
        `cloud`.`host` ON vm_instance.host_id = host.id
            left join
        `cloud`.`vm_template` ON vm_instance.vm_template_id = vm_template.id
            left join
        `cloud`.`service_offering` ON vm_instance.service_offering_id = service_offering.id
            left join
        `cloud`.`disk_offering` ON vm_instance.service_offering_id = disk_offering.id
            left join
        `cloud`.`nics` ON vm_instance.id = nics.instance_id and nics.removed is null
            left join
        `cloud`.`networks` ON nics.network_id = networks.id
            left join
        `cloud`.`vpc` ON domain_router.vpc_id = vpc.id and vpc.removed is null
            left join
        `cloud`.`async_job` ON async_job.instance_id = vm_instance.id
            and async_job.instance_type = 'DomainRouter'
            and async_job.job_status = 0;

INSERT IGNORE INTO `cloud`.`hypervisor_capabilities` values (25,UUID(),'VMware','6.0',128,0,13,32,1,1);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '5.5', 'rhel7_64Guest', 245, utc_timestamp(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'VMware', '6.0', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='VMware' AND hypervisor_version='5.5' AND (guest_os_id NOT IN (1,2,3,4,62,63,64,65,156,157,221,222) AND guest_os_id NOT BETWEEN 121 AND 130);
