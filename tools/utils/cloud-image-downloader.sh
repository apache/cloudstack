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

#-------------------------------------------------------------------------------
# Configuration
#-------------------------------------------------------------------------------
# This section contains the variables you might want to change.

# The temporary directory where files will be downloaded.
# It's a good practice to create a unique temporary directory for each script run.
TEMP_DIR=$(mktemp -d)

# The BASE destination directory for the downloaded image files.
# Subdirectories for each distro will be created inside this one.
# Make sure this directory exists before running the script.
# Must be executed by the cloudstack user on machine hosting the public download site.
# It will be publicly available at https://download.cloudstack.org/templates/cloud-images/
DEST_DIR="${HOME}/repository/templates/cloud-images"

# The directory where log files will be stored.
# Make sure this directory exists.
LOG_DIR="${HOME}/log/cloud-image-downloader"
LOG_FILE="${LOG_DIR}/cloud-image-downloader_$(date +%Y%m%d_%H%M%S).log"
LOG_RETENTION_DAYS=30

LOGGER_TAG="cloud-image-downloader"
LOGGER_FACILITY="user"
LOGGER_AVAILABLE=false

log_message() {
    local priority=$1
    shift
    local message="$*"
    local timestamp=$(date +'%Y-%m-%d %H:%M:%S')

    # Log to file
    echo "${timestamp} [${priority}] ${message}" | tee -a "${LOG_FILE}"

    # Log to syslog using logger utility
    if [ "${LOGGER_AVAILABLE}" = true ]; then
        logger -t "${LOGGER_TAG}" -p "${LOGGER_FACILITY}.${priority}" -- "${message}"
    fi
}

log_info() {
    log_message "info" "$@"
}

log_warn() {
    log_message "warning" "$@"
}

log_error() {
    log_message "err" "$@"
}

cleanup_old_logs() {
    log_info "Cleaning up log files older than ${LOG_RETENTION_DAYS} days..."

    if [ ! -d "$LOG_DIR" ]; then
        log_warn "Log directory does not exist: $LOG_DIR"
        return
    fi

    local deleted_count=0

    # Find and delete log files older than retention period
    while IFS= read -r -d '' log_file; do
        rm -f "$log_file"
        deleted_count=$((deleted_count + 1))
    done < <(find "$LOG_DIR" -name "*.log" -type f -mtime +${LOG_RETENTION_DAYS} -print0 2>/dev/null)

    if [ $deleted_count -gt 0 ]; then
        log_info "Deleted $deleted_count old log file(s)"
    else
        log_info "No old log files to delete"
    fi
}

#-------------------------------------------------------------------------------
# Image Definitions
#-------------------------------------------------------------------------------
# To add a new image, you must add an entry to BOTH arrays below.

