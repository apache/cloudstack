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

INSERT IGNORE INTO configuration VALUES ('Network', 'DEFAULT', 'management-server', 'network.redundantrouter', 'false', 'enable/disable redundant virtual router');
