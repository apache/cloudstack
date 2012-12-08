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

alter table vm_template add image_data_store_id bigint unsigned;
alter table storage_pool add storage_provider_id bigint unsigned; 
alter table storage_pool add configurator_key varchar(255); 
alter table storage_pool modify id bigint unsigned AUTO_INCREMENT UNIQUE NOT NULL;
alter table volumes add disk_type varchar(255);
alter table volumes drop foreign key `fk_volumes__account_id`;
CREATE TABLE `cloud`.`primary_data_store_provider` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(255) NOT NULL COMMENT 'name of primary data store provider',
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`image_data_store_provider` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(255) NOT NULL COMMENT 'name of data store provider',
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`image_data_store` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(255) NOT NULL COMMENT 'name of data store',
  `image_provider_id` bigint unsigned NOT NULL COMMENT 'id of image_data_store_provider',
  PRIMARY KEY(`id`),
  CONSTRAINT `fk_tags__image_data_store_provider_id` FOREIGN KEY(`image_provider_id`) REFERENCES `image_data_store_provider`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