# 1. Add the destination filename and the download URL.
declare -A IMAGE_URLS=(
    ["Rocky-9-GenericCloud.latest.x86_64.qcow2"]="https://dl.rockylinux.org/pub/rocky/9/images/x86_64/Rocky-9-GenericCloud.latest.x86_64.qcow2"
    ["Rocky-9-GenericCloud.latest.aarch64.qcow2"]="https://dl.rockylinux.org/pub/rocky/9/images/aarch64/Rocky-9-GenericCloud.latest.aarch64.qcow2"
    ["Rocky-8-GenericCloud.latest.x86_64.qcow2"]="https://dl.rockylinux.org/pub/rocky/8/images/x86_64/Rocky-8-GenericCloud.latest.x86_64.qcow2"
    ["Rocky-8-GenericCloud.latest.aarch64.qcow2"]="https://dl.rockylinux.org/pub/rocky/8/images/aarch64/Rocky-8-GenericCloud.latest.aarch64.qcow2"
    ["openSUSE-Leap-15.5-Minimal-VM.x86_64-Cloud.qcow2"]="https://download.opensuse.org/distribution/leap/15.5/appliances/openSUSE-Leap-15.5-Minimal-VM.x86_64-Cloud.qcow2"
    ["openSUSE-Leap-15.5-Minimal-VM.aarch64-Cloud.qcow2"]="https://download.opensuse.org/distribution/leap/15.5/appliances/openSUSE-Leap-15.5-Minimal-VM.aarch64-Cloud.qcow2"
    ["debian-12-genericcloud-amd64.qcow2"]="https://cloud.debian.org/images/cloud/bookworm/latest/debian-12-genericcloud-amd64.qcow2"
    ["debian-12-genericcloud-arm64.qcow2"]="https://cloud.debian.org/images/cloud/bookworm/latest/debian-12-genericcloud-arm64.qcow2"
    ["ubuntu-24.04-server-cloudimg-amd64.img"]="https://cloud-images.ubuntu.com/releases/24.04/release/ubuntu-24.04-server-cloudimg-amd64.img"
    ["ubuntu-24.04-server-cloudimg-arm64.img"]="https://cloud-images.ubuntu.com/releases/24.04/release/ubuntu-24.04-server-cloudimg-arm64.img"
    ["ubuntu-22.04-server-cloudimg-amd64.img"]="https://cloud-images.ubuntu.com/releases/22.04/release/ubuntu-22.04-server-cloudimg-amd64.img"
    ["ubuntu-22.04-server-cloudimg-arm64.img"]="https://cloud-images.ubuntu.com/releases/22.04/release/ubuntu-22.04-server-cloudimg-arm64.img"
    ["ubuntu-20.04-server-cloudimg-amd64.img"]="https://cloud-images.ubuntu.com/releases/20.04/release/ubuntu-20.04-server-cloudimg-amd64.img"
    ["ubuntu-20.04-server-cloudimg-arm64.img"]="https://cloud-images.ubuntu.com/releases/20.04/release/ubuntu-20.04-server-cloudimg-arm64.img"
    ["OL9U5_x86_64-kvm-b259.qcow2"]="https://yum.oracle.com/templates/OracleLinux/OL9/u5/x86_64/OL9U5_x86_64-kvm-b259.qcow2"
    ["OL9U5_aarch64-kvm-b126.qcow2"]="https://yum.oracle.com/templates/OracleLinux/OL9/u5/aarch64/OL9U5_aarch64-kvm-b126.qcow2"
    ["OL8U10_x86_64-kvm-b258.qcow2"]="https://yum.oracle.com/templates/OracleLinux/OL8/u10/x86_64/OL8U10_x86_64-kvm-b258.qcow2"
    ["OL8U10_aarch64-kvm-b122.qcow2"]="https://yum.oracle.com/templates/OracleLinux/OL8/u10/aarch64/OL8U10_aarch64-kvm-b122.qcow2"
)

# 2. Add the destination filename and its corresponding distribution subdirectory name.
declare -A IMAGE_DISTROS=(
    ["Rocky-9-GenericCloud.latest.x86_64.qcow2"]="rockylinux"
    ["Rocky-9-GenericCloud.latest.aarch64.qcow2"]="rockylinux"
    ["Rocky-8-GenericCloud.latest.x86_64.qcow2"]="rockylinux"
    ["Rocky-8-GenericCloud.latest.aarch64.qcow2"]="rockylinux"
    ["openSUSE-Leap-15.5-Minimal-VM.x86_64-Cloud.qcow2"]="opensuse"
    ["openSUSE-Leap-15.5-Minimal-VM.aarch64-Cloud.qcow2"]="opensuse"
    ["debian-12-genericcloud-amd64.qcow2"]="debian"
    ["debian-12-genericcloud-arm64.qcow2"]="debian"
    ["ubuntu-24.04-server-cloudimg-amd64.img"]="ubuntu"
    ["ubuntu-24.04-server-cloudimg-arm64.img"]="ubuntu"
    ["ubuntu-22.04-server-cloudimg-amd64.img"]="ubuntu"
    ["ubuntu-22.04-server-cloudimg-arm64.img"]="ubuntu"
    ["ubuntu-20.04-server-cloudimg-amd64.img"]="ubuntu"
    ["ubuntu-20.04-server-cloudimg-arm64.img"]="ubuntu"
    ["OL9U5_x86_64-kvm-b259.qcow2"]="oraclelinux"
    ["OL9U5_aarch64-kvm-b126.qcow2"]="oraclelinux"
    ["OL8U10_x86_64-kvm-b258.qcow2"]="oraclelinux"
    ["OL8U10_aarch64-kvm-b122.qcow2"]="oraclelinux"
)

