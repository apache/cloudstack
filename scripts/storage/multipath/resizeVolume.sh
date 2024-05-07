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

notifyqemu() {
  if `virsh help 2>/dev/null | grep -q blockresize`
  then
    if `virsh domstate $VMNAME >/dev/null 2>&1`
    then
      sizeinkb=$(($NEWSIZE/1024))
      devicepath=$(virsh domblklist $VMNAME | grep ${WWID} | awk '{print $1}')
      virsh blockresize --path $devicepath --size $sizeinkb ${VMNAME} >/dev/null 2>&1
      retval=$?
      if [ -z $retval ] || [ $retval -ne 0 ]
      then
        log "failed to live resize $path to size of $sizeinkb kb" 1
      else
        liveresize='true'
      fi
    fi
  fi
}

WWID=${1:?"WWID required"}
VMNAME=${2:?"VMName required"}
NEWSIZE=${3:?"New size required in bytes"}

WWID=$(echo $WWID | tr '[:upper:]' '[:lower:]')

export WWID VMNAME NEWSIZE

systemctl is-active multipathd || systemctl restart multipathd || {
   echo "$(date): Multipathd is NOT running and cannot be started.  This must be corrected before this host can access this storage volume."
   logger -t "CS_SCSI_VOL_RESIZE" "Unable to notify running VM of resize for ${WWID} because multipathd is not currently running and cannot be started"
   exit 1
}

logger -t "CS_SCSI_VOL_RESIZE" "${WWID} resizing disk path at /dev/mapper/3${WWID} STARTING"

for device in $(multipath -ll 3${WWID} | egrep '^  ' | awk '{print $2}'); do
    echo "1" > /sys/bus/scsi/drivers/sd/${device}/rescan;
done

sleep 3

multipathd reconfigure

sleep 3

multipath -ll 3${WWID}

notifyqemu

logger -t "CS_SCSI_VOL_RESIZE" "${WWID} resizing disk path at /dev/mapper/3${WWID} COMPLETE"

exit 0
