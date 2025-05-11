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
-- Schema upgrade from 4.20.1.0 to 4.21.0.0
--;

-- Add columns max_backup and backup_interval_type to backup table
ALTER TABLE `cloud`.`backup_schedule` ADD COLUMN `max_backups` int(8) default NULL COMMENT 'maximum number of backups to maintain';
ALTER TABLE `cloud`.`backups` ADD COLUMN `backup_interval_type` int(5) COMMENT 'type of backup, e.g. manual, recurring - hourly, daily, weekly or monthly';

-- Add console_endpoint_creator_address column to cloud.console_session table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.console_session', 'console_endpoint_creator_address', 'VARCHAR(45)');

-- Add client_address column to cloud.console_session table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.console_session', 'client_address', 'VARCHAR(45)');

-- Allow default roles to use quotaCreditsList
INSERT INTO `cloud`.`role_permissions` (uuid, role_id, rule, permission, sort_order)
SELECT uuid(), role_id, 'quotaCreditsList', permission, sort_order
FROM `cloud`.`role_permissions` rp
WHERE rp.rule = 'quotaStatement'
  AND NOT EXISTS(SELECT 1 FROM cloud.role_permissions rp_ WHERE rp.role_id = rp_.role_id AND rp_.rule = 'quotaCreditsList');

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.host', 'last_mgmt_server_id', 'bigint unsigned DEFAULT NULL COMMENT "last management server this host is connected to" AFTER `mgmt_server_id`');

<<<<<<< HEAD

-----------------------------------------------------------
-- CKS Enhancements:
-----------------------------------------------------------
-- Add for_cks column to the vm_template table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vm_template','for_cks', 'int(1) unsigned DEFAULT "0" COMMENT "if true, the template can be used for CKS cluster deployment"');

-- Add support for different node types service offerings on CKS clusters
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster','control_service_offering_id', 'bigint unsigned COMMENT "service offering ID for Control Node(s)"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster','worker_service_offering_id', 'bigint unsigned COMMENT "service offering ID for Worker Node(s)"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster','etcd_service_offering_id', 'bigint unsigned COMMENT "service offering ID for etcd Nodes"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster','etcd_node_count', 'bigint unsigned COMMENT "number of etcd nodes to be deployed for the Kubernetes cluster"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster','control_template_id', 'bigint unsigned COMMENT "template id to be used for Control Node(s)"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster','worker_template_id', 'bigint unsigned COMMENT "template id to be used for Worker Node(s)"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster','etcd_template_id', 'bigint unsigned COMMENT "template id to be used for etcd Nodes"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster','cni_config_id', 'bigint unsigned COMMENT "userdata id representing the associated cni configuration"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster','cni_config_details', 'varchar(4096) DEFAULT NULL COMMENT "userdata details representing the values required for the cni configuration associated"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster_vm_map','etcd_node', 'tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT "indicates if the VM is an etcd node"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster_vm_map','external_node', 'tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT "indicates if the node was imported into the Kubernetes cluster"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster_vm_map','manual_upgrade', 'tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT "indicates if the node is marked for manual upgrade and excluded from the Kubernetes cluster upgrade operation"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster_vm_map','kubernetes_node_version', 'varchar(40) COMMENT "version of k8s the cluster node is on"');

ALTER TABLE `cloud`.`kubernetes_cluster` ADD CONSTRAINT `fk_cluster__control_service_offering_id` FOREIGN KEY `fk_cluster__control_service_offering_id`(`control_service_offering_id`) REFERENCES `service_offering`(`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`kubernetes_cluster` ADD CONSTRAINT `fk_cluster__worker_service_offering_id` FOREIGN KEY `fk_cluster__worker_service_offering_id`(`worker_service_offering_id`) REFERENCES `service_offering`(`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`kubernetes_cluster` ADD CONSTRAINT `fk_cluster__etcd_service_offering_id` FOREIGN KEY `fk_cluster__etcd_service_offering_id`(`etcd_service_offering_id`) REFERENCES `service_offering`(`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`kubernetes_cluster` ADD CONSTRAINT `fk_cluster__control_template_id` FOREIGN KEY `fk_cluster__control_template_id`(`control_template_id`) REFERENCES `vm_template`(`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`kubernetes_cluster` ADD CONSTRAINT `fk_cluster__worker_template_id` FOREIGN KEY `fk_cluster__worker_template_id`(`worker_template_id`) REFERENCES `vm_template`(`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`kubernetes_cluster` ADD CONSTRAINT `fk_cluster__etcd_template_id` FOREIGN KEY `fk_cluster__etcd_template_id`(`etcd_template_id`) REFERENCES `vm_template`(`id`) ON DELETE CASCADE;

-- Add for_cks column to the user_data table to represent CNI Configuration stored as userdata
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.user_data','for_cks', 'int(1) unsigned DEFAULT "0" COMMENT "if true, the userdata represent CNI configuration meant for CKS use only"');
-----------------------------------------------------------
-- END - CKS Enhancements
-----------------------------------------------------------

-- Add table for reconcile commands
CREATE TABLE IF NOT EXISTS `cloud`.`reconcile_commands` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `management_server_id` bigint unsigned NOT NULL COMMENT 'node id of the management server',
    `host_id` bigint unsigned NOT NULL COMMENT 'id of the host',
    `request_sequence` bigint unsigned NOT NULL COMMENT 'sequence of the request',
    `resource_id` bigint unsigned DEFAULT NULL COMMENT 'id of the resource',
    `resource_type` varchar(255) COMMENT 'type if the resource',
    `state_by_management` varchar(255) COMMENT 'state of the command updated by management server',
    `state_by_agent` varchar(255) COMMENT 'state of the command updated by cloudstack agent',
    `command_name` varchar(255) COMMENT 'name of the command',
    `command_info` MEDIUMTEXT COMMENT 'info of the command',
    `answer_name` varchar(255) COMMENT 'name of the answer',
    `answer_info` MEDIUMTEXT COMMENT 'info of the answer',
    `created` datetime COMMENT 'date the reconcile command was created',
    `removed` datetime COMMENT 'date the reconcile command was removed',
    `updated` datetime COMMENT 'date the reconcile command was updated',
    `retry_count` bigint unsigned DEFAULT 0 COMMENT 'The retry count of reconciliation',
    PRIMARY KEY(`id`),
    INDEX `i_reconcile_command__host_id`(`host_id`),
    CONSTRAINT `fk_reconcile_command__host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
