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

SET foreign_key_checks = 0;
DROP TABLE disk_offering;
DROP TABLE service_offering;

RENAME TABLE disk_offering_21 TO disk_offering, service_offering_21 TO service_offering;
ALTER TABLE `cloud`.`service_offering` ADD CONSTRAINT `fk_service_offering__id` FOREIGN KEY `fk_service_offering__id`(`id`) REFERENCES `disk_offering`(`id`) ON DELETE CASCADE;

ALTER TABLE `cloud`.`volumes` MODIFY COLUMN `disk_offering_id` bigint unsigned NOT NULL; -- add NOT NULL constraint
ALTER TABLE `cloud`.`host_pod_ref` MODIFY COLUMN `gateway` varchar(255) NOT NULL;  -- add NOT NULL constrait

ALTER TABLE `cloud`.`console_proxy` MODIFY COLUMN `guest_mac_address` varchar(17) NOT NULL UNIQUE; -- add NOT NULL UNIQUE constraint
ALTER TABLE `cloud`.`secondary_storage_vm` MODIFY COLUMN `guest_mac_address` varchar(17)NOT NULL UNIQUE; -- add NOT NULL UNIQUE constraint

update disk_offering set removed=NOW() where type='Service' and unique_name like 'Cloud.com-%';

ALTER TABLE `cloud`.`user_vm` ADD CONSTRAINT `fk_user_vm__service_offering_id` FOREIGN KEY `fk_user_vm__service_offering_id` (`service_offering_id`) REFERENCES `service_offering` (`id`);
