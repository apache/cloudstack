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
-- Schema upgrade from 2.2.9 to 2.2.10;
--;

ALTER TABLE `cloud`.`account` ADD COLUMN `network_domain` varchar(255);
ALTER TABLE `cloud`.`domain` ADD COLUMN `network_domain` varchar(255);

INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'NetworkManager', 'use.external.dns', 'false', 'Bypass internal dns, use exetrnal dns1 and dns2');

ALTER TABLE `cloud`.`domain_router` ADD COLUMN `is_redundant_router` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'if in redundant router mode';
ALTER TABLE `cloud`.`domain_router` ADD COLUMN `priority` int(4) unsigned COMMENT 'priority of router in the redundant router mode';
ALTER TABLE `cloud`.`domain_router` ADD COLUMN `redundant_state` varchar(64) NOT NULL DEFAULT 'UNKNOWN' COMMENT 'the state of redundant virtual router';

ALTER TABLE `cloud`.`cluster` ADD COLUMN  `managed_state` varchar(32) NOT NULL DEFAULT 'Managed' COMMENT 'Is this cluster managed by cloudstack';

ALTER TABLE `cloud`.`host` MODIFY `storage_ip_address` char(40);

INSERT IGNORE INTO configuration VALUES ('Network', 'DEFAULT', 'management-server', 'network.redundantrouter', 'false', 'enable/disable redundant virtual router');
INSERT IGNORE INTO configuration VALUES ('Storage', 'DEFAULT', 'management-server', 'storage.pool.max.waitseconds', '3600', 'Timeout (in seconds) to synchronize storage pool operations.');
INSERT IGNORE INTO configuration VALUES ('Storage', 'DEFAULT', 'management-server', 'storage.template.cleanup.enabled', 'true', 'Enable/disable template cleanup activity, only take effect when overall storage cleanup is enabled');

ALTER TABLE `cloud`.`firewall_rules` ADD COLUMN `icmp_code` int(10) COMMENT 'The ICMP code (if protocol=ICMP). A value of -1 means all codes for the given ICMP type.';
ALTER TABLE `cloud`.`firewall_rules` ADD COLUMN `icmp_type` int(10) COMMENT 'The ICMP type (if protocol=ICMP). A value of -1 means all types.';
ALTER TABLE `cloud`.`firewall_rules` ADD COLUMN `related` bigint unsigned COMMENT 'related to what other firewall rule';
ALTER TABLE `cloud`.`firewall_rules` ADD CONSTRAINT `fk_firewall_rules__related` FOREIGN KEY(`related`) REFERENCES `firewall_rules`(`id`) ON DELETE CASCADE;

ALTER TABLE `cloud`.`firewall_rules` MODIFY `start_port` int(10) COMMENT 'starting port of a port range';
ALTER TABLE `cloud`.`firewall_rules` MODIFY `end_port` int(10) COMMENT 'end port of a port range';

INSERT IGNORE INTO `cloud`.`configuration` (category, instance, name, value, description) VALUES ('Network', 'DEFAULT', 'firewall.rule.ui.enabled', 'false', 'enable/disable UI that separates firewall rules from NAT/LB rules');


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

INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'vmware.reserve.mem', 'false', 'Specify whether or not to reserve memory based on memory overprovisioning factor');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'mem.overprovisioning.factor', '1', 'Used for memory overprovisioning calculation');

INSERT IGNORE INTO configuration VALUES ('Network', 'DEFAULT', 'AgentManager', 'remote.access.vpn.psk.length', '24', 'The length of the ipsec preshared key (minimum 8, maximum 256)');
INSERT IGNORE INTO configuration VALUES ('Network', 'DEFAULT', 'AgentManager', 'remote.access.vpn.client.iprange', '10.1.2.1-10.1.2.8', 'The range of ips to be allocated to remote access vpn clients. The first ip in the range is used by the VPN server');
INSERT IGNORE INTO configuration VALUES ('Network', 'DEFAULT', 'AgentManager', 'remote.access.vpn.user.limit', '8', 'The maximum number of VPN users that can be created per account');

CREATE TABLE IF NOT exists `cloud`.`elastic_lb_vm_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `ip_addr_id` bigint unsigned NOT NULL,
  `elb_vm_id` bigint unsigned NOT NULL,
  `lb_id` bigint unsigned,
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_elastic_lb_vm_map__ip_id` FOREIGN KEY `fk_elastic_lb_vm_map__ip_id` (`ip_addr_id`) REFERENCES `user_ip_address` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_elastic_lb_vm_map__elb_vm_id` FOREIGN KEY `fk_elastic_lb_vm_map__elb_vm_id` (`elb_vm_id`) REFERENCES `vm_instance` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_elastic_lb_vm_map__lb_id` FOREIGN KEY `fk_elastic_lb_vm_map__lb_id` (`lb_id`) REFERENCES `load_balancing_rules` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

UPDATE `cloud`.`network_offerings` SET lb_service=1 where unique_name='System-Guest-Network';
