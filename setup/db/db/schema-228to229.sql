--;
-- Schema upgrade from 2.2.8 to 2.2.9;
--;

ALTER TABLE `cloud`.`account` ADD COLUMN `network_domain` varchar(255);
ALTER TABLE `cloud`.`domain` ADD COLUMN `network_domain` varchar(255);

ALTER TABLE `cloud`.`cluster` ADD CONSTRAINT `fk_cluster__data_center_id` FOREIGN KEY (`data_center_id`) REFERENCES `cloud`.`data_center`(`id`) ON DELETE CASCADE;
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'NetworkManager', 'network.dns.basiczone.updates', 'all', 'This parameter can take 2 values: all (default) and pod. It defines if DHCP/DNS requests have to be send to all dhcp servers in cloudstack, or only to the one in the same pod');

ALTER TABLE `cloud`.`op_host_capacity` DROP FOREIGN KEY `fk_op_host_capacity__pod_id`;
ALTER TABLE `cloud`.`op_host_capacity` DROP FOREIGN KEY `fk_op_host_capacity__data_center_id`;
ALTER TABLE `cloud`.`op_host_capacity` DROP FOREIGN KEY `fk_op_host_capacity__cluster_id`;

INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'NetworkManager', 'domain.level.virtual.network', 'false', 'True if domain level virtual network is supported, false otherwise');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'agent.load.threshold', '0.70', 'Percentage (as a value between 0 and 1) of connected agents after which agent load balancing will start happening');
