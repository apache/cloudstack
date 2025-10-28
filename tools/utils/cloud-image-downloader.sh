#!/bin/bash

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
DEST_DIR="~/repository/templates/cloud-images/"

# The directory where log files will be stored.
# Make sure this directory exists.
LOG_DIR="~/log/cloud-image-downloader"
LOG_FILE="${LOG_DIR}/run_$(date +%Y%m%d_%H%M%S).log"
ERROR_LOG_FILE="${LOG_DIR}/error_$(date +%Y%m%d_%H%M%S).log"

#-------------------------------------------------------------------------------
# Image Definitions
#-------------------------------------------------------------------------------
# To add a new image, you must add an entry to BOTH arrays below.

# 1. Add the destination filename and the download URL.
declare -A IMAGE_URLS=(
    ["Rocky-8-GenericCloud.latest.aarch64.qcow2"]="https://dl.rockylinux.org/pub/rocky/8/images/aarch64/Rocky-8-GenericCloud.latest.aarch64.qcow2"
    ["Rocky-9-GenericCloud.latest.aarch64.qcow2"]="https://dl.rockylinux.org/pub/rocky/9/images/aarch64/Rocky-9-GenericCloud.latest.aarch64.qcow2"
    ["Rocky-9-GenericCloud.latest.x86_64.qcow2"]="https://dl.rockylinux.org/pub/rocky/9/images/x86_64/Rocky-9-GenericCloud.latest.x86_64.qcow2"
    ["openSUSE-Leap-15.5-Minimal-VM.x86_64-Cloud.qcow2"]="https://download.opensuse.org/distribution/leap/15.5/appliances/openSUSE-Leap-15.5-Minimal-VM.x86_64-Cloud.qcow2"
    ["openSUSE-Leap-15.5-Minimal-VM.aarch64-Cloud.qcow2"]="https://download.opensuse.org/distribution/leap/15.5/appliances/openSUSE-Leap-15.5-Minimal-VM.aarch64-Cloud.qcow2"
    ["debian-12-genericcloud-amd64.qcow2"]="https://cloud.debian.org/images/cloud/bookworm/latest/debian-12-genericcloud-amd64.qcow2"
    ["debian-12-genericcloud-arm64.qcow2"]="https://cloud.debian.org/images/cloud/bookworm/latest/debian-12-genericcloud-arm64.qcow2"
    ["ubuntu-24.04-server-cloudimg-amd64.img"]="https://cloud-images.ubuntu.com/releases/24.04/release/ubuntu-24.04-server-cloudimg-amd64.img"
    ["ubuntu-22.04-server-cloudimg-amd64.img"]="https://cloud-images.ubuntu.com/releases/22.04/release/ubuntu-22.04-server-cloudimg-amd64.img"
    ["ubuntu-20.04-server-cloudimg-amd64.img"]="https://cloud-images.ubuntu.com/releases/20.04/release/ubuntu-20.04-server-cloudimg-amd64.img"
    ["ubuntu-24.04-server-cloudimg-arm64.img"]="https://cloud-images.ubuntu.com/releases/24.04/release/ubuntu-24.04-server-cloudimg-arm64.img"
    ["ubuntu-22.04-server-cloudimg-arm64.img"]="https://cloud-images.ubuntu.com/releases/22.04/release/ubuntu-22.04-server-cloudimg-arm64.img"
    ["ubuntu-20.04-server-cloudimg-arm64.img"]="https://cloud-images.ubuntu.com/releases/20.04/release/ubuntu-20.04-server-cloudimg-arm64.img"
    ["OL9U5_x86_64-kvm-b259.qcow2"]="https://yum.oracle.com/templates/OracleLinux/OL9/u5/x86_64/OL9U5_x86_64-kvm-b259.qcow2"
    ["OL8U10_x86_64-kvm-b258.qcow2"]="https://yum.oracle.com/templates/OracleLinux/OL8/u10/x86_64/OL8U10_x86_64-kvm-b258.qcow2"
    ["OL9U5_aarch64-kvm-b126.qcow2"]="https://yum.oracle.com/templates/OracleLinux/OL9/u5/aarch64/OL9U5_aarch64-kvm-b126.qcow2"
    ["OL8U10_aarch64-kvm-b122.qcow2"]="https://yum.oracle.com/templates/OracleLinux/OL8/u10/aarch64/OL8U10_aarch64-kvm-b122.qcow2"
    ["Rocky-8-GenericCloud.latest.x86_64.qcow2"]="https://dl.rockylinux.org/pub/rocky/8/images/x86_64/Rocky-8-GenericCloud.latest.x86_64.qcow2"
)

