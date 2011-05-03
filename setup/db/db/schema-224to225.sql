--;
-- Schema upgrade from 2.2.4 to 2.2.5;
--;

ALTER TABLE `cloud`.`mshost` ADD COLUMN `runid` bigint NOT NULL DEFAULT 0 COMMENT 'run id, combined with msid to form a cluster session';
ALTER TABLE `cloud`.`mshost` ADD COLUMN `state` varchar(10) NOT NULL default 'Down';
ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `limit_cpu_use` tinyint(1) NOT NULL DEFAULT 0 ;
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `limit_cpu_use` tinyint(1) NOT NULL DEFAULT 0 ;

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