#-------------------------------------------------------------------------------
# Cleanup Handler
#-------------------------------------------------------------------------------

cleanup_on_exit() {
    local exit_code=$?
    if [ -d "$TEMP_DIR" ]; then
        rm -rf "$TEMP_DIR"
        log_info "Temporary directory $TEMP_DIR removed."
    fi

    if [ $exit_code -ne 0 ]; then
        log_error "Script exited with error code: $exit_code"
    fi
}

trap cleanup_on_exit EXIT INT TERM

#-------------------------------------------------------------------------------
# Main Script Logic
#-------------------------------------------------------------------------------

if command -v logger &> /dev/null; then
    LOGGER_AVAILABLE=true
fi

# Ensure base destination and log directories exist
mkdir -p "$DEST_DIR"
mkdir -p "$LOG_DIR"

# Clean up old logs first
cleanup_old_logs

log_info "Starting image download process."
log_info "Temporary directory: $TEMP_DIR"
log_info "Base destination directory: $DEST_DIR"
log_info "Log file: $LOG_FILE"

# Inform about logger status
if [ "${LOGGER_AVAILABLE}" = true ]; then
    log_info "Syslog logging enabled (tag: ${LOGGER_TAG})"
else
    log_warn "Syslog logging disabled - logger utility not found"
fi

# Loop through the image URLs
for filename in "${!IMAGE_URLS[@]}"; do
    url="${IMAGE_URLS[$filename]}"
    distro="${IMAGE_DISTROS[$filename]}"

    # Check if a distro is defined for the file
    if [ -z "$distro" ]; then
        log_error "No distribution directory defined for $filename. Skipping."
        continue
    fi

    distro_dest_dir="${DEST_DIR}/${distro}"
    temp_filepath="${TEMP_DIR}/${filename}"
    dest_filepath="${distro_dest_dir}/${filename}"

    log_info "--------------------------------------------------"
    log_info "Starting download for: $filename"
    log_info "URL: $url"

    # Download the file to the temporary directory
    wget --progress=bar:force:noscroll -O "$temp_filepath" "$url"
    download_status=$?

    if [ $download_status -ne 0 ]; then
        # Handle download failure
        log_error "Failed to download $filename from $url. wget exit code: $download_status"
    else
        # Handle download success
        log_info "Successfully downloaded $filename to temporary location."

        # Ensure the specific distro directory exists
        log_info "Ensuring destination directory exists: $distro_dest_dir"
        mkdir -p "$distro_dest_dir"

        # Move the file to the destination directory, replacing any existing file
        log_info "Moving $filename to $dest_filepath"
        mv -f "$temp_filepath" "$dest_filepath"
        move_status=$?

        if [ $move_status -ne 0 ]; then
            log_error "Failed to move $filename to $dest_filepath. mv exit code: $move_status"
        else
            log_info "Successfully moved $filename."
        fi
    fi
done

log_info "Generate checksum"
# Create md5 checksum
checksum_file="md5sum.txt"
sha512_checksum_file="sha512sum.txt"

cd "$DEST_DIR"
find . -type f ! -iname '*.txt' -exec md5sum {} \; > "$checksum_file"
checksum_status=$?
if [ $checksum_status -ne 0 ]; then
    log_error "Failed to create md5 checksum. md5sum exit code: $checksum_status"
else
    log_info "Successfully created checksum file: $checksum_file"
fi

find . -type f ! -iname '*.txt' -exec sha512sum {} \; > "$sha512_checksum_file"
sha512_checksum_status=$?
if [ $sha512_checksum_status -ne 0 ]; then
    log_error "Failed to create sha512 checksum. sha512sum exit code: $sha512_checksum_status"
else
    log_info "Successfully created checksum file: $sha512_checksum_file"
fi

log_info "--------------------------------------------------"
log_info "Image download process finished."
