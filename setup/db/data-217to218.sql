INSERT INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'default.page.size', '500', 'Default page size for API list* commands');
delete FROM `cloud`.`op_host_capacity` where capacity_type in (2,6);
