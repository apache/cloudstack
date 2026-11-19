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

--
-- AI Services ownership table (SPEC.md section 9).
--
-- Deliberately minimal. The engine is the source of truth for runtime
-- state: replicas, health, rollouts and usage all live there. This table
-- exists only so CloudStack can do what CloudStack does natively --
-- enforce ownership, filter list commands by account and domain, and
-- clean up when an account or domain is deleted. Duplicating engine
-- state here would create two answers to the same question.
--

CREATE TABLE IF NOT EXISTS `cloud`.`ai_deployment` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(40) NOT NULL COMMENT 'UUID exposed through the API',
  `name` varchar(255) NOT NULL COMMENT 'catalog name of the deployed model',
  `account_id` bigint unsigned NOT NULL COMMENT 'owning account',
  `domain_id` bigint unsigned NOT NULL COMMENT 'owning domain',
  `zone_id` bigint unsigned NOT NULL COMMENT 'data center the deployment runs in',
  `engine_id` varchar(255) NOT NULL COMMENT 'the engine''s identifier for this deployment',
  `state` varchar(32) NOT NULL DEFAULT 'Allocated' COMMENT 'last state the engine reported',
  `created` datetime NOT NULL,
  `removed` datetime DEFAULT NULL COMMENT 'soft delete, as CloudStack expects',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uc_ai_deployment__uuid` (`uuid`),
  KEY `i_ai_deployment__account_id` (`account_id`),
  KEY `i_ai_deployment__domain_id` (`domain_id`),
  KEY `i_ai_deployment__zone_id` (`zone_id`),
  KEY `i_ai_deployment__removed` (`removed`),
  CONSTRAINT `fk_ai_deployment__account_id`
    FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ai_deployment__domain_id`
    FOREIGN KEY (`domain_id`) REFERENCES `domain` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ai_deployment__zone_id`
    FOREIGN KEY (`zone_id`) REFERENCES `data_center` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
