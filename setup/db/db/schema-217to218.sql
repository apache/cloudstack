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


ALTER TABLE `cloud`.`account_vlan_map` modify `account_id` bigint unsigned default null;


ALTER TABLE `cloud`.`account_vlan_map` ADD COLUMN `domain_id` bigint unsigned COMMENT 'domain id. foreign key to domain table';
ALTER TABLE `cloud`.`account_vlan_map` ADD CONSTRAINT `fk_account_vlan_map__domain_id` FOREIGN KEY `fk_account_vlan_map__domain_id` (`domain_id`) REFERENCES `domain` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`account_vlan_map` ADD INDEX `i_account_vlan_map__domain_id`(`domain_id`);

CREATE TABLE `cloud`.`version` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id',
  `version` char(40) NOT NULL UNIQUE COMMENT 'version',
  `updated` datetime NOT NULL COMMENT 'Date this version table was updated',
  `step` char(32) NOT NULL COMMENT 'Step in the upgrade to this version',
  PRIMARY KEY (`id`),
  INDEX `i_version__version`(`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
