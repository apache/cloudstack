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

-- Schema upgrade from 3.0.1 to 3.0.2;

DELETE FROM `cloud`.`configuration` WHERE name='consoleproxy.cpu.mhz';
DELETE FROM `cloud`.`configuration` WHERE name='secstorage.vm.cpu.mhz';
DELETE FROM `cloud`.`configuration` WHERE name='consoleproxy.ram.size';
DELETE FROM `cloud`.`configuration` WHERE name='secstorage.vm.ram.size';

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'consoleproxy.service.offering', NULL, 'Service offering used by console proxy; if NULL - system offering will be used');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'secstorage.service.offering', NULL, 'Service offering used by secondary storage; if NULL - system offering will be used');

UPDATE `cloud`.`network_offerings` SET display_text='Offering for Isolated networks with Source Nat service enabled' WHERE name='DefaultIsolatedNetworkOfferingWithSourceNatService' and `cloud`.`network_offerings`.default=1;
UPDATE `cloud`.`network_offerings` SET display_text='Offering for Isolated networks with no Source Nat service' WHERE name='DefaultIsolatedNetworkOffering' and `cloud`.`network_offerings`.default=1;
UPDATE `cloud`.`network_offerings` SET display_text='Offering for Shared networks' WHERE name='DefaultSharedNetworkOffering' and `cloud`.`network_offerings`.default=1;

INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled) VALUES ('XenServer', '6.0.2', 50, 1);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled) VALUES ('VMware', '5.0', 128, 0);


INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'direct.agent.load.size', '16', 'The number of direct agents to load each time');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network', 'DEFAULT', 'management-server', 'direct.network.no.default.route', 'false', 'Direct Network Dhcp Server should not send a default route');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'endpointe.url', 'http://localhost:8080/client/api', 'Endpointe Url');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'extract.url.expiration.interval', '14400', 'The life of an extract URL after which it is deleted');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Hidden', 'DEFAULT', 'management-server', 'kvm.guest.network.device', null, 'Specify the private bridge on host for private network');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network', 'DEFAULT', 'management-server', 'network.disable.rpfilter', 'true', 'disable rp_filter on Domain Router VM public interfaces.');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network', 'DEFAULT', 'management-server', 'network.securitygroups.work.cleanup.interval', '120', 'Time interval (seconds) in which finished work is cleaned up from the work table');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network', 'DEFAULT', 'management-server', 'network.securitygroups.work.lock.timeout', '300', 'Lock wait timeout (seconds) while updating the security group work queues');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network', 'DEFAULT', 'management-server', 'network.securitygroups.work.per.agent.queue.size', '100', 
'The number of outstanding security group work items that can be queued to a host. If exceeded, work items will get dropped to conserve memory. Security Group Sync will take care of ensuring that the host gets updated eventually');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network', 'DEFAULT', 'management-server', 'network.securitygroups.workers.pool.size', '50', 'Number of worker threads processing the security group update work queue');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Hidden', 'DEFAULT', 'management-server', 'ovm.guest.network.device', null, 'Specify the private bridge on host for private network');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Hidden', 'DEFAULT', 'management-server', 'ovm.private.network.device', null, 'Specify the private bridge on host for private network');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Hidden', 'DEFAULT', 'management-server', 'ovm.public.network.device', null, 'Specify the public bridge on host for public network');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'router.extra.public.nics', '2', 'specify extra public nics used for virtual router(up to 5)');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'secstorage.capacity.standby', '10', 'The minimal number of command execution sessions that system is able to serve immediately(standby capacity)');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'secstorage.cmd.execution.time.max', '30', 'The max command execution time in minute');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'secstorage.session.max', '50', 'The max number of command execution sessions that a SSVM can handle');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Storage', 'DEFAULT', 'management-server', 'storage.max.volume.size', '2000', 'The maximum size for a volume (in GB).');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'task.cleanup.retry.interval', '600', 'Time (in seconds) to wait before retrying cleanup of tasks if the cleanup failed previously.  0 means to never retry.');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'vmware.additional.vnc.portrange.start', '50000', 'Start port number of additional VNC port range');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'vmware.percluster.host.max', '8', 'maxmium hosts per vCenter cluster(do not let it grow over 8)');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'vmware.reserve.cpu', 'false', 'Specify whether or not to reserve CPU based on CPU overprovisioning factor');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'vmware.reserve.mem', 'false', 'Specify whether or not to reserve memory based on memory overprovisioning factor');

UPDATE `cloud`.`storage_pool` SET removed=now() WHERE path='lvm' AND id NOT IN (select pool_id from storage_pool_host_ref);

UPDATE `cloud`.`user` SET PASSWORD=RAND() WHERE id=1;
