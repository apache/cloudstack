-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
-- 
--   http://www.apache.org/licenses/LICENSE-2.0
-- 
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.

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

CREATE USER cloud@`localhost` identified by 'cloud';
CREATE USER cloud@`%` identified by 'cloud';

GRANT ALL ON cloud.* to cloud@`localhost`;
GRANT ALL ON cloud.* to cloud@`%`;

GRANT process ON *.* TO cloud@`localhost`;
GRANT process ON *.* TO cloud@`%`;

commit;
