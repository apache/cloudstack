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

 

name=$1

host_query="GET	http://10.91.30.226:8096/client/?command=addHost&zoneId=1&podId=$((name+250))&username=sim&password=sim&clustername=simulator-$name&hosttags=RP$name&url=http%3A%2F%2Fsim	HTTP/1.0\n\n"
echo -e $host_query | nc -v -q 60 10.91.30.226 8096
