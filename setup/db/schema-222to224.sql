--;
-- Schema upgrade from 2.2.2 to 2.2.4;
--;
ALTER TABLE `cloud`.`op_host_capacity` ADD COLUMN `cluster_id` bigint unsigned AFTER `pod_id`;
ALTER TABLE `cloud`.`op_host_capacity` ADD CONSTRAINT `fk_op_host_capacity__cluster_id` FOREIGN KEY `fk_op_host_capacity__cluster_id` (`cluster_id`) REFERENCES `cloud`.`cluster`(`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`op_host_capacity` ADD INDEX `i_op_host_capacity__cluster_id`(`cluster_id`);