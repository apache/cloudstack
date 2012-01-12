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

account_query="GET	http://127.0.0.1/client/?command=createAccount&accounttype=0&email=simulator%40simulator.com&username=$name&firstname=first$name&lastname=last$name&password=5f4dcc3b5aa765d61d8327deb882cf99&account=$name&domainid=1	HTTP/1.1\n\n"

echo -e $account_query | nc -v -q 120 127.0.0.1 8096
