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

use cloud;

alter table vm_template add image_data_store_id bigint unsigned;
alter table vm_template add size bigint unsigned;
alter table vm_template add state varchar(255);
alter table vm_template add update_count bigint unsigned;
alter table vm_template add updated  datetime;
alter table storage_pool add storage_provider_id bigint unsigned; 
alter table storage_pool add scope varchar(255); 
alter table storage_pool modify id bigint unsigned AUTO_INCREMENT UNIQUE NOT NULL;
alter table template_spool_ref add state varchar(255);
alter table template_spool_ref add update_count bigint unsigned;
alter table volumes add disk_type varchar(255);
alter table volumes drop foreign key `fk_volumes__account_id`;
alter table vm_instance add column disk_offering_id bigint unsigned;
alter table vm_instance add column cpu int(10) unsigned;
alter table vm_instance add column ram bigint unsigned;
alter table vm_instance add column owner varchar(255);
alter table vm_instance add column speed int(10) unsigned;
alter table vm_instance add column host_name varchar(255);
alter table vm_instance add column display_name varchar(255);

alter table data_center add column owner varchar(255);
alter table data_center add column created datetime COMMENT 'date created';
alter table data_center add column lastUpdated datetime COMMENT 'last updated';
alter table data_center add column engine_state varchar(32) NOT NULL DEFAULT 'Disabled' COMMENT 'the engine state of the zone';
alter table host_pod_ref add column owner varchar(255);
alter table host_pod_ref add column created datetime COMMENT 'date created';
alter table host_pod_ref add column lastUpdated datetime COMMENT 'last updated';
alter table host_pod_ref add column engine_state varchar(32) NOT NULL DEFAULT 'Disabled' COMMENT 'the engine state of the zone';
alter table host add column owner varchar(255);
alter table host add column lastUpdated datetime COMMENT 'last updated';
alter table host add column engine_state varchar(32) NOT NULL DEFAULT 'Disabled' COMMENT 'the engine state of the zone';


alter table cluster add column owner varchar(255);
alter table cluster add column created datetime COMMENT 'date created';
alter table cluster add column lastUpdated datetime COMMENT 'last updated';
alter table cluster add column engine_state varchar(32) NOT NULL DEFAULT 'Disabled' COMMENT 'the engine state of the zone';
CREATE TABLE  `cloud`.`object_datastore_ref` (
  `id` bigint unsigned NOT NULL auto_increment,
  `datastore_id` bigint unsigned NOT NULL,
  `datastore_role` varchar(255) NOT NULL,
  `object_id` bigint unsigned NOT NULL,
  `object_type` varchar(255) NOT NULL,
  `created` DATETIME NOT NULL,
  `last_updated` DATETIME,
  `job_id` varchar(255),
  `download_pct` int(10) unsigned,
  `download_state` varchar(255),
  `error_str` varchar(255),
  `local_path` varchar(255),
  `install_path` varchar(255),
  `size` bigint unsigned COMMENT 'the size of the template on the pool',
  `state` varchar(255) NOT NULL,
  `update_count` bigint unsigned NOT NULL,
  `updated` DATETIME,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`data_store_provider` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(255) NOT NULL COMMENT 'name of primary data store provider',
  `uuid` varchar(255) NOT NULL COMMENT 'uuid of primary data store provider',
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`image_data_store` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(255) NOT NULL COMMENT 'name of data store',
  `image_provider_id` bigint unsigned NOT NULL COMMENT 'id of image_data_store_provider',
  `protocol` varchar(255) NOT NULL COMMENT 'protocol of data store',
  `data_center_id` bigint unsigned  COMMENT 'datacenter id of data store',
  `scope` varchar(255) COMMENT 'scope of data store',
  `uuid` varchar(255) COMMENT 'uuid of data store',
  PRIMARY KEY(`id`),
  CONSTRAINT `fk_tags__image_data_store_provider_id` FOREIGN KEY(`image_provider_id`) REFERENCES `data_store_provider`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`vm_compute_tags` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `vm_id` bigint unsigned NOT NULL COMMENT 'vm id',
  `compute_tag` varchar(255) NOT NULL COMMENT 'name of tag',
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`vm_root_disk_tags` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `vm_id` bigint unsigned NOT NULL COMMENT 'vm id',
  `root_disk_tag` varchar(255) NOT NULL COMMENT 'name of tag',
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `cloud`.`vm_network_map` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `vm_id` bigint unsigned NOT NULL COMMENT 'vm id',
  `network_id` bigint unsigned NOT NULL COMMENT 'network id',
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `cloud`.`vm_reservation` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(40) NOT NULL COMMENT 'reservation id',
  `vm_id` bigint unsigned NOT NULL COMMENT 'vm id',
  `data_center_id` bigint unsigned NOT NULL COMMENT 'zone id',
  `pod_id` bigint unsigned NOT NULL COMMENT 'pod id',
  `cluster_id` bigint unsigned NOT NULL COMMENT 'cluster id',
  `host_id` bigint unsigned NOT NULL COMMENT 'host id',
  `created` datetime COMMENT 'date created',
  `removed` datetime COMMENT 'date removed if not null',
  CONSTRAINT `uc_vm_reservation__uuid` UNIQUE (`uuid`),
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`volume_reservation` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `vm_reservation_id` bigint unsigned NOT NULL COMMENT 'id of the vm reservation',
  `vm_id` bigint unsigned NOT NULL COMMENT 'vm id',
  `volume_id` bigint unsigned NOT NULL COMMENT 'volume id',
  `pool_id` bigint unsigned NOT NULL COMMENT 'pool assigned to the volume',
  CONSTRAINT `fk_vm_pool_reservation__vm_reservation_id` FOREIGN KEY (`vm_reservation_id`) REFERENCES `vm_reservation`(`id`) ON DELETE CASCADE,
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
