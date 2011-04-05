--;
-- Schema upgrade from 2.2.4 to 2.2.5;
--;
INSERT INTO `cloud`.`configuration` (category, instance, component, name, value, description) VALUES ('Network', 'DEFAULT', 'management-server', 'vm.network.throttling.rate', 200, 'Default data transfer rate in megabits per second allowed in user vm\'s default network.');
