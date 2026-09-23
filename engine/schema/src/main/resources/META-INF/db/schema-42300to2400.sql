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
-- Schema upgrade from 4.23.0.0 to 24.0.0
--;

-- resource_alert_rules: stores per-resource or generic metric threshold rules
CREATE TABLE IF NOT EXISTS `cloud`.`resource_alert_rules` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `uuid` varchar(255) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `resource_type` varchar(64) NOT NULL COMMENT 'VirtualMachine, Volume, Host, StoragePool',
    `resource_id` bigint unsigned DEFAULT NULL COMMENT 'null = applies to all resources of the type in scope',
    `account_id` bigint unsigned NOT NULL,
    `domain_id` bigint unsigned NOT NULL,
    `metric` varchar(64) NOT NULL,
    `condition_operator` varchar(8) NOT NULL COMMENT 'GT, GTE, LT, LTE, EQ',
    `threshold` double NOT NULL,
    `severity` varchar(32) NOT NULL COMMENT 'CRITICAL, HIGH, MEDIUM, LOW',
    `message` varchar(4096) DEFAULT NULL,
    `email` tinyint(1) NOT NULL DEFAULT 0,
    `reset_interval` int unsigned NOT NULL DEFAULT 600 COMMENT 'minimum seconds between repeat firings of this rule',
    `created` datetime DEFAULT NULL,
    `updated` datetime DEFAULT NULL,
    `removed` datetime DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `i_resource_alert_rules__account_id`(`account_id`),
    INDEX `i_resource_alert_rules__domain_id`(`domain_id`),
    CONSTRAINT `fk_resource_alert_rules__account_id` FOREIGN KEY (`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_resource_alert_rules__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- resource_alerts: immutable log of fired alerts
CREATE TABLE IF NOT EXISTS `cloud`.`resource_alerts` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `uuid` varchar(255) NOT NULL UNIQUE,
    `alert_rule_id` bigint unsigned NOT NULL,
    `resource_id` bigint unsigned DEFAULT NULL COMMENT 'the specific resource that triggered the alert',
    `metric_type` varchar(64) NOT NULL,
    `metric_value` double NOT NULL,
    `severity` varchar(32) NOT NULL,
    `message` varchar(4096) DEFAULT NULL,
    `alert_timestamp` datetime NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `i_resource_alerts__alert_rule_id`(`alert_rule_id`),
    INDEX `i_resource_alerts__alert_timestamp`(`alert_timestamp`),
    CONSTRAINT `fk_resource_alerts__alert_rule_id` FOREIGN KEY (`alert_rule_id`) REFERENCES `resource_alert_rules`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- resource_alert_rules_webhook: webhooks a rule delivers its alerts to
CREATE TABLE IF NOT EXISTS `cloud`.`resource_alert_rules_webhook` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `resource_alert_rule_id` bigint unsigned NOT NULL,
    `webhook_id` bigint unsigned NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uc_resource_alert_rules_webhook__rule_webhook`(`resource_alert_rule_id`, `webhook_id`),
    CONSTRAINT `fk_resource_alert_rules_webhook__rule_id` FOREIGN KEY (`resource_alert_rule_id`) REFERENCES `resource_alert_rules`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_resource_alert_rules_webhook__webhook_id` FOREIGN KEY (`webhook_id`) REFERENCES `webhook`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
