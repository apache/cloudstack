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


#Schema cleanup from 3.0.1 to 3.0.2;


DROP TABLE IF EXISTS `cloud`.`network_tags`;

ALTER TABLE `cloud`.`nics` MODIFY `vm_type` varchar(32) DEFAULT NULL;
ALTER TABLE `cloud`.`service_offering` MODIFY `default_use` tinyint(1) UNSIGNED NOT NULL DEFAULT '0';
ALTER TABLE `cloud`.`snapshots` MODIFY `hypervisor_type` varchar(32) NOT NULL;
ALTER TABLE `cloud`.`snapshots` MODIFY `version` varchar(32) DEFAULT NULL;
ALTER TABLE `cloud`.`volumes` MODIFY `state` varchar(32) DEFAULT NULL;


ALTER TABLE `cloud_usage`.`usage_ip_address` MODIFY `id` bigint(20) UNSIGNED NOT NULL;
ALTER TABLE `cloud_usage`.`usage_ip_address` MODIFY  `is_source_nat` smallint(1) NOT NULL;
ALTER TABLE `cloud_usage`.`usage_network` MODIFY `host_id` bigint(20) UNSIGNED NOT NULL;
ALTER TABLE `cloud_usage`.`usage_network` MODIFY `host_type` varchar(32) DEFAULT NULL;
ALTER TABLE `cloud_usage`.`user_statistics` MODIFY `device_id` bigint(20) UNSIGNED NOT NULL;
