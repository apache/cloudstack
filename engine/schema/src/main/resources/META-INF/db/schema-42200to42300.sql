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
-- Schema upgrade from 4.22.0.0 to 4.23.0.0
--;

CREATE TABLE IF NOT EXISTS `cloud`.`service_offering_category` (
 `id` bigint unsigned NOT NULL auto_increment,
 `name` varchar(255) NOT NULL,
 `uuid` varchar(40),
 `sort_key` int NOT NULL DEFAULT 0,
 PRIMARY KEY  (`id`),
 CONSTRAINT `uc_service_offering_category__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;


ALTER TABLE `cloud`.`service_offering` ADD COLUMN `category_id` bigint unsigned NOT NULL DEFAULT 1;
ALTER TABLE `cloud`.`service_offering` ADD CONSTRAINT `fk_service_offering__category_id` FOREIGN KEY (`category_id`) REFERENCES `cloud`.`service_offering_category` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE;
INSERT INTO `cloud`.`service_offering_category` (id, name, uuid) VALUES (1, 'Default', UUID());