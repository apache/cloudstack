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

CREATE TABLE `cloud`.`backup_offering_details` (
    `id` bigint unsigned NOT NULL auto_increment,
    `backup_offering_id` bigint unsigned NOT NULL COMMENT 'Backup offering id',
    `name` varchar(255) NOT NULL,
    `value` varchar(1024) NOT NULL,
    `display` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Should detail be displayed to the end user',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_offering_details__backup_offering_id` FOREIGN KEY `fk_offering_details__backup_offering_id`(`backup_offering_id`) REFERENCES `backup_offering`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Update value to random for the config 'vm.allocation.algorithm' or 'volume.allocation.algorithm' if configured as userconcentratedpod_random
-- Update value to firstfit for the config 'vm.allocation.algorithm' or 'volume.allocation.algorithm' if configured as userconcentratedpod_firstfit
UPDATE `cloud`.`configuration` SET value='random' WHERE name IN ('vm.allocation.algorithm', 'volume.allocation.algorithm') AND value='userconcentratedpod_random';
UPDATE `cloud`.`configuration` SET value='firstfit' WHERE name IN ('vm.allocation.algorithm', 'volume.allocation.algorithm') AND value='userconcentratedpod_firstfit';

-- Create kubernetes_cluster_affinity_group_map table for CKS per-node-type affinity groups
CREATE TABLE IF NOT EXISTS `cloud`.`kubernetes_cluster_affinity_group_map` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `cluster_id` bigint unsigned NOT NULL COMMENT 'kubernetes cluster id',
    `node_type` varchar(32) NOT NULL COMMENT 'CONTROL, WORKER, or ETCD',
    `affinity_group_id` bigint unsigned NOT NULL COMMENT 'affinity group id',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_kubernetes_cluster_ag_map__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `kubernetes_cluster`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_kubernetes_cluster_ag_map__ag_id` FOREIGN KEY (`affinity_group_id`) REFERENCES `affinity_group`(`id`) ON DELETE CASCADE,
    INDEX `i_kubernetes_cluster_ag_map__cluster_id`(`cluster_id`),
    INDEX `i_kubernetes_cluster_ag_map__ag_id`(`affinity_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Create webhook_filter table
DROP TABLE IF EXISTS `cloud`.`webhook_filter`;
CREATE TABLE IF NOT EXISTS `cloud`.`webhook_filter` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id of the webhook filter',
    `uuid` varchar(255) COMMENT 'uuid of the webhook filter',
    `webhook_id` bigint unsigned  NOT NULL COMMENT 'id of the webhook',
    `type` varchar(20) COMMENT 'type of the filter',
    `mode` varchar(20) COMMENT 'mode of the filter',
    `match_type` varchar(20) COMMENT 'match type of the filter',
    `value` varchar(256) NOT NULL COMMENT 'value of the filter used for matching',
    `created` datetime NOT NULL COMMENT 'date created',
    PRIMARY KEY (`id`),
    INDEX `i_webhook_filter__webhook_id`(`webhook_id`),
    CONSTRAINT `fk_webhook_filter__webhook_id` FOREIGN KEY(`webhook_id`) REFERENCES `webhook`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- "api_keypair" table for API and secret keys
CREATE TABLE IF NOT EXISTS `cloud`.`api_keypair` (
    `id` bigint(20) unsigned NOT NULL auto_increment,
    `uuid` varchar(40) UNIQUE NOT NULL,
    `name` varchar(255) NOT NULL,
    `domain_id` bigint(20) unsigned NOT NULL,
    `account_id` bigint(20) unsigned NOT NULL,
    `user_id` bigint(20) unsigned NOT NULL,
    `start_date` datetime,
    `end_date` datetime,
    `description` varchar(100),
    `api_key` varchar(255) NOT NULL,
    `secret_key` varchar(255) NOT NULL,
    `created` datetime NOT NULL,
    `removed` datetime,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_api_keypair__user_id` FOREIGN KEY(`user_id`) REFERENCES `cloud`.`user`(`id`),
    CONSTRAINT `fk_api_keypair__account_id` FOREIGN KEY(`account_id`) REFERENCES `cloud`.`account`(`id`),
    CONSTRAINT `fk_api_keypair__domain_id` FOREIGN KEY(`domain_id`) REFERENCES `cloud`.`domain`(`id`)
);

-- "api_keypair_permissions" table for API key pairs permissions
CREATE TABLE IF NOT EXISTS `cloud`.`api_keypair_permissions` (
    `id` bigint(20) unsigned NOT NULL auto_increment,
    `uuid` varchar(40) UNIQUE,
    `sort_order` bigint(20) unsigned NOT NULL DEFAULT 0,
    `rule` varchar(255) NOT NULL,
    `api_keypair_id` bigint(20) unsigned NOT NULL,
    `permission` varchar(255) NOT NULL,
    `description` varchar(255),
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_keypair_permissions__api_keypair_id` FOREIGN KEY(`api_keypair_id`) REFERENCES `cloud`.`api_keypair`(`id`)
);

-- Populate "api_keypair" table with existing user API keys
INSERT INTO `cloud`.`api_keypair` (uuid, user_id, domain_id, account_id, api_key, secret_key, created, name)
SELECT UUID(), user.id, account.domain_id, account.id, user.api_key, user.secret_key, NOW(), 'Active key pair'
FROM `cloud`.`user` AS user
JOIN `cloud`.`account` AS account ON user.account_id = account.id
WHERE user.api_key IS NOT NULL AND user.secret_key IS NOT NULL;

-- Drop API keys from user table
ALTER TABLE `cloud`.`user` DROP COLUMN api_key, DROP COLUMN secret_key;

-- Grant access to the "deleteUserKeys" API to the "User", "Domain Admin" and "Resource Admin" roles, similarly to the "registerUserKeys" API
CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('User', 'deleteUserKeys', 'ALLOW');
CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('Domain Admin', 'deleteUserKeys', 'ALLOW');
CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('Resource Admin', 'deleteUserKeys', 'ALLOW');

-- Add conserve mode for VPC offerings
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vpc_offerings','conserve_mode', 'tinyint(1) unsigned NULL DEFAULT 0 COMMENT ''True if the VPC offering is IP conserve mode enabled, allowing public IP services to be used across multiple VPC tiers'' ');

-- KMS HSM Profiles (Generic table for PKCS#11, KMIP, etc.)
-- Scoped to account (user-provided) or global/zone (admin-provided)
CREATE TABLE IF NOT EXISTS `cloud`.`kms_hsm_profiles` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `uuid` VARCHAR(40) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `protocol` VARCHAR(32) NOT NULL COMMENT 'PKCS11, KMIP, AWS_KMS, etc.',

    -- Scoping
    `account_id` BIGINT UNSIGNED COMMENT 'null = admin-provided (available to all accounts)',
    `domain_id` BIGINT UNSIGNED COMMENT 'null = zone/global scope',
    `zone_id` BIGINT UNSIGNED COMMENT 'null = global scope',

    -- Metadata
    `vendor_name` VARCHAR(64) COMMENT 'HSM vendor (Thales, AWS, SoftHSM, etc.)',
    `enabled` BOOLEAN NOT NULL DEFAULT TRUE,
    `system` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'System profile (globally available, root admin only)',
    `created` DATETIME NOT NULL,
    `removed` DATETIME,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uuid` (`uuid`),
    UNIQUE KEY `uk_account_name` (`account_id`, `name`, `removed`),
    INDEX `idx_protocol_enabled` (`protocol`, `enabled`, `removed`),
    INDEX `idx_scoping` (`account_id`, `domain_id`, `zone_id`, `removed`),
    CONSTRAINT `fk_kms_hsm_profiles__account_id` FOREIGN KEY (`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_kms_hsm_profiles__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_kms_hsm_profiles__zone_id` FOREIGN KEY (`zone_id`) REFERENCES `data_center`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='HSM profiles for KMS providers';

-- KMS HSM Profile Details (Protocol-specific configuration)
CREATE TABLE IF NOT EXISTS `cloud`.`kms_hsm_profile_details` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `profile_id` BIGINT UNSIGNED NOT NULL COMMENT 'HSM profile ID',
    `name` VARCHAR(255) NOT NULL COMMENT 'Config key (e.g. library_path, endpoint, pin, cert_content)',
    `value` TEXT NOT NULL COMMENT 'Config value (encrypted if sensitive)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_profile_name` (`profile_id`, `name`),
    CONSTRAINT `fk_kms_hsm_profile_details__profile_id` FOREIGN KEY (`profile_id`) REFERENCES `kms_hsm_profiles`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Details for HSM profiles (key-value configuration)';

-- KMS Keys (Key Encryption Key Metadata)
-- Account-scoped KEKs for envelope encryption
CREATE TABLE IF NOT EXISTS `cloud`.`kms_keys` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Unique ID',
    `uuid` VARCHAR(40) NOT NULL COMMENT 'UUID - user-facing identifier',
    `name` VARCHAR(255) NOT NULL COMMENT 'User-friendly name',
    `description` VARCHAR(1024) COMMENT 'User description',
    `kek_label` VARCHAR(255) NOT NULL COMMENT 'Provider-specific KEK label/ID',
    `purpose` VARCHAR(32) NOT NULL COMMENT 'Key purpose (VOLUME_ENCRYPTION, TLS_CERT)',
    `account_id` BIGINT UNSIGNED NOT NULL COMMENT 'Owning account',
    `domain_id` BIGINT UNSIGNED NOT NULL COMMENT 'Owning domain',
    `zone_id` BIGINT UNSIGNED NOT NULL COMMENT 'Zone where key is valid',
    `algorithm` VARCHAR(64) NOT NULL DEFAULT 'AES/GCM/NoPadding' COMMENT 'Encryption algorithm',
    `key_bits` INT NOT NULL DEFAULT 256 COMMENT 'Key size in bits',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Whether the key is enabled for new cryptographic operations',
    `hsm_profile_id` BIGINT UNSIGNED NOT NULL COMMENT 'Current HSM profile ID for this key',
    `created` DATETIME NOT NULL COMMENT 'Creation timestamp',
    `removed` DATETIME COMMENT 'Removal timestamp for soft delete',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uuid` (`uuid`),
    INDEX `idx_account_purpose` (`account_id`, `purpose`, `enabled`),
    INDEX `idx_domain_purpose` (`domain_id`, `purpose`, `enabled`),
    INDEX `idx_zone_enabled` (`zone_id`, `enabled`),
    CONSTRAINT `fk_kms_keys__account_id` FOREIGN KEY (`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_kms_keys__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_kms_keys__zone_id` FOREIGN KEY (`zone_id`) REFERENCES `data_center`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_kms_keys__hsm_profile_id` FOREIGN KEY (`hsm_profile_id`) REFERENCES `kms_hsm_profiles`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KMS Key (KEK) metadata - account-scoped keys for envelope encryption';

-- KMS KEK Versions (multiple KEKs per KMS key for gradual rotation)
-- Supports multiple KEK versions per logical KMS key during rotation
CREATE TABLE IF NOT EXISTS `cloud`.`kms_kek_versions` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Unique ID',
    `uuid` VARCHAR(40) NOT NULL COMMENT 'UUID',
    `kms_key_id` BIGINT UNSIGNED NOT NULL COMMENT 'Reference to kms_keys table',
    `version_number` INT NOT NULL COMMENT 'Version number (1, 2, 3, ...)',
    `kek_label` VARCHAR(255) NOT NULL COMMENT 'Provider-specific KEK label/ID for this version',
    `status` VARCHAR(32) NOT NULL DEFAULT 'Active' COMMENT 'Active, Previous, Archived',
    `hsm_profile_id` BIGINT UNSIGNED COMMENT 'HSM profile where this KEK version is stored',
    `created` DATETIME NOT NULL COMMENT 'Creation timestamp',
    `removed` DATETIME COMMENT 'Removal timestamp for soft delete',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uuid` (`uuid`),
    UNIQUE KEY `uk_kms_key_version` (`kms_key_id`, `version_number`, `removed`),
    INDEX `idx_kms_key_status` (`kms_key_id`, `status`, `removed`),
    INDEX `idx_kek_label` (`kek_label`),
    CONSTRAINT `fk_kms_kek_versions__kms_key_id` FOREIGN KEY (`kms_key_id`) REFERENCES `kms_keys`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_kms_kek_versions__hsm_profile_id` FOREIGN KEY (`hsm_profile_id`) REFERENCES `kms_hsm_profiles`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KEK versions for a KMS key - supports gradual rotation';

-- KMS Wrapped Keys (Data Encryption Keys)
-- Generic table for wrapped DEKs - references kms_keys for metadata and kek_versions for specific KEK version
CREATE TABLE IF NOT EXISTS `cloud`.`kms_wrapped_key` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Unique ID',
    `uuid` VARCHAR(40) NOT NULL COMMENT 'UUID',
    `kms_key_id` BIGINT UNSIGNED COMMENT 'Reference to kms_keys table',
    `kek_version_id` BIGINT UNSIGNED COMMENT 'Reference to kms_kek_versions table',
    `zone_id` BIGINT UNSIGNED NOT NULL COMMENT 'Zone ID for zone-scoped keys',
    `wrapped_blob` VARBINARY(4096) NOT NULL COMMENT 'Encrypted DEK material',
    `created` DATETIME NOT NULL COMMENT 'Creation timestamp',
    `removed` DATETIME COMMENT 'Removal timestamp for soft delete',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uuid` (`uuid`),
    INDEX `idx_kms_key_id` (`kms_key_id`, `removed`),
    INDEX `idx_kek_version_id` (`kek_version_id`, `removed`),
    INDEX `idx_zone_id` (`zone_id`, `removed`),
    CONSTRAINT `fk_kms_wrapped_key__kms_key_id` FOREIGN KEY (`kms_key_id`) REFERENCES `kms_keys`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_kms_wrapped_key__kek_version_id` FOREIGN KEY (`kek_version_id`) REFERENCES `kms_kek_versions`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_kms_wrapped_key__zone_id` FOREIGN KEY (`zone_id`) REFERENCES `data_center`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KMS wrapped encryption keys (DEKs) - references kms_keys for KEK metadata and kek_versions for specific version';

-- Add KMS key reference to volumes table (which KMS key was used)
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.volumes', 'kms_key_id', 'BIGINT UNSIGNED COMMENT ''KMS key ID used for volume encryption''');
CALL `cloud`.`IDEMPOTENT_ADD_FOREIGN_KEY`('cloud.volumes', 'fk_volumes__kms_key_id', '(kms_key_id)', '`kms_keys`(`id`)');

-- Add KMS wrapped key reference to volumes table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.volumes', 'kms_wrapped_key_id', 'BIGINT UNSIGNED COMMENT ''KMS wrapped key ID for volume encryption''');
CALL `cloud`.`IDEMPOTENT_ADD_FOREIGN_KEY`('cloud.volumes', 'fk_volumes__kms_wrapped_key_id', '(kms_wrapped_key_id)', '`kms_wrapped_key`(`id`)');

-- KMS Database Provider KEK Objects (PKCS#11-like object storage)
-- Stores KEKs for the database KMS provider in a PKCS#11-compatible format
CREATE TABLE IF NOT EXISTS `cloud`.`kms_database_kek_objects` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Object handle (PKCS#11 CKA_HANDLE)',
    `uuid` VARCHAR(40) NOT NULL COMMENT 'UUID',
    -- PKCS#11 Object Class (CKA_CLASS)
    `object_class` VARCHAR(32) NOT NULL DEFAULT 'CKO_SECRET_KEY' COMMENT 'PKCS#11 object class (CKO_SECRET_KEY, CKO_PRIVATE_KEY, etc.)',
    -- PKCS#11 Label (CKA_LABEL) - human-readable identifier
    `label` VARCHAR(255) NOT NULL COMMENT 'PKCS#11 label (CKA_LABEL) - human-readable identifier',
    -- PKCS#11 ID (CKA_ID) - application-defined identifier
    `object_id` VARBINARY(64) COMMENT 'PKCS#11 object ID (CKA_ID) - application-defined identifier',
    -- Key Type (CKA_KEY_TYPE)
    `key_type` VARCHAR(32) NOT NULL DEFAULT 'CKK_AES' COMMENT 'PKCS#11 key type (CKK_AES, CKK_RSA, etc.)',
    -- Key Material (CKA_VALUE) - encrypted KEK material
    `key_material` VARBINARY(512) NOT NULL COMMENT 'PKCS#11 key value (CKA_VALUE) - encrypted KEK material',
    -- Key Attributes (PKCS#11 boolean attributes)
    `is_sensitive` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'PKCS#11 CKA_SENSITIVE - key material is sensitive',
    `is_extractable` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'PKCS#11 CKA_EXTRACTABLE - key can be extracted',
    `is_token` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'PKCS#11 CKA_TOKEN - object is on token (persistent)',
    `is_private` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'PKCS#11 CKA_PRIVATE - object is private',
    `is_modifiable` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'PKCS#11 CKA_MODIFIABLE - object can be modified',
    `is_copyable` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'PKCS#11 CKA_COPYABLE - object can be copied',
    `is_destroyable` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'PKCS#11 CKA_DESTROYABLE - object can be destroyed',
    `always_sensitive` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'PKCS#11 CKA_ALWAYS_SENSITIVE - key was always sensitive',
    `never_extractable` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'PKCS#11 CKA_NEVER_EXTRACTABLE - key was never extractable',
    -- Key Metadata
    `purpose` VARCHAR(32) NOT NULL COMMENT 'Key purpose (VOLUME_ENCRYPTION, TLS_CERT)',
    `key_bits` INT NOT NULL COMMENT 'Key size in bits (128, 192, 256)',
    `algorithm` VARCHAR(64) NOT NULL DEFAULT 'AES/GCM/NoPadding' COMMENT 'Encryption algorithm',
    -- Validity Dates (PKCS#11 CKA_START_DATE, CKA_END_DATE)
    `start_date` DATETIME COMMENT 'PKCS#11 CKA_START_DATE - key validity start',
    `end_date` DATETIME COMMENT 'PKCS#11 CKA_END_DATE - key validity end',
    -- Lifecycle
    `created` DATETIME NOT NULL COMMENT 'Creation timestamp',
    `last_used` DATETIME COMMENT 'Last usage timestamp',
    `removed` DATETIME COMMENT 'Removal timestamp for soft delete',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uuid` (`uuid`),
    UNIQUE KEY `uk_label_removed` (`label`, `removed`),
    INDEX `idx_purpose_removed` (`purpose`, `removed`),
    INDEX `idx_key_type` (`key_type`, `removed`),
    INDEX `idx_object_class` (`object_class`, `removed`),
    INDEX `idx_created` (`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KMS Database Provider KEK Objects - PKCS#11-like object storage for KEKs';
