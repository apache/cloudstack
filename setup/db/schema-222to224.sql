--;
-- Schema upgrade from 2.2.2 to 2.2.4;
--;
ALTER TABLE `cloud`.`op_host_capacity` ADD COLUMN `cluster_id` bigint unsigned AFTER `pod_id`;
ALTER TABLE `cloud`.`op_host_capacity` ADD CONSTRAINT `fk_op_host_capacity__cluster_id` FOREIGN KEY `fk_op_host_capacity__cluster_id` (`cluster_id`) REFERENCES `cloud`.`cluster`(`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`op_host_capacity` ADD INDEX `i_op_host_capacity__cluster_id`(`cluster_id`);
ALTER TABLE `cloud`.`usage_event` ADD COLUMN `resource_type` varchar(32);

CREATE TABLE `cloud`.`domain_network_ref` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `domain_id` bigint unsigned NOT NULL COMMENT 'domain id',
  `network_id` bigint unsigned NOT NULL COMMENT 'network id',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_domain_network_ref__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_domain_network_ref__networks_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
