SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ANSI';

DROP DATABASE IF EXISTS cloudbridge;
CREATE DATABASE cloudbridge;
  
GRANT ALL ON cloudbridge.* to `cloud`@`localhost` identified by 'cloud';
GRANT ALL ON cloudbridge.* to `cloud`@`%` identified by 'cloud';

GRANT process ON *.* TO `cloud`@`localhost`;
GRANT process ON *.* TO `cloud`@`%`;

COMMIT;

