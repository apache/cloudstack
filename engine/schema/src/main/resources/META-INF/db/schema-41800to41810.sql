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
-- Schema upgrade from 4.18.0.0 to 4.18.1.0
--;

-- IP quarantine PR#7378
CREATE TABLE IF NOT EXISTS `cloud`.`quarantined_ips` (
  `id` bigint(20) unsigned NOT NULL auto_increment,
  `uuid` varchar(255) UNIQUE,
  `public_ip_address_id` bigint(20) unsigned NOT NULL COMMENT 'ID of the quarantined public IP address, foreign key to `user_ip_address` table',
  `previous_owner_id` bigint(20) unsigned NOT NULL COMMENT 'ID of the previous owner of the public IP address, foreign key to `user_ip_address` table',
  `created` datetime NOT NULL,
  `removed` datetime DEFAULT NULL,
  `end_date` datetime NOT NULL,
  `removal_reason` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_quarantined_ips__public_ip_address_id` FOREIGN KEY(`public_ip_address_id`) REFERENCES `cloud`.`user_ip_address`(`id`),
  CONSTRAINT `fk_quarantined_ips__previous_owner_id` FOREIGN KEY(`previous_owner_id`) REFERENCES `cloud`.`account`(`id`)
);

-- create_public_parameter_on_roles. #6960
ALTER TABLE `cloud`.`roles` ADD COLUMN `public_role` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Indicates whether the role will be visible to all users (public) or only to root admins (private). If this parameter is not specified during the creation of the role its value will be defaulted to true (public).';
