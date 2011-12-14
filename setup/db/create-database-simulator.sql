DROP DATABASE IF EXISTS `cloud_simulator`;

CREATE DATABASE `cloud_simulator`;

GRANT ALL ON cloud_simulator.* to cloud@`localhost` identified by 'cloud';
GRANT ALL ON cloud_simulator.* to cloud@`%` identified by 'cloud';

GRANT process ON *.* TO cloud@`localhost`;
GRANT process ON *.* TO cloud@`%`;

commit;
