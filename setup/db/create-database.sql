SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ANSI';

USE `mysql`;

DROP PROCEDURE IF EXISTS `mysql`.`cloud_drop_user_if_exists` ;
DELIMITER $$
CREATE PROCEDURE `mysql`.`cloud_drop_user_if_exists`()
BEGIN
  DECLARE foo BIGINT DEFAULT 0 ;
  SELECT COUNT(*)
  INTO foo
    FROM `mysql`.`user`
      WHERE `User` = 'cloud' and host = 'localhost';
  
  IF foo > 0 THEN 
         DROP USER 'cloud'@'localhost' ;
  END IF;
  
  SELECT COUNT(*)
  INTO foo
    FROM `mysql`.`user`
      WHERE `User` = 'cloud' and host = '%';
  
  IF foo > 0 THEN 
         DROP USER 'cloud'@'%' ;
  END IF;
END ;$$
DELIMITER ;

CALL `mysql`.`cloud_drop_user_if_exists`() ;

DROP PROCEDURE IF EXISTS `mysql`.`cloud_drop_users_if_exists` ;

SET SQL_MODE=@OLD_SQL_MODE ;

DROP DATABASE IF EXISTS `billing`;
DROP DATABASE IF EXISTS `cloud`;

CREATE DATABASE `cloud`;

CREATE USER cloud identified by 'cloud';

GRANT ALL ON cloud.* to cloud@`localhost` identified by 'cloud';
GRANT ALL ON cloud.* to cloud@`%` identified by 'cloud';

GRANT process ON *.* TO cloud@`localhost`;
GRANT process ON *.* TO cloud@`%`;

commit;
