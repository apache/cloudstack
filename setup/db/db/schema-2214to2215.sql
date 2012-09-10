--;
-- Schema upgrade from 2.2.14 to 2.2.15;
--;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'direct.agent.pool.size', '1000', 'Default size for DirectAgentPool');
