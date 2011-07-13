INSERT INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'default.page.size', '500', 'Default page size for API list* commands');
DELETE FROM `cloud`.`op_host_capacity` WHERE `capacity_type` in (2,6);

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'vmware.management.portgroup', 'Management Network', 'Specify the management network name(for ESXi hosts)');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'vmware.additional.vnc.portrange.start', '59000', 'Start port number of additional VNC port range');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'vmware.additional.vnc.portrange.size', '1000', 'Start port number of additional VNC port range');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Console Proxy', 'DEFAULT', 'AgentManager', 'consoleproxy.management.state', 'Auto', 'console proxy service management state');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Console Proxy', 'DEFAULT', 'AgentManager', 'consoleproxy.management.state.last', 'Auto', 'last console proxy service management state');
