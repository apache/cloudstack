# Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
#     
# This software is licensed under the GNU General Public License v3 or later.
# 
# It is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or any later version.
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
# 
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
END $$
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
