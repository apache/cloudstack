--;
-- Schema upgrade from 2.2.5 to 2.2.6;
--;

ALTER TABLE `cloud`.`mshost` ADD COLUMN `runid` bigint NOT NULL DEFAULT 0 COMMENT 'run id, combined with msid to form a cluster session';
ALTER TABLE `cloud`.`mshost` ADD COLUMN `state` varchar(10) NOT NULL default 'Down';
ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `limit_cpu_use` tinyint(1) NOT NULL DEFAULT 0 ;
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `limit_cpu_use` tinyint(1) NOT NULL DEFAULT 0 ;
ALTER TABLE `cloud`.`storage_pool` MODIFY `host_address` varchar(255) NOT NULL;

DROP TABLE IF EXISTS `cloud`.`certificate`;
CREATE TABLE `cloud`.`keystore` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(64) NOT NULL COMMENT 'unique name for the certifiation',
  `certificate` text NOT NULL COMMENT 'the actual certificate being stored in the db',
  `key` text NOT NULL COMMENT 'private key associated wih the certificate',
  `domain_suffix` varchar(256) NOT NULL COMMENT 'DNS domain suffix associated with the certificate',
  PRIMARY KEY (`id`),
  UNIQUE(name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`cmd_exec_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `host_id` bigint unsigned NOT NULL COMMENT 'host id of the system VM agent that command is sent to',
  `instance_id` bigint unsigned NOT NULL COMMENT 'instance id of the system VM that command is executed on',
  `command_name` varchar(255) NOT NULL COMMENT 'command name',
  `weight` integer NOT NULL DEFAULT 1 COMMENT 'command weight in consideration of the load factor added to host that is executing the command',
  `created` datetime NOT NULL COMMENT 'date created',
  PRIMARY KEY (`id`),
  INDEX `i_cmd_exec_log__host_id`(`host_id`),
  INDEX `i_cmd_exec_log__instance_id`(`instance_id`),
  CONSTRAINT `fk_cmd_exec_log_ref__inst_id` FOREIGN KEY (`instance_id`) REFERENCES `vm_instance`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`network_tags` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `network_id` bigint unsigned NOT NULL COMMENT 'id of the network',
  `tag` varchar(255) NOT NULL COMMENT 'tag',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_network_tags__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`),
  UNIQUE KEY(`network_id`, `tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  IF NOT EXISTS `cloud`.`firewall_rules_cidrs` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `firewall_rule_id` bigint(20) unsigned NOT NULL COMMENT 'firewall rule id',
  `source_cidr` varchar(18) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_firewall_cidrs_firewall_rules` (`firewall_rule_id`),
  CONSTRAINT `fk_firewall_cidrs_firewall_rules` FOREIGN KEY (`firewall_rule_id`) REFERENCES `firewall_rules` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


ALTER TABLE `cloud`.`secondary_storage_vm` ADD COLUMN `role` varchar(64) NOT NULL DEFAULT 'templateProcessor';

INSERT INTO `cloud`.`configuration` (category, instance, component, name, value, description) VALUES ('Network', 'DEFAULT', 'management-server', 'vm.network.throttling.rate', 200, 'Default data transfer rate in megabits per second allowed in user vm\'s default network.');

DELETE FROM `cloud`.`configuration` where name='guest.ip.network';
DELETE FROM `cloud`.`configuration` where name='guest.netmask';

ALTER TABLE `cloud`.`host_pod_ref` ADD COLUMN `removed` datetime COMMENT 'date removed if not null';
ALTER TABLE `cloud`.`host_pod_ref` MODIFY `name` varchar(255);

ALTER TABLE `cloud`.`security_group` DROP COLUMN `account_name`;

ALTER TABLE `cloud`.`security_ingress_rule` DROP COLUMN `allowed_security_group`;
ALTER TABLE `cloud`.`security_ingress_rule` DROP COLUMN `allowed_sec_grp_acct`;

ALTER TABLE `cloud`.`data_center` ADD COLUMN `zone_token` varchar(255);
ALTER TABLE `cloud`.`data_center` ADD INDEX `i_data_center__zone_token`(`zone_token`);

ALTER TABLE `cloud`.`vm_template` ADD COLUMN `source_template_id` bigint unsigned COMMENT 'Id of the original template, if this template is created from snapshot';

ALTER TABLE `cloud`.`op_dc_link_local_ip_address_alloc` ADD INDEX `i_op_dc_link_local_ip_address_alloc__pod_id`(`pod_id`);
ALTER TABLE `cloud`.`op_dc_link_local_ip_address_alloc` ADD INDEX `i_op_dc_link_local_ip_address_alloc__data_center_id`(`data_center_id`);
ALTER TABLE `cloud`.`op_dc_link_local_ip_address_alloc` ADD INDEX `i_op_dc_link_local_ip_address_alloc__nic_id_reservation_id`(`nic_id`,`reservation_id`);

INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (139, 7, 'Other PV (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (140, 7, 'Other PV (64-bit)');

INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ('XenServer', 'Other PV (32-bit)', 139);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ('XenServer', 'Other PV (64-bit)', 140);

ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `shared_source_nat_service` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'true if the network offering provides the shared source nat service';

CREATE TABLE `cloud`.`op_host_transfer` (
  `id` bigint unsigned UNIQUE NOT NULL COMMENT 'Id of the host',
  `initial_mgmt_server_id` bigint unsigned COMMENT 'management server the host is transfered from',
  `future_mgmt_server_id` bigint unsigned COMMENT 'management server the host is transfered to',
  `state` varchar(32) NOT NULL COMMENT 'the transfer state of the host',
  `created` datetime NOT NULL COMMENT 'date created',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_op_host_transfer__id` FOREIGN KEY `fk_op_host_transfer__id` (`id`) REFERENCES `host` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_op_host_transfer__initial_mgmt_server_id` FOREIGN KEY `fk_op_host_transfer__initial_mgmt_server_id`(`initial_mgmt_server_id`) REFERENCES `mshost`(`msid`),
  CONSTRAINT `fk_op_host_transfer__future_mgmt_server_id` FOREIGN KEY `fk_op_host_transfer__future_mgmt_server_id`(`future_mgmt_server_id`) REFERENCES `mshost`(`msid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


ALTER TABLE `cloud`.`snapshots` ADD COLUMN `swift_id` bigint unsigned;
ALTER TABLE `cloud`.`snapshots` ADD COLUMN `swift_name` varchar(255);
ALTER TABLE `cloud`.`snapshots` ADD COLUMN `sechost_id` bigint unsigned;


CREATE TABLE `cloud`.`swift` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `hostname` varchar(255),
  `account` varchar(255) COMMENT ' account in swift',
  `username` varchar(255) COMMENT ' username in swift',
  `token` varchar(255) COMMENT 'token for this user',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `vm_type` varchar(32) NOT NULL;
UPDATE vm_instance set vm_type=type;

ALTER TABLE `cloud`.`networks` ADD COLUMN `is_domain_specific` int(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 if network is domain specific, 0 false otherwise';
INSERT INTO configuration (`category`, `instance`, `component`, `name`, `value`, `description`) VALUES ('Advanced', 'DEFAULT', 'NetworkManager', 'allow.subdomain.network.access', 'true', 'Allow subdomains to use networks dedicated to their parent domain(s)'); 
