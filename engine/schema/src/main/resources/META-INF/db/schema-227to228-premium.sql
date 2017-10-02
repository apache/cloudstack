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
-- Schema upgrade from 2.2.7 to 2.2.8;
--;
CREATE TABLE IF NOT EXISTS `cloud`.`netapp_pool` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id',
  `name` varchar(255) NOT NULL UNIQUE COMMENT 'name for the pool',
  `algorithm` varchar(255) NOT NULL COMMENT 'algorithm',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE IF NOT EXISTS `cloud`.`netapp_volume` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id',
  `ip_address` varchar(255) NOT NULL COMMENT 'ip address/fqdn of the volume',
  `pool_id` bigint unsigned NOT NULL COMMENT 'id for the pool',
  `pool_name` varchar(255) NOT NULL COMMENT 'name for the pool',
  `aggregate_name` varchar(255) NOT NULL COMMENT 'name for the aggregate',
  `volume_name` varchar(255) NOT NULL COMMENT 'name for the volume',
  `volume_size` varchar(255) NOT NULL COMMENT 'volume size',
  `snapshot_policy` varchar(255) NOT NULL COMMENT 'snapshot policy',
  `snapshot_reservation` int NOT NULL COMMENT 'snapshot reservation',  
  `username` varchar(255) NOT NULL COMMENT 'username',  
  `password` varchar(200) COMMENT 'password',
  `round_robin_marker` int COMMENT 'This marks the volume to be picked up for lun creation, RR fashion',
  PRIMARY KEY (`ip_address`,`aggregate_name`,`volume_name`),
  CONSTRAINT `fk_netapp_volume__pool_id` FOREIGN KEY `fk_netapp_volume__pool_id` (`pool_id`) REFERENCES `netapp_pool` (`id`) ON DELETE CASCADE,
  INDEX `i_netapp_volume__pool_id`(`pool_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE IF NOT EXISTS `cloud`.`netapp_lun` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id',
  `lun_name` varchar(255) NOT NULL COMMENT 'lun name',
  `target_iqn` varchar(255) NOT NULL COMMENT 'target iqn',
  `path` varchar(255) NOT NULL COMMENT 'lun path',
  `size` bigint NOT NULL COMMENT 'lun size',
  `volume_id` bigint unsigned NOT NULL COMMENT 'parent volume id',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_netapp_lun__volume_id` FOREIGN KEY `fk_netapp_lun__volume_id` (`volume_id`) REFERENCES `netapp_volume` (`id`) ON DELETE CASCADE,
  INDEX `i_netapp_lun__volume_id`(`volume_id`),
  INDEX `i_netapp_lun__lun_name`(`lun_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--;
-- Cleanup usage records for bug # 10727;
--;


create table `cloud_usage`.`temp_usage` (  `vol_id` bigint unsigned, `created` DATETIME);

insert into `cloud_usage`.`temp_usage` (vol_id, created) select id, max(created) from `cloud_usage`.`usage_volume` where deleted is null group by id having count(id) > 1;

delete `cloud_usage`.`usage_volume` from `cloud_usage`.`usage_volume` inner join `cloud_usage`.`temp_usage` where `cloud_usage`.`usage_volume`.created = `cloud_usage`.`temp_usage`.created and `cloud_usage`.`usage_volume`.id = `cloud_usage`.`temp_usage`.vol_id and `cloud_usage`.`usage_volume`.deleted is null;

drop table `cloud_usage`.`temp_usage`;

update `cloud_usage`.`cloud_usage` set raw_usage = (raw_usage % 24) where usage_type =6 and raw_usage > 24 and (raw_usage % 24) <> 0;
update `cloud_usage`.`cloud_usage` set raw_usage = 24 where usage_type =6 and raw_usage > 24 and (raw_usage % 24) = 0;

