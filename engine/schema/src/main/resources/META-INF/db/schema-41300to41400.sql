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
-- Schema upgrade from 4.13.0.0 to 4.14.0.0
--;

-- KVM: enable storage data motion on KVM hypervisor_capabilities
UPDATE `cloud`.`hypervisor_capabilities` SET `storage_motion_supported` = 1 WHERE `hypervisor_capabilities`.`hypervisor_type` = 'KVM';

-- Use 'Other Linux 64-bit' as guest os for the default systemvmtemplate for XenServer
UPDATE `cloud`.`vm_template` SET guest_os_id=99 WHERE id=1;

-- #3659 Fix typo: the past tense of shutdown is shutdown, not shutdowned
UPDATE `cloud`.`vm_instance` SET state='Shutdown' WHERE state='Shutdowned';

-- Fix OS category for some Ubuntu and RedHat OS-es
UPDATE `cloud`.`guest_os` SET `category_id`='10' WHERE `id`=277 AND display_name="Ubuntu 17.04";
UPDATE `cloud`.`guest_os` SET `category_id`='10' WHERE `id`=278 AND display_name="Ubuntu 17.10";
UPDATE `cloud`.`guest_os` SET `category_id`='10' WHERE `id`=279 AND display_name="Ubuntu 18.04 LTS";
UPDATE `cloud`.`guest_os` SET `category_id`='10' WHERE `id`=280 AND display_name="Ubuntu 18.10";
UPDATE `cloud`.`guest_os` SET `category_id`='10' WHERE `id`=281 AND display_name="Ubuntu 19.04";
UPDATE `cloud`.`guest_os` SET `category_id`='4' WHERE `id`=282 AND display_name="Red Hat Enterprise Linux 7.3";
UPDATE `cloud`.`guest_os` SET `category_id`='4' WHERE `id`=283 AND display_name="Red Hat Enterprise Linux 7.4";
UPDATE `cloud`.`guest_os` SET `category_id`='4' WHERE `id`=284 AND display_name="Red Hat Enterprise Linux 7.5";
UPDATE `cloud`.`guest_os` SET `category_id`='4' WHERE `id`=285 AND display_name="Red Hat Enterprise Linux 7.6";
UPDATE `cloud`.`guest_os` SET `category_id`='4' WHERE `id`=286 AND display_name="Red Hat Enterprise Linux 8.0";

-- Create table for router health checks. We only save last check result for each.
CREATE TABLE  `cloud`.`router_health_check` (
  `id` bigint unsigned NOT NULL auto_increment,
  `router_id` bigint unsigned NOT NULL COMMENT 'router id',
  `check_name` varchar(255) NOT NULL COMMENT 'name of the health check',
  `check_type` varchar(255) NOT NULL COMMENT 'type of the health check',
  `last_update` DATETIME NULL COMMENT 'last check update time',
  `check_result` boolean NOT NULL COMMENT 'check executions success or failure',
  `check_details` BLOB NULL COMMENT 'check result detailed message',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_router_health_checks__router_id` FOREIGN KEY (`router_id`) REFERENCES `domain_router`(`id`) ON DELETE CASCADE,
  UNIQUE `i_router_health_checks__router_id__check_name__check_type`(`router_id`, `check_name`, `check_type`),
  INDEX `i_router_health_checks__router_id`(`router_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
