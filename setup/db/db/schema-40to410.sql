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
-- Schema upgrade from 4.0.0 to 4.1.0;
--;

CREATE TABLE `cloud`.`s3` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(40),
  `access_key` varchar(20) NOT NULL COMMENT ' The S3 access key',
  `secret_key` varchar(40) NOT NULL COMMENT ' The S3 secret key',
  `end_point` varchar(1024) COMMENT ' The S3 host',
  `bucket` varchar(63) NOT NULL COMMENT ' The S3 host',
  `https` tinyint unsigned DEFAULT NULL COMMENT ' Flag indicating whether or not to connect over HTTPS',
  `connection_timeout` integer COMMENT ' The amount of time to wait (in milliseconds) when initially establishing a connection before giving up and timing out.',
  `max_error_retry` integer  COMMENT ' The maximum number of retry attempts for failed retryable requests (ex: 5xx error responses from services).',
  `socket_timeout` integer COMMENT ' The amount of time to wait (in milliseconds) for data to be transfered over an established, open connection before the connection times out and is closed.',
  `created` datetime COMMENT 'date the s3 first signed on',
  PRIMARY KEY (`id`),
  CONSTRAINT `uc_s3__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`template_s3_ref` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `s3_id` bigint unsigned NOT NULL COMMENT ' Associated S3 instance id',
  `template_id` bigint unsigned NOT NULL COMMENT ' Associated template id',
  `created` DATETIME NOT NULL COMMENT ' The creation timestamp',
  `size` bigint unsigned COMMENT ' The size of the object',
  `physical_size` bigint unsigned DEFAULT 0 COMMENT ' The physical size of the object',
  PRIMARY KEY (`id`),
  CONSTRAINT `uc_template_s3_ref__template_id` UNIQUE (`template_id`),
  CONSTRAINT `fk_template_s3_ref__s3_id` FOREIGN KEY `fk_template_s3_ref__s3_id` (`s3_id`) REFERENCES `s3` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_template_s3_ref__template_id` FOREIGN KEY `fk_template_s3_ref__template_id` (`template_id`) REFERENCES `vm_template` (`id`),
  INDEX `i_template_s3_ref__swift_id`(`s3_id`),
  INDEX `i_template_s3_ref__template_id`(`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 's3.enable', 'false', 'enable s3');

ALTER TABLE `cloud`.`snapshots` ADD COLUMN `s3_id` bigint unsigned COMMENT 'S3 to which this snapshot will be stored';

ALTER TABLE `cloud`.`snapshots` ADD CONSTRAINT `fk_snapshots__s3_id` FOREIGN KEY `fk_snapshots__s3_id` (`s3_id`) REFERENCES `s3` (`id`);

ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `eip_associate_public_ip` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'true if public IP is associated with user VM creation by default when EIP service is enabled.' AFTER `elastic_ip_service`;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network','DEFAULT','NetworkManager','network.dhcp.nondefaultnetwork.setgateway.guestos','Windows','The guest OS\'s name start with this fields would result in DHCP server response gateway information even when the network it\'s on is not default network. Names are separated by comma.');

ALTER TABLE `sync_queue` ADD `queue_size` SMALLINT NOT NULL DEFAULT '0' COMMENT 'number of items being processed by the queue';

ALTER TABLE `sync_queue` ADD `queue_size_limit` SMALLINT NOT NULL DEFAULT '1' COMMENT 'max number of items the queue can process concurrently';

ALTER TABLE `sync_queue_item` ADD `queue_proc_time` DATETIME NOT NULL COMMENT 'when processing started for the item' AFTER `queue_proc_number`;

ALTER TABLE upload ADD uuid VARCHAR(40);
ALTER TABLE async_job modify job_cmd VARCHAR(255);

-- populate uuid column with db id if uuid is null
UPDATE `cloud`.`account` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`alert` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`async_job` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`cluster` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`data_center` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`disk_offering` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`domain` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`event` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`external_firewall_devices` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`external_load_balancer_devices` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`external_nicira_nvp_devices` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`firewall_rules` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`guest_os` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`guest_os_category` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`host` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`host_pod_ref` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`hypervisor_capabilities` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`instance_group` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`load_balancer_stickiness_policies` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`network_external_firewall_device_map` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`network_external_lb_device_map` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`network_offerings` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`networks` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`nics` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`physical_network` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`physical_network_service_providers` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`physical_network_traffic_types` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`port_profile` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`project_invitations` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`projects` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`resource_tags` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`s2s_customer_gateway` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`s2s_vpn_connection` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`s2s_vpn_gateway` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`security_group` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`security_group_rule` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`snapshot_schedule` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`snapshots` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`static_routes` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`storage_pool` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`swift` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`upload` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`user` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`user_ip_address` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`user_vm_temp` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`virtual_router_providers` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`virtual_supervisor_module` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`vlan` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`vm_instance` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`vm_template` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`vpc` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`vpc_gateways` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`vpc_offerings` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`vpn_users` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`volumes` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`autoscale_vmgroups` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`autoscale_vmprofiles` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`autoscale_policies` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`counter` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`conditions` set uuid=id WHERE uuid is NULL;


--- DB views for list api ---
DROP VIEW IF EXISTS `cloud`.`user_vm_view`;
CREATE VIEW `cloud`.`user_vm_view` AS
select
vm_instance.id id,
vm_instance.name name,
user_vm.display_name display_name,
user_vm.user_data user_data,
account.id account_id,
account.uuid account_uuid,
account.account_name account_name,
account.type account_type,
domain.id domain_id,
domain.uuid domain_uuid,
domain.name domain_name,
domain.path domain_path,
projects.id project_id,
projects.uuid project_uuid,
projects.name project_name,
instance_group.id instance_group_id,
instance_group.uuid instance_group_uuid,
instance_group.name instance_group_name,
vm_instance.uuid uuid,
vm_instance.last_host_id last_host_id,
vm_instance.vm_type type,
vm_instance.vnc_password vnc_password,
vm_instance.limit_cpu_use limit_cpu_use,
vm_instance.created created,
vm_instance.state state,
vm_instance.removed removed,
vm_instance.ha_enabled ha_enabled,
vm_instance.hypervisor_type hypervisor_type,
vm_instance.instance_name instance_name,
vm_instance.guest_os_id guest_os_id,
guest_os.uuid guest_os_uuid,
vm_instance.pod_id pod_id,
host_pod_ref.uuid pod_uuid,
vm_instance.private_ip_address private_ip_address,
vm_instance.private_mac_address private_mac_address,
vm_instance.vm_type vm_type,
data_center.id data_center_id,
data_center.uuid data_center_uuid,
data_center.name data_center_name,
data_center.is_security_group_enabled security_group_enabled,
host.id host_id,
host.uuid host_uuid,
host.name host_name,
vm_template.id template_id,
vm_template.uuid template_uuid,
vm_template.name template_name,
vm_template.display_text template_display_text,
vm_template.enable_password password_enabled,
iso.id iso_id,
iso.uuid iso_uuid,
iso.name iso_name,
iso.display_text iso_display_text,
service_offering.id service_offering_id,
disk_offering.uuid service_offering_uuid,
service_offering.cpu cpu,
service_offering.speed speed,
service_offering.ram_size ram_size,
disk_offering.name service_offering_name,
storage_pool.id pool_id,
storage_pool.uuid pool_uuid,
storage_pool.pool_type pool_type,
volumes.id volume_id,
volumes.uuid volume_uuid,
volumes.device_id volume_device_id,
volumes.volume_type volume_type,
security_group.id security_group_id,
security_group.uuid security_group_uuid,
security_group.name security_group_name,
security_group.description security_group_description,
nics.id nic_id,
nics.uuid nic_uuid,
nics.network_id network_id,
nics.ip4_address ip_address,
nics.default_nic is_default_nic,
nics.gateway gateway,
nics.netmask netmask,
nics.mac_address mac_address,
nics.broadcast_uri broadcast_uri,
nics.isolation_uri isolation_uri,
vpc.id vpc_id,
vpc.uuid vpc_uuid,
networks.uuid network_uuid,
networks.traffic_type traffic_type,
networks.guest_type guest_type,
user_ip_address.id public_ip_id,
user_ip_address.uuid public_ip_uuid,
user_ip_address.public_ip_address public_ip_address,
ssh_keypairs.keypair_name keypair_name,
resource_tags.id tag_id,
resource_tags.uuid tag_uuid,
resource_tags.key tag_key,
resource_tags.value tag_value,
resource_tags.domain_id tag_domain_id,
resource_tags.account_id tag_account_id,
resource_tags.resource_id tag_resource_id,
resource_tags.resource_uuid tag_resource_uuid,
resource_tags.resource_type tag_resource_type,
resource_tags.customer tag_customer,
async_job.id job_id,
async_job.uuid job_uuid,
async_job.job_status job_status,
async_job.account_id job_account_id
from user_vm
inner join vm_instance on vm_instance.id = user_vm.id and vm_instance.removed is NULL
inner join account on vm_instance.account_id=account.id
inner join domain on vm_instance.domain_id=domain.id
left join guest_os on vm_instance.guest_os_id = guest_os.id
left join host_pod_ref on vm_instance.pod_id = host_pod_ref.id
left join projects on projects.project_account_id = account.id
left join instance_group_vm_map on vm_instance.id=instance_group_vm_map.instance_id
left join instance_group on instance_group_vm_map.group_id=instance_group.id
left join data_center on vm_instance.data_center_id=data_center.id
left join host on vm_instance.host_id=host.id
left join vm_template on vm_instance.vm_template_id=vm_template.id
left join vm_template iso on iso.id=user_vm.iso_id
left join service_offering on vm_instance.service_offering_id=service_offering.id
left join disk_offering  on vm_instance.service_offering_id=disk_offering.id
left join volumes on vm_instance.id=volumes.instance_id
left join storage_pool on volumes.pool_id=storage_pool.id
left join security_group_vm_map on vm_instance.id=security_group_vm_map.instance_id
left join security_group on security_group_vm_map.security_group_id=security_group.id
left join nics on vm_instance.id=nics.instance_id
left join networks on nics.network_id=networks.id
left join vpc on networks.vpc_id = vpc.id
left join user_ip_address on user_ip_address.vm_id=vm_instance.id
left join user_vm_details on user_vm_details.vm_id=vm_instance.id and user_vm_details.name = "SSH.PublicKey"
left join ssh_keypairs on ssh_keypairs.public_key = user_vm_details.value
left join resource_tags on resource_tags.resource_id = vm_instance.id and resource_tags.resource_type = "UserVm"
left join async_job on async_job.instance_id = vm_instance.id and async_job.instance_type = "VirtualMachine" and async_job.job_status = 0;

DROP VIEW IF EXISTS `cloud`.`domain_router_view`;
CREATE VIEW domain_router_view AS
select
vm_instance.id id,
vm_instance.name name,
account.id account_id,
account.uuid account_uuid,
account.account_name account_name,
account.type account_type,
domain.id domain_id,
domain.uuid domain_uuid,
domain.name domain_name,
domain.path domain_path,
projects.id project_id,
projects.uuid project_uuid,
projects.name project_name,
vm_instance.uuid uuid,
vm_instance.created created,
vm_instance.state state,
vm_instance.removed removed,
vm_instance.pod_id pod_id,
vm_instance.instance_name instance_name,
host_pod_ref.uuid pod_uuid,
data_center.id data_center_id,
data_center.uuid data_center_uuid,
data_center.name data_center_name,
data_center.dns1 dns1,
data_center.dns2 dns2,
host.id host_id,
host.uuid host_uuid,
host.name host_name,
vm_template.id template_id,
vm_template.uuid template_uuid,
service_offering.id service_offering_id,
disk_offering.uuid service_offering_uuid,
disk_offering.name service_offering_name,
nics.id nic_id,
nics.uuid nic_uuid,
nics.network_id network_id,
nics.ip4_address ip_address,
nics.default_nic is_default_nic,
nics.gateway gateway,
nics.netmask netmask,
nics.mac_address mac_address,
nics.broadcast_uri broadcast_uri,
nics.isolation_uri isolation_uri,
vpc.id vpc_id,
vpc.uuid vpc_uuid,
networks.uuid network_uuid,
networks.name network_name,
networks.network_domain network_domain,
networks.traffic_type traffic_type,
networks.guest_type guest_type,
async_job.id job_id,
async_job.uuid job_uuid,
async_job.job_status job_status,
async_job.account_id job_account_id,
domain_router.template_version template_version,
domain_router.scripts_version scripts_version,
domain_router.is_redundant_router is_redundant_router,
domain_router.redundant_state redundant_state,
domain_router.stop_pending stop_pending
from domain_router
inner join vm_instance on vm_instance.id = domain_router.id
inner join account on vm_instance.account_id=account.id
inner join domain on vm_instance.domain_id=domain.id
left join host_pod_ref on vm_instance.pod_id = host_pod_ref.id
left join projects on projects.project_account_id = account.id
left join data_center on vm_instance.data_center_id=data_center.id
left join host on vm_instance.host_id=host.id
left join vm_template on vm_instance.vm_template_id=vm_template.id
left join service_offering on vm_instance.service_offering_id=service_offering.id
left join disk_offering  on vm_instance.service_offering_id=disk_offering.id
left join volumes on vm_instance.id=volumes.instance_id
left join storage_pool on volumes.pool_id=storage_pool.id
left join nics on vm_instance.id=nics.instance_id
left join networks on nics.network_id=networks.id
left join vpc on networks.vpc_id = vpc.id
left join async_job on async_job.instance_id = vm_instance.id and async_job.instance_type = "DomainRouter" and async_job.job_status = 0;

DROP VIEW IF EXISTS `cloud`.`security_group_view`;
CREATE VIEW security_group_view AS
select
security_group.id id,
security_group.name name,
security_group.description description,
security_group.uuid uuid,
account.id account_id,
account.uuid account_uuid,
account.account_name account_name,
account.type account_type,
domain.id domain_id,
domain.uuid domain_uuid,
domain.name domain_name,
domain.path domain_path,
projects.id project_id,
projects.uuid project_uuid,
projects.name project_name,
security_group_rule.id rule_id,
security_group_rule.uuid rule_uuid,
security_group_rule.type rule_type,
security_group_rule.start_port rule_start_port,
security_group_rule.end_port rule_end_port,
security_group_rule.protocol rule_protocol,
security_group_rule.allowed_network_id rule_allowed_network_id,
security_group_rule.allowed_ip_cidr rule_allowed_ip_cidr,
security_group_rule.create_status rule_create_status,
resource_tags.id tag_id,
resource_tags.uuid tag_uuid,
resource_tags.key tag_key,
resource_tags.value tag_value,
resource_tags.domain_id tag_domain_id,
resource_tags.account_id tag_account_id,
resource_tags.resource_id tag_resource_id,
resource_tags.resource_uuid tag_resource_uuid,
resource_tags.resource_type tag_resource_type,
resource_tags.customer tag_customer,
async_job.id job_id,
async_job.uuid job_uuid,
async_job.job_status job_status,
async_job.account_id job_account_id
from security_group
left join security_group_rule on security_group.id = security_group_rule.security_group_id
inner join account on security_group.account_id=account.id
inner join domain on security_group.domain_id=domain.id
left join projects on projects.project_account_id = security_group.account_id
left join resource_tags on resource_tags.resource_id = security_group.id and resource_tags.resource_type = "SecurityGroup"
left join async_job on async_job.instance_id = security_group.id and async_job.instance_type = "SecurityGroup" and async_job.job_status = 0;

DROP VIEW IF EXISTS `cloud`.`resource_tag_view`;
CREATE VIEW resource_tag_view AS
select
resource_tags.id,
resource_tags.uuid,
resource_tags.key,
resource_tags.value,
resource_tags.resource_id,
resource_tags.resource_uuid,
resource_tags.resource_type,
resource_tags.customer,
account.id account_id,
account.uuid account_uuid,
account.account_name account_name,
account.type account_type,
domain.id domain_id,
domain.uuid domain_uuid,
domain.name domain_name,
domain.path domain_path,
projects.id project_id,
projects.uuid project_uuid,
projects.name project_name
from resource_tags
inner join account on resource_tags.account_id=account.id
inner join domain on resource_tags.domain_id=domain.id
left join projects on projects.project_account_id = resource_tags.account_id;


DROP VIEW IF EXISTS `cloud`.`event_view`;
CREATE VIEW event_view AS
select
event.id,
event.uuid,
event.type,
event.state,
event.description,
event.created,
event.level,
event.parameters,
event.start_id,
eve.uuid start_uuid,
event.user_id,
user.username user_name,
account.id account_id,
account.uuid account_uuid,
account.account_name account_name,
account.type account_type,
domain.id domain_id,
domain.uuid domain_uuid,
domain.name domain_name,
domain.path domain_path,
projects.id project_id,
projects.uuid project_uuid,
projects.name project_name
from event
inner join account on event.account_id=account.id
inner join domain on event.domain_id=domain.id
inner join user on event.user_id = user.id
left join projects on projects.project_account_id = event.account_id
left join event eve on event.start_id = eve.id;

DROP VIEW IF EXISTS `cloud`.`instance_group_view`;
CREATE VIEW instance_group_view AS
select
instance_group.id,
instance_group.uuid,
instance_group.name,
instance_group.removed,
instance_group.created,
account.id account_id,
account.uuid account_uuid,
account.account_name account_name,
account.type account_type,
domain.id domain_id,
domain.uuid domain_uuid,
domain.name domain_name,
domain.path domain_path,
projects.id project_id,
projects.uuid project_uuid,
projects.name project_name
from instance_group
inner join account on instance_group.account_id=account.id
inner join domain on account.domain_id=domain.id
left join projects on projects.project_account_id = instance_group.account_id;

DROP VIEW IF EXISTS `cloud`.`user_view`;
CREATE VIEW user_view AS
select
user.id,
user.uuid,
user.username,
user.password,
user.firstname,
user.lastname,
user.email,
user.state,
user.api_key,
user.secret_key,
user.created,
user.removed,
user.timezone,
user.registration_token,
user.is_registered,
user.incorrect_login_attempts,
account.id account_id,
account.uuid account_uuid,
account.account_name account_name,
account.type account_type,
domain.id domain_id,
domain.uuid domain_uuid,
domain.name domain_name,
domain.path domain_path
from user
inner join account on user.account_id = account.id
inner join domain on account.domain_id=domain.id;

DROP VIEW IF EXISTS `cloud`.`project_view`;
CREATE VIEW project_view AS
select
projects.id,
projects.uuid,
projects.name,
projects.display_text,
projects.state,
projects.removed,
projects.created,
account.account_name owner,
pacct.account_id,
domain.id domain_id,
domain.uuid domain_uuid,
domain.name domain_name,
domain.path domain_path,
resource_tags.id tag_id,
resource_tags.uuid tag_uuid,
resource_tags.key tag_key,
resource_tags.value tag_value,
resource_tags.domain_id tag_domain_id,
resource_tags.account_id tag_account_id,
resource_tags.resource_id tag_resource_id,
resource_tags.resource_uuid tag_resource_uuid,
resource_tags.resource_type tag_resource_type,
resource_tags.customer tag_customer
from projects
inner join domain on projects.domain_id=domain.id
inner join project_account on projects.id = project_account.project_id and project_account.account_role = "Admin"
inner join account on account.id = project_account.account_id
left join resource_tags on resource_tags.resource_id = projects.id and resource_tags.resource_type = "Project"
left join project_account pacct on projects.id = pacct.project_id;

DROP VIEW IF EXISTS `cloud`.`project_account_view`;
CREATE VIEW project_account_view AS
select
project_account.id,
account.id account_id,
account.uuid account_uuid,
account.account_name,
account.type account_type,
project_account.account_role,
projects.id project_id,
projects.uuid project_uuid,
projects.name project_name,
domain.id domain_id,
domain.uuid domain_uuid,
domain.name domain_name,
domain.path domain_path
from project_account
inner join account on project_account.account_id = account.id
inner join domain on account.domain_id=domain.id
inner join projects on projects.id = project_account.project_id;

DROP VIEW IF EXISTS `cloud`.`project_invitation_view`;
CREATE VIEW project_invitation_view AS
select
project_invitations.id,
project_invitations.uuid,
project_invitations.email,
project_invitations.created,
project_invitations.state,
projects.id project_id,
projects.uuid project_uuid,
projects.name project_name,
account.id account_id,
account.uuid account_uuid,
account.account_name,
account.type account_type,
domain.id domain_id,
domain.uuid domain_uuid,
domain.name domain_name,
domain.path domain_path
from project_invitations
left join account on project_invitations.account_id = account.id
left join domain on project_invitations.domain_id=domain.id
left join projects on projects.id = project_invitations.project_id;

DROP VIEW IF EXISTS `cloud`.`host_view`;
CREATE VIEW host_view AS
select 
host.id,
host.uuid,
host.name,
host.status,
host.disconnected,
host.type,
host.private_ip_address,
host.version,
host.hypervisor_type,
host.hypervisor_version,
host.capabilities,
host.last_ping,
host.created,
host.removed,
host.resource_state,
host.mgmt_server_id,
host.cpus,
host.speed,
host.ram,
cluster.id cluster_id,
cluster.uuid cluster_uuid,
cluster.name cluster_name,
cluster.cluster_type,
data_center.id data_center_id, 
data_center.uuid data_center_uuid,
data_center.name data_center_name, 
host_pod_ref.id pod_id, 
host_pod_ref.uuid pod_uuid,
host_pod_ref.name pod_name,
host_tags.tag,
guest_os_category.id guest_os_category_id,
guest_os_category.uuid guest_os_category_uuid,
guest_os_category.name guest_os_category_name,
mem_caps.used_capacity memory_used_capacity,
mem_caps.reserved_capacity memory_reserved_capacity,
cpu_caps.used_capacity cpu_used_capacity,
cpu_caps.reserved_capacity cpu_reserved_capacity,
async_job.id job_id,
async_job.uuid job_uuid,
async_job.job_status job_status,
async_job.account_id job_account_id
from host 
left join cluster on host.cluster_id = cluster.id
left join data_center on host.data_center_id = data_center.id
left join host_pod_ref on host.pod_id = host_pod_ref.id
left join host_details on host.id = host_details.id and host_details.name = "guest.os.category.id"
left join guest_os_category on guest_os_category.id = CONVERT( host_details.value, UNSIGNED )
left join host_tags on host_tags.host_id = host.id
left join op_host_capacity mem_caps on host.id = mem_caps.host_id and mem_caps.capacity_type = 0
left join op_host_capacity cpu_caps on host.id = cpu_caps.host_id and cpu_caps.capacity_type = 1
left join async_job on async_job.instance_id = host.id and async_job.instance_type = "Host" and async_job.job_status = 0;

DROP VIEW IF EXISTS `cloud`.`volume_view`;
CREATE VIEW volume_view AS
select 
volumes.id,
volumes.uuid,
volumes.name,
volumes.device_id,
volumes.volume_type,
volumes.size,
volumes.created,
volumes.state,
volumes.attached,
volumes.removed,
volumes.pod_id,
account.id account_id, 
account.uuid account_uuid,
account.account_name account_name,
account.type account_type,
domain.id domain_id, 
domain.uuid domain_uuid,
domain.name domain_name, 
domain.path domain_path,
projects.id project_id,
projects.uuid project_uuid,
projects.name project_name,
data_center.id data_center_id, 
data_center.uuid data_center_uuid,
data_center.name data_center_name, 
vm_instance.id vm_id,
vm_instance.uuid vm_uuid,
vm_instance.name vm_name,
vm_instance.state vm_state,
vm_instance.vm_type,
user_vm.display_name vm_display_name,
volume_host_ref.size volume_host_size,
volume_host_ref.created volume_host_created,
volume_host_ref.format,
volume_host_ref.download_pct,
volume_host_ref.download_state,
volume_host_ref.error_str,
disk_offering.id disk_offering_id,
disk_offering.uuid disk_offering_uuid,
disk_offering.name disk_offering_name,
disk_offering.display_text disk_offering_display_text,
disk_offering.use_local_storage,
disk_offering.system_use,
storage_pool.id pool_id,
storage_pool.uuid pool_uuid,
storage_pool.name pool_name,
cluster.hypervisor_type,
vm_template.id template_id,
vm_template.uuid template_uuid,
vm_template.extractable,
vm_template.type template_type,
resource_tags.id tag_id, 
resource_tags.uuid tag_uuid,
resource_tags.key tag_key,
resource_tags.value tag_value,
resource_tags.domain_id tag_domain_id, 
resource_tags.account_id tag_account_id, 
resource_tags.resource_id tag_resource_id, 
resource_tags.resource_uuid tag_resource_uuid, 
resource_tags.resource_type tag_resource_type, 
resource_tags.customer tag_customer,
async_job.id job_id,
async_job.uuid job_uuid,
async_job.job_status job_status,
async_job.account_id job_account_id
from volumes 
inner join account on volumes.account_id=account.id 
inner join domain on volumes.domain_id=domain.id
left join projects on projects.project_account_id = account.id
left join data_center on volumes.data_center_id = data_center.id
left join vm_instance on volumes.instance_id = vm_instance.id
left join user_vm on user_vm.id = vm_instance.id
left join volume_host_ref on volumes.id = volume_host_ref.volume_id and volumes.data_center_id = volume_host_ref.zone_id
left join disk_offering on volumes.disk_offering_id = disk_offering.id
left join storage_pool on volumes.pool_id = storage_pool.id
left join cluster on storage_pool.cluster_id = cluster.id
left join vm_template on volumes.template_id = vm_template.id
left join resource_tags on resource_tags.resource_id = volumes.id and resource_tags.resource_type = "Volume"
left join async_job on async_job.instance_id = volumes.id and async_job.instance_type = "Volume" and async_job.job_status = 0;


