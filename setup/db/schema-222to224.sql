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

ALTER TABLE `cloud`.`nics` MODIFY `ip4_address` char(40);
ALTER TABLE `cloud`.`op_lock` MODIFY `ip` char(40) NOT NULL;
ALTER TABLE `cloud`.`volumes` MODIFY `host_ip` char(40);
ALTER TABLE `cloud`.`op_dc_ip_address_alloc` MODIFY `ip_address` char(40) NOT NULL;
ALTER TABLE `cloud`.`op_dc_link_local_ip_address_alloc` MODIFY `ip_address` char(40) NOT NULL;
ALTER TABLE `cloud`.`host` MODIFY `private_ip_address` char(40) NOT NULL;
ALTER TABLE `cloud`.`host` MODIFY `storage_ip_address` char(40) NOT NULL;
ALTER TABLE `cloud`.`host` MODIFY `storage_ip_address_2` char(40);
ALTER TABLE `cloud`.`host` MODIFY `public_ip_address` char(40);
ALTER TABLE `cloud`.`mshost` MODIFY `service_ip` char(40) NOT NULL;
ALTER TABLE `cloud`.`user_statistics` MODIFY `public_ip_address` char(40);
ALTER TABLE `cloud`.`vm_instance` MODIFY `private_ip_address` char(40);
ALTER TABLE `cloud`.`user_vm` MODIFY `guest_ip_address` char(40);
ALTER TABLE `cloud`.`domain_router` MODIFY `public_ip_address` char(40);
ALTER TABLE `cloud`.`domain_router` MODIFY `guest_ip_address` char(40);
ALTER TABLE `cloud`.`console_proxy` MODIFY `public_ip_address` char(40) UNIQUE;
ALTER TABLE `cloud`.`secondary_storage_vm` MODIFY `public_ip_address` char(40) UNIQUE;
ALTER TABLE `cloud`.`load_balancer` MODIFY `ip_address` char(40) NOT NULL;
ALTER TABLE `cloud`.`remote_access_vpn` MODIFY `local_ip` char(40) NOT NULL;
ALTER TABLE `cloud`.`storage_pool` MODIFY `host_address` char(40) NOT NULL;


ALTER TABLE `cloud`.`networks` DROP FOREIGN KEY `fk_networks__related`;
ALTER TABLE `cloud`.`networks` ADD CONSTRAINT `fk_networks__related` FOREIGN KEY(`related`) REFERENCES `networks`(`id`) ON DELETE CASCADE;

