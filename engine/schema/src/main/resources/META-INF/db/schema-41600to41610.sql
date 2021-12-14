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
-- Schema upgrade from 4.16.0.0 to 4.16.1.0
--;

-- Add missing primary keys on tables
ALTER TABLE `cloud`.`op_user_stats_log` ADD COLUMN `id` BIGINT(20) NOT NULL AUTO_INCREMENT FIRST, ADD PRIMARY KEY (`id`);

ALTER TABLE `cloud_usage`.`usage_ip_address`
    DROP INDEX `id`,
    CHANGE COLUMN `id` `ip_id` BIGINT(20) UNSIGNED NOT NULL,
    ADD COLUMN `id` BIGINT(20) NOT NULL AUTO_INCREMENT FIRST,
    ADD PRIMARY KEY (`id`),
    ADD UNIQUE INDEX `id` (`ip_id` ASC, `assigned` ASC);

ALTER TABLE `cloud_usage`.`usage_load_balancer_policy`
    CHANGE COLUMN `id` `lb_id` BIGINT(20) UNSIGNED NOT NULL,
    ADD COLUMN `id` BIGINT(20) NOT NULL AUTO_INCREMENT FIRST,
    ADD PRIMARY KEY (`id`);

ALTER TABLE `cloud_usage`.`usage_network_offering`
    ADD COLUMN `id` BIGINT(20) NOT NULL AUTO_INCREMENT FIRST, ADD PRIMARY KEY (`id`);
