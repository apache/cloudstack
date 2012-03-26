# Copyright (C) 2012 Citrix Systems, Inc.  All rights reserved
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


#Schema upgrade from 3.0 to 3.0.1;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'max.project.networks', '20', 'The default maximum number of networks that can be created for a project');


INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Account Defaults', 'DEFAULT', 'management-server', 'max.account.networks', '20', 'The default maximum number of networks that can be created for an account');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Account Defaults', 'DEFAULT', 'management-server', 'max.account.networks', '20', 'The default maximum number of networks that can be created for an account');

UPDATE snapshots SET removed=now() WHERE removed IS NULL AND sechost_id IN (SELECT id FROM host WHERE type='SecondaryStorage' AND removed IS NOT NULL);

ALTER TABLE `cloud_usage`.`usage_ip_address` MODIFY COLUMN `is_system` smallint(1) NOT NULL default '0';
