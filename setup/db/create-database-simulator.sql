DROP DATABASE IF EXISTS `simulator`;

CREATE DATABASE `simulator`;

GRANT ALL ON simulator.* to cloud@`localhost` identified by 'cloud';
GRANT ALL ON simulator.* to cloud@`%` identified by 'cloud';

GRANT process ON *.* TO cloud@`localhost`;
GRANT process ON *.* TO cloud@`%`;

commit;
