SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ANSI';

DROP DATABASE IF EXISTS `cloud_usage`;

CREATE DATABASE `cloud_usage`;

GRANT ALL ON cloud_usage.* to cloud@`localhost`;
GRANT ALL ON cloud_usage.* to cloud@`%`;

GRANT process ON *.* TO cloud@`localhost`;
GRANT process ON *.* TO cloud@`%`;

commit;
