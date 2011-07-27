--;
-- Schema upgrade from 2.2.8 to 2.2.9;
--;

ALTER TABLE `cloud`.`account` ADD COLUMN `network_domain` varchar(255);
ALTER TABLE `cloud`.`domain` ADD COLUMN `network_domain` varchar(255);

ALTER TABLE `cloud`.`cluster` ADD CONSTRAINT `fk_cluster__data_center_id` FOREIGN KEY (`data_center_id`) REFERENCES `cloud`.`data_center`(`id`) ON DELETE CASCADE;

ALTER TABLE `cloud`.`op_host_capacity` DROP FOREIGN KEY `fk_op_host_capacity__pod_id`;
ALTER TABLE `cloud`.`op_host_capacity` DROP FOREIGN KEY `fk_op_host_capacity__data_center_id`;

ALTER TABLE `cloud`.`op_host_capacity` DROP FOREIGN KEY `fk_op_host_capacity__cluster_id`;


INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'agent.load.threshold', '0.70', 'Percentage (as a value between 0 and 1) of connected agents after which agent load balancing will start happening');
INSERT IGNORE INTO configuration VALUES ('Network', 'DEFAULT', 'management-server', 'network.loadbalancer.haproxy.stats.visibility', 'global', 'Load Balancer(haproxy) stats visibilty, it can be global,guest-network,disabled');
INSERT IGNORE INTO configuration VALUES ('Network', 'DEFAULT', 'management-server', 'network.loadbalancer.haproxy.stats.uri','/admin?stats','Load Balancer(haproxy) uri.');
INSERT IGNORE INTO configuration VALUES ('Network', 'DEFAULT', 'management-server', 'network.loadbalancer.haproxy.stats.auth','admin1:AdMiN123','Load Balancer(haproxy) authetication string in the format username:password');
INSERT IGNORE INTO configuration VALUES ('Network', 'DEFAULT', 'management-server', 'network.loadbalancer.haproxy.stats.port','8081','Load Balancer(haproxy) stats port number.');


