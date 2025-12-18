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
-- Schema upgrade from 4.22.1.0 to 4.23.0.0
--;

ALTER TABLE `cloud`.`oauth_provider` ADD COLUMN `domain_id` bigint unsigned DEFAULT NULL COMMENT 'NULL for global provider, domain ID for domain-specific' AFTER `redirect_uri`;
ALTER TABLE `cloud`.`oauth_provider` ADD CONSTRAINT `fk_oauth_provider__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain`(`id`);
ALTER TABLE `cloud`.`oauth_provider` ADD INDEX `i_oauth_provider__domain_id`(`domain_id`);

ALTER TABLE `cloud`.`oauth_provider` ADD UNIQUE KEY `uk_oauth_provider__provider_domain` (`provider`, `domain_id`);