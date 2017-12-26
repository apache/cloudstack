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
-- Schema upgrade from 2.2.1 to 2.2.2;
--;
ALTER TABLE `cloud_usage`.`cloud_usage` ADD COLUMN `network_id` bigint unsigned;

ALTER TABLE `cloud_usage`.`usage_network` ADD COLUMN `network_id` bigint unsigned;

ALTER TABLE `cloud_usage`.`user_statistics` ADD COLUMN `network_id` bigint unsigned;
ALTER TABLE `cloud_usage`.`user_statistics` ADD UNIQUE KEY (`account_id`, `data_center_id`, `public_ip_address`, `device_id`, `device_type`);
