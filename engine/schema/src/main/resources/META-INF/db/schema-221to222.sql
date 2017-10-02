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

update firewall_rules set purpose='StaticNat' where is_static_nat=1;
alter table user_ip_address add CONSTRAINT `fk_user_ip_address__vm_id` FOREIGN KEY (`vm_id`) REFERENCES `vm_instance`(`id`);
update network_offerings set system_only=1 where name='System-Guest-Network';
update network_offerings set dns_service=1,userdata_service=1,dhcp_service=1 where system_only=0;
update network_offerings set firewall_service=1, lb_service=1,vpn_service=1,gateway_service=1 where traffic_type='guest' and system_only=0;
alter table domain add column `state` char(32) NOT NULL default 'Active' COMMENT 'state of the domain';
alter table nics add column `vm_type` char(32);
update nics set vm_type=(select type from vm_instance where vm_instance.id=nics.instance_id);
INSERT INTO configuration (`category`, `instance`, `component`, `name`, `value`, `description`) VALUES ('Network','DEFAULT','none','network.guest.cidr.limit','22','size limit for guest cidr; cant be less than this value'); 
alter table user_statistics add column `network_id` bigint unsigned;
update op_networks set nics_count=(nics_count-1) where id in (select d.network_id from domain_router d, vm_instance i where i.state='Running' and i.id=d.id);
update network_offerings set traffic_type='Guest' where system_only=0;
alter table network_offerings add column `guest_type` char(32);
update network_offerings set guest_type='Direct' where id=5;
update network_offerings set guest_type='Virtual' where id=6;
update network_offerings set guest_type='Direct' where id=7;
alter table op_it_work add column `vm_type` char(32) NOT NULL;
update op_it_work set vm_type=(select type from vm_instance where vm_instance.id=op_it_work.instance_id);
alter table networks add column `is_security_group_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '1: enabled, 0: not';
update networks set is_security_group_enabled=0;
alter table data_center add column `is_security_group_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '1: enabled, 0: not';
update data_center set is_security_group_enabled=0;
update data_center set dns_provider='DhcpServer', dhcp_provider='DhcpServer', userdata_provider='DhcpServer', lb_provider=null, firewall_provider=null, vpn_provider=null, gateway_provider=null where networktype='Basic';
update network_offerings set specify_vlan=1 where system_only=0 and guest_type='Direct';
update networks set traffic_type='Guest' where network_offering_id in (select id from network_offerings where system_only=0);
update network_offerings set availability='Optional' where id=7;
delete from configuration where name='router.cleanup.interval';
INSERT INTO configuration (category, instance, component, name, value, description)
    VALUES ('Advanced', 'DEFAULT', 'management-server', 'management.network.cidr', NULL, 'The cidr of management server network');
CREATE TABLE IF NOT EXISTS `cloud`.`version` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id',
  `version` char(40) NOT NULL UNIQUE COMMENT 'version',
  `updated` datetime NOT NULL COMMENT 'Date this version table was updated',
  `step` char(32) NOT NULL COMMENT 'Step in the upgrade to this version',
  PRIMARY KEY (`id`),
  INDEX `i_version__version`(`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


