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
-- Schema upgrade from 2.1 to 2.2;
--;
DROP TABLE IF EXISTS `cloud_usage`.`usage_port_forwarding`;
DROP TABLE IF EXISTS `cloud_usage`.`usage_network_offering`;
DROP TABLE IF EXISTS `cloud_usage`.`usage_event`;

ALTER TABLE `cloud_usage`.`cloud_usage` modify `raw_usage` DOUBLE UNSIGNED NOT NULL;
ALTER TABLE `cloud_usage`.`cloud_usage` ADD COLUMN `type` varchar(32);

ALTER TABLE `cloud_usage`.`usage_network` ADD COLUMN `host_id` bigint unsigned NOT NULL default 0;
ALTER TABLE `cloud_usage`.`usage_network` ADD COLUMN `host_type` varchar(32) default 'DomainRouter';

ALTER TABLE `cloud_usage`.`usage_network` drop PRIMARY KEY;
ALTER TABLE `cloud_usage`.`usage_network` add PRIMARY KEY (`account_id`, `zone_id`, `host_id`, `event_time_millis`);

ALTER TABLE `cloud_usage`.`usage_ip_address` ADD COLUMN `id` bigint unsigned default 0;
ALTER TABLE `cloud_usage`.`usage_ip_address` ADD COLUMN `is_source_nat` smallint(1) NOT NULL default 0;

ALTER TABLE `cloud_usage`.`account` ADD COLUMN `network_domain` varchar(100) COMMENT 'Network domain name of the Vms of the account';

ALTER TABLE `cloud_usage`.`user_statistics` ADD COLUMN `public_ip_address` varchar(15);
ALTER TABLE `cloud_usage`.`user_statistics` ADD COLUMN `device_id` bigint unsigned NOT NULL default 0;
ALTER TABLE `cloud_usage`.`user_statistics` ADD COLUMN `device_type` varchar(32) NOT NULL default 'DomainRouter';

CREATE TABLE  `cloud_usage`.`usage_event` (
  `id` bigint unsigned NOT NULL auto_increment,
  `type` varchar(32) NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `created` datetime NOT NULL,
  `zone_id` bigint unsigned NOT NULL,
  `resource_id` bigint unsigned,
  `resource_name` varchar(255),
  `offering_id` bigint unsigned,
  `template_id` bigint unsigned,
  `size` bigint unsigned,  
  `processed` tinyint NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud_usage`.`usage_port_forwarding` (
  `id` bigint unsigned NOT NULL,
  `zone_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `domain_id` bigint unsigned NOT NULL,
  `created` DATETIME NOT NULL,
  `deleted` DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud_usage`.`usage_network_offering` (
  `zone_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `domain_id` bigint unsigned NOT NULL,
  `vm_instance_id` bigint unsigned NOT NULL,
  `network_offering_id` bigint unsigned NOT NULL,
  `is_default` smallint(1) NOT NULL,
  `created` DATETIME NOT NULL,
  `deleted` DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

update `cloud_usage`.`usage_volume` set size = (size * 1048576);



