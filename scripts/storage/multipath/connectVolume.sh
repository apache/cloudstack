#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

#####################################################################################
#
# Given a lun # and a WWID for a volume provisioned externally, find the volume
# through the SCSI bus and make sure its visable via multipath
#
#####################################################################################


LUN=${1:?"LUN required"}
WWID=${2:?"WWID required"}

WWID=$(echo $WWID | tr '[:upper:]' '[:lower:]')

START_CONNECT=$(dirname $0)/startConnect.sh
if [ -x "${START_CONNECT}" ]; then
   echo "$(date): Starting connect process for ${WWID} on lun ${LUN}"
   ${START_CONNECT} ${LUN} ${WWID}
   if [ $? -ne 0 ]; then
      echo "$(date): Failed to start connect process for ${WWID} on lun ${LUN}"
      logger -t "CS_SCSI_VOL_FIND" "${WWID} failed to start connect process on lun ${LUN}"
      exit 1
   fi
else
   echo "$(date): Unable to find startConnect.sh script!"
   exit 1
fi

// wait for the device path to show up
while [ ! -e /dev/mapper/3${WWID} ]; do
   echo "$(date): Waiting for /dev/mapper/3${WWID} to appear"
   sleep
done

FINISH_CONNECT=$(dirname $0)/finishConnect.sh
if [ -x "${FINISH_CONNECT}" ]; then
   echo "$(date): Starting post-connect validation for ${WWID} on lun ${LUN}"
   ${FINISH_CONNECT} ${LUN} ${WWID}
   if [ $? -ne 0 ]; then
      echo "$(date): Failed to finish connect process for ${WWID} on lun ${LUN}"
      logger -t "CS_SCSI_VOL_CONN_FINISH" "${WWID} failed to finish connect process on lun ${LUN}"
      exit 1
   fi
else
   echo "$(date): Unable to find finishConnect.sh script!"
   exit 1
fi

logger -t "CS_SCSI_VOL_FIND" "${WWID} successfully discovered and available"

echo "$(date): Complete - found mapped LUN at /dev/mapper/3${WWID}"

exit 0
