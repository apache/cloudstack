#!/bin/bash

OUTPUT_FORMAT=${1:?"Output format is required"}
INPUT_FILE=${2:?"Input file/path is required"}
OUTPUT_FILE=${3:?"Output file/path is required"}

echo "$(date): qemu-img convert -n -p -W -t none -O ${OUTPUT_FORMAT} ${INPUT_FILE} ${OUTPUT_FILE}"

qemu-img convert -n -p -W -t none -O ${OUTPUT_FORMAT} ${INPUT_FILE} ${OUTPUT_FILE} && {
   # if its a block device make sure we flush caches before exiting
   lsblk ${OUTPUT_FILE} >/dev/null 2>&1 && {
      blockdev --flushbufs ${OUTPUT_FILE}
      hdparm -F ${OUTPUT_FILE}
   }
   exit 0
}