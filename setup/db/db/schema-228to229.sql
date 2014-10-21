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
INSERT IGNORE INTO configuration VALUES ('Network', 'DEFAULT', 'management-server', 'network.loadbalancer.haproxy.stats.visibility', 'global', 'Load Balancer(haproxy) stats visibilty, it can take the following four parameters : global,guest-network,link-local,disabled');
INSERT IGNORE INTO configuration VALUES ('Network', 'DEFAULT', 'management-server', 'network.loadbalancer.haproxy.stats.uri','/admin?stats','Load Balancer(haproxy) uri.');
INSERT IGNORE INTO configuration VALUES ('Network', 'DEFAULT', 'management-server', 'network.loadbalancer.haproxy.stats.auth','admin1:AdMiN123','Load Balancer(haproxy) authetication string in the format username:password');
INSERT IGNORE INTO configuration VALUES ('Network', 'DEFAULT', 'management-server', 'network.loadbalancer.haproxy.stats.port','8081','Load Balancer(haproxy) stats port number.');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'NetworkManager', 'use.external.dns', 'false', 'Bypass the cloudstack DHCP/DNS server vm name service, use zone external dns1 and dns2');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'network.loadbalancer.basiczone.elb.enabled', 'false', 'Whether the load balancing service is enabled for basic zones');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'network.loadbalancer.basiczone.elb.gc.interval.minutes', '120', 'Garbage collection interval to destroy unused ELB vms in minutes. Minimum of 5');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'network.loadbalancer.basiczone.elb.network', 'guest', 'Whether the elastic load balancing service public ips are taken from the public or guest network');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'network.loadbalancer.basiczone.elb.vm.cpu.mhz', '128', 'CPU speed for the elastic load balancer vm');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'network.loadbalancer.basiczone.elb.vm.ram.size', '128', 'Memory in MB for the elastic load balancer vm');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'network.loadbalancer.basiczone.elb.vm.vcpu.num', '1', 'Number of VCPU  for the elastic load balancer vm');

UPDATE `cloud`.`nics` SET strategy='Start' where reserver_name='DirectPodBasedNetworkGuru';
UPDATE `cloud`.`network_offerings` SET lb_service=1 where unique_name='System-Guest-Network';


CREATE TABLE `cloud`.`elastic_lb_vm_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `ip_addr_id` bigint unsigned NOT NULL,
  `elb_vm_id` bigint unsigned NOT NULL,
  `lb_id` bigint unsigned,
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_elastic_lb_vm_map__ip_id` FOREIGN KEY `fk_elastic_lb_vm_map__ip_id` (`ip_addr_id`) REFERENCES `user_ip_address` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_elastic_lb_vm_map__elb_vm_id` FOREIGN KEY `fk_elastic_lb_vm_map__elb_vm_id` (`elb_vm_id`) REFERENCES `vm_instance` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_elastic_lb_vm_map__lb_id` FOREIGN KEY `fk_elastic_lb_vm_map__lb_id` (`lb_id`) REFERENCES `load_balancing_rules` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


