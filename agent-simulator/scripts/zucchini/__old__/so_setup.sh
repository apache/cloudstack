#setup service offerings with hosttags - TAG1, TAG2, TAG3
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
#offering sizes are such that only one VM sits on each host
host=$1

./setupServiceOffering.sh -h $host -i T1 -l -m 7168 -c 1024 -n 1 -g TAG1
./setupServiceOffering.sh -h $host -i T2 -l -m 7168 -c 1024 -n 1 -g TAG2
./setupServiceOffering.sh -h $host -i T3 -l -m 7168 -c 1024 -n 1 -g TAG3
