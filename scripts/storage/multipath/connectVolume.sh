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

echo "$(date): Looking for ${WWID} on lun ${LUN}"

# first we need to check if any stray references are left from a previous use of this lun
lingering_devs=$(lsscsi -w "*:*:*:${LUN}" | grep /dev | awk '{printf("%s:%s ", $NF, $NF-1); }')

if [ ! -z "${lingering_devs}" ]; then
    for dev in ${lingering_devs}; do
       LSSCSI_WWID=$(echo $dev | awk -F: '{print $2}')
       dev=$(echo $dev | awk -F: '{ print $1}')
       logger -t "CS_SCSI_VOL_FIND" "${WWID} processing identified a lingering device ${dev} from previous lun use, attempting to clean up"
       MP_WWID=$(multipath -l ${dev} | head -1 | awk '{print $1}')
       MP_WWID=${MP_WWID:1} # strip first character (3) off
       # don't do this if the WWID passed in matches the WWID from multipath
       if [ ! -z "${MP_WWID}" ] && [ "${MP_WWID}" != "${WWID}" ]; then
          # run full removal again so all devices and multimap are cleared
          $(dirname $0)/disconnectVolume.sh ${MP_WWID}
       # we don't have a multimap but we may still have some stranded devices to clean up
       elif [ "${LSSCSI_WWID}" != "${WWID}" ]; then
           echo "1" > /sys/block/$(echo ${dev} | awk -F'/' '{print $NF}')/device/delete
       fi
    done

    sleep 3
fi

logger -t "CS_SCSI_VOL_FIND" "${WWID} awaiting disk path at /dev/mapper/3${WWID}"

# wait for multipath to map the new lun to the WWID
echo "$(date): Waiting for multipath entry to show up for the WWID"
while true; do
   ls /dev/mapper/3${WWID} >/dev/null 2&1
   if [ $? == 0 ]; then
      break
   fi

   logger -t "CS_SCSI_VOL_FIND" "${WWID} not available yet, triggering scan"

   # instruct bus to scan for new lun
   for fchost in $(ls /sys/class/fc_host); do
      echo "   --> Scanning ${fchost}"
      echo "- - ${LUN}" > /sys/class/scsi_host/${fchost}/scan
   done

   multipath -v2 2>/dev/null

   ls /dev/mapper/3${WWID} >/dev/null 2>&1
   if [ $? == 0 ]; then
      break
   fi

   sleep 5
done

# cleanup any old/faulty paths
delete_needed=false
multipath -l 3${WWID}
for dev in $(multipath -l 3${WWID} 2>/dev/null| grep failed  | awk '{print $3}' ); do
   logger -t "CS_SCSI_VOL_FIND" "${WWID} multipath contains faulty path ${dev}, removing"
   echo 1 > /sys/block/${dev}/device/delete;
   delete_needed=true
done

if [ "${delete_needed}" == "true" ]; then
    sleep 10
    multipath -v2 >/dev/null
fi

multipath -l 3${WWID}

logger -t "CS_SCSI_VOLFIND" "${WWID} successfully discovered and available"

echo "$(date): Complete - found mapped LUN at /dev/mapper/3${WWID}"

exit 0
