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

#########################################################################################
#
# Given a WWID, cleanup/remove any multipath and devices associated with this WWID.  This
# may not always have lasting result because if the storage array still has the volume
# visable to the host, it may be rediscovered.  The cleanupStaleMaps.sh script should
# catch those cases
#
#########################################################################################

WWID=${1:?"WWID required"}
WWID=$(echo $WWID | tr '[:upper:]' '[:lower:]')

echo "$(date): Removing ${WWID}"

systemctl is-active multipathd || systemctl restart multipathd || {
   echo "$(date): Multipathd is NOT running and cannot be started.  This must be corrected before this host can access this storage volume."
   logger -t "CS_SCSI_VOL_REMOVE" "${WWID} cannot be disconnected from this host because multipathd is not currently running and cannot be started"
   exit 1
}

# first get dm- name
DM_NAME=$(ls -lrt /dev/mapper/3${WWID} | awk '{ print $NF }' | awk -F'/' '{print $NF}')
SLAVE_DEVS=""
if [ -z "${DM_NAME}" ]; then
   logger -t CS_SCSI_VOL_REMOVE "${WWID} has no active multimap so no removal performed"
   logger -t CS_SCSI_VOL_REMOVE "WARN: dm name could not be found for ${WWID}"
   dmsetup remove /dev/mapper/*${WWID}
   logger -t CS_SCSI_VOL_REMOVE "${WWID} removal via dmsetup remove /dev/mapper/${WWID} finished with return code $?"
else
   # now look for slave devices and save for deletion
   for dev in $(ls /sys/block/${DM_NAME}/slaves/ 2>/dev/null); do
      SLAVE_DEVS="${SLAVE_DEVS} ${dev}"
   done
fi

# delete the path map last
multipath -f 3${WWID}

# now delete slave devices
# https://bugzilla.redhat.com/show_bug.cgi?id=1949369
if [ ! -z "${SLAVE_DEVS}" ]; then
  for dev in ${SLAVE_DEVS}; do
     multipathd del path /dev/${dev}
     echo "1" > /sys/block/${dev}/device/delete
     logger -t CS_SCSI_VOL_REMOVE "${WWID} removal of device ${dev} complete"
  done
fi

logger -t CS_SCSI_VOL_REMOVE "${WWID} successfully purged from multipath along with slave devices"

echo "$(date): ${WWID} removed"

exit 0
