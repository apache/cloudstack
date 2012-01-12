#!/bin/bash
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



 

#_run.sh runs the agent client.

# set -x
 
while true
do
  ./_run.sh "$@" &
  wait
  ex=$?
  if [ $ex -eq 0 ] || [ $ex -eq 1 ] || [ $ex -eq 66 ] || [ $ex -gt 128 ]; then
      # permanent errors
      sleep 5
  fi

  # user stop agent by service cloud stop
  grep 'stop' /usr/local/cloud/systemvm/user_request &>/dev/null
  if [ $? -eq 0 ]; then
      timestamp=$(date)
      echo "$timestamp User stops cloud.com service" >> /var/log/cloud.log
      exit 0
  fi
  sleep 5
done
