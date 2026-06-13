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
# through the SCSI bus and make sure its visible via multipath
#
#####################################################################################


LUN=${1:?"LUN required"}
WWID=${2:?"WWID required"}

WWID=$(echo $WWID | tr '[:upper:]' '[:lower:]')

systemctl is-active multipathd || systemctl restart multipathd || {
   echo "$(date): Multipathd is NOT running and cannot be started.  This must be corrected before this host can access this storage volume."
   logger -t "CS_SCSI_VOL_CONN_FINISH" "${WWID} cannot be mapped to this host because multipathd is not currently running and cannot be started"
   exit 1
}

echo "$(date): Doing post-connect validation for ${WWID} on lun ${LUN}"

# get vendor OUI.  we will only delete a device on the designated lun if it matches the
# incoming WWN OUI value.  This is because multiple storage arrays may be mapped to the
# host on different fiber channel hosts with the same LUN
INCOMING_OUI=$(echo ${WWID} | cut -c2-7)
echo "$(date): Incoming OUI: ${INCOMING_OUI}"

logger -t "CS_SCSI_VOL_CONN_FINISH" "${WWID} looking for disk path at /dev/mapper/3${WWID}"

echo "$(date): Doing a recan to make sure we have proper current size locally"
for device in $(multipath -ll 3${WWID} | egrep '^  ' | awk '{print $2}'); do
    echo "1" > /sys/bus/scsi/drivers/sd/${device}/rescan;
done

sleep 3

multipathd reconfigure

sleep 3

# cleanup any old/faulty paths
delete_needed=false
multipath -l 3${WWID}
for dev in $(multipath -l 3${WWID} 2>/dev/null| grep failed  | awk '{print $3}' ); do
   logger -t "CS_SCSI_VOL_CONN_FINISH" "${WWID} multipath contains faulty path ${dev}, removing"
   echo 1 > /sys/block/${dev}/device/delete;
   delete_needed=true
done

if [ "${delete_needed}" == "true" ]; then
    sleep 10
    multipath -v2 >/dev/null
fi

multipath -l 3${WWID}

logger -t "CS_SCSI_VOL_CONN_FINISH" "${WWID} successfully discovered and available"

echo "$(date): Complete - found mapped LUN at /dev/mapper/3${WWID}"

exit 0
