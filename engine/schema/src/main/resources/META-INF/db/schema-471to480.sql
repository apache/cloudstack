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
-- Schema upgrade from 4.7.1 to 4.8.0;
--;

ALTER TABLE `cloud`.`nicira_nvp_router_map` DROP INDEX `logicalrouter_uuid` ;

ALTER TABLE `cloud`.`volume_host_ref` MODIFY COLUMN `url` varchar(2048);
ALTER TABLE `cloud`.`object_datastore_ref` MODIFY COLUMN `url` varchar(2048);
ALTER TABLE `cloud`.`image_store` MODIFY COLUMN `url` varchar(2048);
ALTER TABLE `cloud`.`template_store_ref` MODIFY COLUMN `url` varchar(2048);
ALTER TABLE `cloud`.`volume_store_ref` MODIFY COLUMN `url` varchar(2048);
ALTER TABLE `cloud`.`volume_store_ref` MODIFY COLUMN `download_url` varchar(2048);
ALTER TABLE `cloud`.`upload` MODIFY COLUMN `url` varchar(2048);
