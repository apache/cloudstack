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


spool_query="GET        http://127.0.0.1/client/?command=createStoragePool&zoneId=1&podId=1&clusterid=1&name=SPOOL&url=nfs://10.91.25.6/export/share/1   HTTP/1.0\n\n"
echo -e $spool_query | nc -v -q 60 127.0.0.1 8096
