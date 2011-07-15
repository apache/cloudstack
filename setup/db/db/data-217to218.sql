INSERT INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'default.page.size', '500', 'Default page size for API list* commands');
DELETE FROM `cloud`.`op_host_capacity` WHERE `capacity_type` in (2,6);

