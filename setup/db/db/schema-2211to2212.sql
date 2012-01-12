# Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
#     
# This software is licensed under the GNU General Public License v3 or later.
# 
# It is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or any later version.
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
# 
--;
-- Schema upgrade from 2.2.11 to 2.2.12;
--;

ALTER TABLE `cloud`.`vm_template` ADD COLUMN `template_tag` varchar(255) COMMENT 'template tag';

UPDATE vm_instance SET state='Error' WHERE state='Creating' AND removed IS null;

INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'json.content.type', 'text/javascript', 'Http response content type for .js files (default is text/javascript)');
INSERT IGNORE INTO configuration VALUES ('Premium', 'DEFAULT', 'management-server', 'usage.aggregation.timezone', 'GMT', 'The timezone to use for usage stats aggregation');

ALTER IGNORE TABLE `cloud`.`user_vm_details` DROP FOREIGN KEY `fk_user_vm_details__vm_id`;
ALTER TABLE `cloud`.`user_vm_details` ADD CONSTRAINT `fk_user_vm_details__vm_id` FOREIGN KEY `fk_user_vm_details__vm_id`(`vm_id`) REFERENCES `vm_instance`(`id`) ON DELETE CASCADE;


ALTER TABLE `cloud`.`domain_router` ADD COLUMN `is_priority_bumpup` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'if the priority has been bumped up';
ALTER TABLE `cloud`.`domain_router` ADD COLUMN `stop_pending` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'if this router would be stopped after we can connect to it';

DELETE FROM `cloud`.`configuration` where name='vmware.guest.nic.device.type';

INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'agent.lb.enabled', 'true', 'If agent load balancing enabled in cluster setup');

update `cloud`.`vm_template` set hypervisor_type='XenServer' where unique_name='xs-tools.iso';

ALTER TABLE `cloud`.`host` MODIFY COLUMN  `storage_ip_address` char(40);

ALTER TABLE `cloud`.`resource_count` ADD UNIQUE `i_resource_count__type_accountId`(`type`, `account_id`);
ALTER TABLE `cloud`.`resource_count` ADD UNIQUE `i_resource_count__type_domaintId`(`type`, `domain_id`);

UPDATE configuration set description='Load Balancer(haproxy) stats visibilty, the value can be one of the following six parameters : global,guest-network,link-local,disabled,all,default' WHERE name='network.loadbalancer.haproxy.stats.visibility' ;

UPDATE nics SET strategy="Managed" WHERE (ip4_address, network_id) IN (SELECT public_ip_address, source_network_id FROM user_ip_address WHERE source_nat=1) AND vm_type="DomainRouter";

UPDATE nics SET strategy="Created" WHERE strategy="Managed" AND vm_type!="DomainRouter";

CREATE TABLE IF NOT EXISTS `cloud`.`inline_load_balancer_nic_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `load_balancer_id` bigint unsigned NOT NULL,
  `public_ip_address` char(40) NOT NULL,
  `nic_id` bigint unsigned NULL COMMENT 'nic id',
  PRIMARY KEY (`id`),
  UNIQUE KEY (`nic_id`),
  CONSTRAINT `fk_inline_load_balancer_nic_map__load_balancer_id` FOREIGN KEY(`load_balancer_id`) REFERENCES `load_balancing_rules`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_inline_load_balancer_nic_map__nic_id` FOREIGN KEY(`nic_id`) REFERENCES `nics`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