# 2. Add the destination filename and its corresponding distribution subdirectory name.
declare -A IMAGE_DISTROS=(
    ["Rocky-8-GenericCloud.latest.aarch64.qcow2"]="rockylinux"
    ["Rocky-8-GenericCloud.latest.x86_64.qcow2"]="rockylinux"
    ["Rocky-9-GenericCloud.latest.aarch64.qcow2"]="rockylinux"
    ["Rocky-9-GenericCloud.latest.x86_64.qcow2"]="rockylinux"
    ["openSUSE-Leap-15.5-Minimal-VM.x86_64-Cloud.qcow2"]="opensuse"
    ["openSUSE-Leap-15.5-Minimal-VM.aarch64-Cloud.qcow2"]="opensuse"
    ["debian-12-genericcloud-amd64.qcow2"]="debian"
    ["debian-12-genericcloud-arm64.qcow2"]="debian"
    ["ubuntu-24.04-server-cloudimg-amd64.img"]="ubuntu"
    ["ubuntu-22.04-server-cloudimg-amd64.img"]="ubuntu"
    ["ubuntu-20.04-server-cloudimg-amd64.img"]="ubuntu"
    ["ubuntu-24.04-server-cloudimg-arm64.img"]="ubuntu"
    ["ubuntu-22.04-server-cloudimg-arm64.img"]="ubuntu"
    ["ubuntu-20.04-server-cloudimg-arm64.img"]="ubuntu"
    ["OL9U5_x86_64-kvm-b259.qcow2"]="oraclelinux"
    ["OL8U10_x86_64-kvm-b258.qcow2"]="oraclelinux"
    ["OL9U5_aarch64-kvm-b126.qcow2"]="oraclelinux"
    ["OL8U10_aarch64-kvm-b122.qcow2"]="oraclelinux"
)


#-------------------------------------------------------------------------------
# Main Script Logic
#-------------------------------------------------------------------------------

# Function to log messages
log() {
    echo "$(date +'%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

# Ensure base destination and log directories exist
mkdir -p "$DEST_DIR"
mkdir -p "$LOG_DIR"

log "Starting image download process."
log "Temporary directory: $TEMP_DIR"
log "Base destination directory: $DEST_DIR"
log "Log file: $LOG_FILE"
log "Error log file: $ERROR_LOG_FILE"

# Loop through the image URLs
for filename in "${!IMAGE_URLS[@]}"; do
    url="${IMAGE_URLS[$filename]}"
    distro="${IMAGE_DISTROS[$filename]}"
    
    # Check if a distro is defined for the file
    if [ -z "$distro" ]; then
        error_message="No distribution directory defined for $filename. Skipping."
        log "ERROR: $error_message"
        echo "$(date +'%Y-%m-%d %H:%M:%S') - $error_message" >> "$ERROR_LOG_FILE"
        continue
    fi

    distro_dest_dir="${DEST_DIR}/${distro}"
    temp_filepath="${TEMP_DIR}/${filename}"
    dest_filepath="${distro_dest_dir}/${filename}"

    log "--------------------------------------------------"
    log "Starting download for: $filename"
    log "URL: $url"

    # Download the file to the temporary directory
    wget --progress=bar:force:noscroll -O "$temp_filepath" "$url"
    download_status=$?

    if [ $download_status -ne 0 ]; then
        # Handle download failure
        error_message="Failed to download $filename from $url. wget exit code: $download_status"
        log "ERROR: $error_message"
        echo "$(date +'%Y-%m-%d %H:%M:%S') - $error_message" >> "$ERROR_LOG_FILE"
    else
        # Handle download success
        log "Successfully downloaded $filename to temporary location."
        
        # Ensure the specific distro directory exists
        log "Ensuring destination directory exists: $distro_dest_dir"
        mkdir -p "$distro_dest_dir"

        # Move the file to the destination directory, replacing any existing file
        log "Moving $filename to $dest_filepath"
        mv -f "$temp_filepath" "$dest_filepath"
        move_status=$?

        if [ $move_status -ne 0 ]; then
            error_message="Failed to move $filename to $dest_filepath. mv exit code: $move_status"
            log "ERROR: $error_message"
            echo "$(date +'%Y-%m-%d %H:%M:%S') - $error_message" >> "$ERROR_LOG_FILE"
        else
            log "Successfully moved $filename."
        fi
    fi
done

log "Generate checksum"
# Create md5 checksum
checksum_file="md5sum.txt"
sha512_checksum_file="sha512sum.txt"

cd "$DEST_DIR"
find . -type f ! -iname '*.txt' -exec md5sum {} \; > "$checksum_file"
find . -type f ! -iname '*.txt' -exec sha512sum {} \; > "$sha512_checksum_file"
checksum_status=$?
if [ $checksum_status -ne 0 ]; then
    error_message="Failed to create md5 checksum. md5sum exit code: $checksum_status"
    log "ERROR: $error_message"
    echo "$(date +'%Y-%m-%d %H:%M:%S') - $error_message" >> "$ERROR_LOG_FILE"
else
    log "Successfully created checksum file: $checksum_file"
fi

log "--------------------------------------------------"
log "Image download process finished."

# Clean up the temporary directory
rm -rf "$TEMP_DIR"
log "Temporary directory $TEMP_DIR removed."