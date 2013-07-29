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
-- Schema cleanup from 3.0.7 to 4.1.0
--;

# Drop the fields introduced as a part of 410-420 upgrade and re-enable it back
ALTER TABLE `cloud`.`network_offerings` DROP COLUMN `eip_associate_public_ip`;
ALTER TABLE `cloud`.`network_offerings` CHANGE COLUMN `eip_associate_public_ip_1` `eip_associate_public_ip` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'true if public IP is associated with user VM creation by default when EIP service is enabled.' AFTER `elastic_ip_service`;

ALTER TABLE `cloud`.`usage_event` DROP COLUMN `virtual_size`;
ALTER TABLE `cloud_usage`.`usage_event` DROP COLUMN `virtual_size`;
ALTER TABLE `cloud_usage`.`usage_storage` DROP COLUMN `virtual_size`;
ALTER TABLE `cloud_usage`.`cloud_usage` DROP COLUMN `virtual_size`;

ALTER TABLE `cloud`.`usage_event` CHANGE COLUMN `virtual_size1` `virtual_size` bigint unsigned;
ALTER TABLE `cloud_usage`.`usage_event` CHANGE COLUMN `virtual_size1` `virtual_size` bigint unsigned;
ALTER TABLE `cloud_usage`.`usage_storage` CHANGE COLUMN `virtual_size1` `virtual_size` bigint unsigned;
ALTER TABLE `cloud_usage`.`cloud_usage` CHANGE COLUMN `virtual_size1` `virtual_size` bigint unsigned;

ALTER TABLE `cloud`.`network_offerings` DROP COLUMN `concurrent_connections`;
ALTER TABLE `cloud`.`network_offerings` CHANGE COLUMN `concurrent_connections1` `concurrent_connections` int(10) unsigned COMMENT 'Load Balancer(haproxy) maximum number of concurrent connections(global max)';






