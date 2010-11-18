--
-- Schema upgrade from 2.1 to 2.2
--


CREATE TABLE `cloud`.`instance_group` (
  `id` bigint unsigned NOT NULL UNIQUE auto_increment,
  `account_id` bigint unsigned NOT NULL COMMENT 'owner.  foreign key to account table',
  `name` varchar(255) NOT NULL,
  `removed` datetime COMMENT 'date the group was removed',
  `created` datetime COMMENT 'date the group was created',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`instance_group_vm_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `group_id` bigint unsigned NOT NULL,
  `instance_id` bigint unsigned NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`certificate` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `certificate` text COMMENT 'the actual custom certificate being stored in the db',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`remote_access_vpn` (
  `id` bigint unsigned NOT NULL auto_increment,
  `account_id` bigint unsigned NOT NULL,
  `zone_id` bigint unsigned NOT NULL,
  `vpn_server_addr` varchar(15) UNIQUE NOT NULL,
  `local_ip` varchar(15) NOT NULL,
  `ip_range` varchar(32) NOT NULL,
  `ipsec_psk` varchar(256) NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`vpn_users` (
  `id` bigint unsigned NOT NULL auto_increment,
  `account_id` bigint unsigned NOT NULL,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`data_center` MODIFY COLUMN `guest_network_cidr` varchar(18); -- modify column width to 18 from 15

ALTER TABLE `cloud`.`resource_count` ADD COLUMN `domain_id` bigint unsigned; -- add the new column
ALTER TABLE `cloud`.`resource_count` MODIFY COLUMN `account_id` bigint unsigned; -- modify the column to allow NULL values
ALTER TABLE `cloud`.`storage_pool` add COLUMN STATUS varchar(32) not null; -- new status column for maintenance mode support for primary storage
ALTER TABLE `cloud`.`volumes` ADD COLUMN `source_id` bigint unsigned;  -- id for the source
ALTER TABLE `cloud`.`volumes` ADD COLUMN `source_type` varchar(32); --source from which the volume is created i.e. snapshot, diskoffering, template, blank
ALTER TABLE `cloud`.`volumes` ADD COLUMN 'attached' datetime; --date and time the volume was attached
ALTER TABLE `cloud`.`disk_offering` ADD COLUMN `customized` tinyint(1) unsigned NOT NULL DEFAULT 0;-- 0 implies not customized by default 
ALTER TABLE `cloud`.`user_ip_address` ADD COLUMN `one_to_one_nat` int(1) unsigned NOT NULL default '0'; -- new column for NAT ip



