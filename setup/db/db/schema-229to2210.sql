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

INSERT IGNORE INTO `cloud`.`configuration` (category, instance, name, value, description) VALUES ('Network', 'DEFAULT', 'firewall.rule.ui.enabled', 'true', 'enable/disable UI that separates firewall rules from NAT/LB rules');

