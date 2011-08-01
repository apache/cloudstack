--;
-- Schema upgrade from 2.2.8 to 2.2.9;
--;

INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'NetworkManager', 'network.dns.basiczone.updates', 'all', 'This parameter can take 2 values: all (default) and pod. It defines if DHCP/DNS requests have to be send to all dhcp servers in cloudstack, or only to the one in the same pod');

ALTER TABLE `cloud`.`op_host_capacity` DROP FOREIGN KEY `fk_op_host_capacity__pod_id`;
ALTER TABLE `cloud`.`op_host_capacity` DROP FOREIGN KEY `fk_op_host_capacity__data_center_id`;
ALTER TABLE `cloud`.`op_host_capacity` DROP FOREIGN KEY `fk_op_host_capacity__cluster_id`;

ALTER TABLE `cloud`.`firewall_rules_cidrs` ADD UNIQUE INDEX `unique_rule_cidrs`  (`firewall_rule_id`, `source_cidr`);

ALTER TABLE `cloud`.`cluster` ADD INDEX `i_cluster__removed`(`removed`);
ALTER TABLE `cloud`.`data_center` ADD INDEX `i_data_center__removed`(`removed`);
ALTER TABLE `cloud`.`host_pod_ref` ADD INDEX `i_host_pod_ref__removed`(`removed`);
ALTER TABLE `cloud`.`mshost` ADD INDEX `i_mshost__removed`(`removed`);
ALTER TABLE `cloud`.`template_zone_ref` ADD INDEX `i_template_zone_ref__removed`(`removed`);
ALTER TABLE `cloud`.`domain` ADD INDEX `i_domain__removed`(`removed`);
ALTER TABLE `cloud`.`disk_offering` ADD INDEX `i_disk_offering__removed`(`removed`);
ALTER TABLE `cloud`.`storage_pool` ADD INDEX `i_storage_pool__removed`(`removed`);
ALTER TABLE `cloud`.`instance_group` ADD INDEX `i_instance_group__removed`(`removed`);

ALTER TABLE `cloud`.`sync_queue_item` ADD INDEX `i_sync_queue_item__queue_proc_number`(`queue_proc_number`);
ALTER TABLE `cloud`.`sync_queue_item` ADD INDEX `i_sync_queue_item__queue_proc_msid`(`queue_proc_msid`);
ALTER TABLE `cloud`.`op_nwgrp_work` ADD INDEX `i_op_nwgrp_work__taken`(`taken`);
ALTER TABLE `cloud`.`op_nwgrp_work` ADD INDEX `i_op_nwgrp_work__step`(`step`);
ALTER TABLE `cloud`.`op_nwgrp_work` ADD INDEX `i_op_nwgrp_work__seq_no`(`seq_no`);
ALTER TABLE `cloud`.`volumes` ADD INDEX `i_volumes__state`(`state`);


INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'agent.load.threshold', '0.70', 'Percentage (as a value between 0 and 1) of connected agents after which agent load balancing will start happening');
INSERT IGNORE INTO configuration VALUES ('Network', 'DEFAULT', 'management-server', 'network.loadbalancer.haproxy.stats.visibility', 'global', 'Load Balancer(haproxy) stats visibilty, it can be global,guest-network,disabled');
INSERT IGNORE INTO configuration VALUES ('Network', 'DEFAULT', 'management-server', 'network.loadbalancer.haproxy.stats.uri','/admin?stats','Load Balancer(haproxy) uri.');
INSERT IGNORE INTO configuration VALUES ('Network', 'DEFAULT', 'management-server', 'network.loadbalancer.haproxy.stats.auth','admin1:AdMiN123','Load Balancer(haproxy) authetication string in the format username:password');
INSERT IGNORE INTO configuration VALUES ('Network', 'DEFAULT', 'management-server', 'network.loadbalancer.haproxy.stats.port','8081','Load Balancer(haproxy) stats port number.');

