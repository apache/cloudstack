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
/* This file specifies default values that go into the database, before the Management Server is run. */

/* Root Domain */
INSERT INTO `cloud`.`domain` (id, name, parent, path, owner) VALUES (1, 'ROOT', NULL, '/', 2);

/* Configuration Table */

INSERT INTO `cloud`.`configuration` (category, instance, component, name, value, description) VALUES ('Hidden', 'DEFAULT', 'none', 'init', null, null);
-- INSERT INTO `cloud`.`configuration` (category, instance, component, name, value, description) VALUES ('Advanced', 'DEFAULT', 'AgentManager', 'xen.public.network.device', 'public-network', "[OPTIONAL]The name of the Xen network containing the physical network interface that is connected to the public network ");


