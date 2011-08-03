--;
-- Schema upgrade from 2.2.8 to 2.2.9;
--;

INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'NetworkManager', 'network.dns.basiczone.updates', 'all', 'This parameter can take 2 values: all (default) and pod. It defines if DHCP/DNS requests have to be send to all dhcp servers in cloudstack, or only to the one in the same pod');

ALTER TABLE `cloud`.`op_host_capacity` DROP FOREIGN KEY `fk_op_host_capacity__pod_id`;
ALTER TABLE `cloud`.`op_host_capacity` DROP FOREIGN KEY `fk_op_host_capacity__data_center_id`;
ALTER TABLE `cloud`.`op_host_capacity` DROP FOREIGN KEY `fk_op_host_capacity__cluster_id`;

ALTER TABLE `cloud`.`firewall_rules_cidrs` ADD UNIQUE INDEX `unique_rule_cidrs`  (`firewall_rule_id`, `source_cidr`);
ALTER TABLE `cloud`.`firewall_rules` ADD INDEX `i_firewall_rules__purpose` (`purpose`);

ALTER TABLE `cloud`.`cluster` ADD INDEX `i_cluster__removed`(`removed`);
ALTER TABLE `cloud`.`data_center` ADD INDEX `i_data_center__removed`(`removed`);
ALTER TABLE `cloud`.`host_pod_ref` ADD INDEX `i_host_pod_ref__removed`(`removed`);

ALTER TABLE `cloud`.`mshost` ADD INDEX `i_mshost__removed`(`removed`);
ALTER TABLE `cloud`.`mshost` ADD INDEX `i_mshost__last_update` (`last_update`);

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

ALTER TABLE `cloud`.`op_vm_ruleset_log` ADD INDEX `i_op_vm_ruleset_log__instance_id` (`instance_id`);

ALTER TABLE `cloud`.`storage_pool_host_ref` ADD CONSTRAINT `fk_storage_pool_host_ref__host_id` FOREIGN KEY `fk_storage_pool_host_ref__host_id`(`host_id`) REFERENCES `host` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`storage_pool_host_ref` ADD CONSTRAINT `fk_storage_pool_host_ref__pool_id` FOREIGN KEY `fk_storage_pool_host_ref__pool_id`(`pool_id`) REFERENCES `storage_pool`(`id`) ON DELETE CASCADE;

ALTER TABLE `cloud`.`network_offerings` ADD INDEX `i_network_offerings__system_only` (`system_only`);
ALTER TABLE `cloud`.`resource_count` ADD CONSTRAINT `fk_resource_count__account_id` FOREIGN KEY `fk_resource_count__account_id`(`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`resource_count` ADD CONSTRAINT `fk_resource_count__domain_id` FOREIGN KEY `fk_resource_count__domain_id`(`domain_id`) REFERENCES `domain` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`resource_count` ADD INDEX `i_resource_count__type` (`type`);

ALTER TABLE `cloud`.`configuration` ADD INDEX `i_configuration__instance`(`instance`);
ALTER TABLE `cloud`.`configuration` ADD INDEX `i_configuration__name` (`name`);
ALTER TABLE `cloud`.`configuration` ADD INDEX `i_configuration__category` (`category`);
ALTER TABLE `cloud`.`configuration` ADD INDEX `i_configuration__component` (`component`);

ALTER TABLE `cloud`.`port_forwarding_rules` ADD CONSTRAINT `fk_port_forwarding_rules__instance_id` FOREIGN KEY `fk_port_forwarding_rules__instance_id` (`instance_id`) REFERENCES `vm_instance` (`id`) ON DELETE CASCADE;

INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'agent.load.threshold', '0.70', 'Percentage (as a value between 0 and 1) of connected agents after which agent load balancing will start happening');
INSERT IGNORE INTO configuration VALUES ('Network', 'DEFAULT', 'management-server', 'network.loadbalancer.haproxy.stats.visibility', 'global', 'Load Balancer(haproxy) stats visibilty, it can be global,guest-network,disabled');
INSERT IGNORE INTO configuration VALUES ('Network', 'DEFAULT', 'management-server', 'network.loadbalancer.haproxy.stats.uri','/admin?stats','Load Balancer(haproxy) uri.');
INSERT IGNORE INTO configuration VALUES ('Network', 'DEFAULT', 'management-server', 'network.loadbalancer.haproxy.stats.auth','admin1:AdMiN123','Load Balancer(haproxy) authetication string in the format username:password');
INSERT IGNORE INTO configuration VALUES ('Network', 'DEFAULT', 'management-server', 'network.loadbalancer.haproxy.stats.port','8081','Load Balancer(haproxy) stats port number.');

