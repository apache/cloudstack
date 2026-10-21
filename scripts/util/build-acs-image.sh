#!/bin/bash
# Prepare a generic cloud qcow2 image for CloudStack: patch it, shrink it and convert it to VHD/OVA.
#
# Usage: build-acs-image.sh [-a ACTIONS] [-f FORMATS] [-o OUTDIR] [-g GUESTOS] [-u] [-z]
#                           [-d debian|rhel|suse] [-p ROOT_PASSWORD] <image.qcow2>
#   -a  actions, a list separated by commas or spaces, done in this order (default: all)
#         patch    install the guest tools, set the root password, cloud-init and sshd config
#                  (OS detected from the image: Debian/Ubuntu, EL, SUSE and variants)
#         shrink   virt-sparsify and qemu-img convert -c
#         convert  convert to the formats of -f
#         all      patch, shrink and convert
#   -f  formats for the convert action, a list like "vhd,ova" or "ova, xen" (default: all)
#         vhd  plain VHD for Hyper-V (also hyperv)           <name>-acs.vhd (removed if xen is built too)
#         ova  OVA for VMware (also vmware)                  <name>-acs.ova
#         xen  VHD for XenServer, bzip2 (also xenserver)     <name>-acs-xen.vhd.bz2
#         all  vhd, ova and xen
#   -o  output directory (default: directory of the image)
#   -g  VMware guestos id for the OVA (default: detected from /etc/os-release in the image,
#       e.g. debian12-64, ubuntu-64, rhel9-64, sles15-64, otherlinux-64)
#   -u  with patch: also run a full package upgrade inside the image
#   -z  with shrink: do not compress (plain qcow2 rewrite)
#   -d  with patch: OS family, if it cannot be detected from the image
#   -p  with patch: root password to set (default: password)
#
# The image is never modified. Patch and shrink work on a copy named <name>-acs.<ext> in the
# output directory (the name is kept if it already ends with -acs), and the converted files
# are named <name>-acs.vhd and <name>-acs.ova. Convert alone reads the original image.
#
# SHA512SUMS with the checksums of the output files is written to the output directory when all
# is done (the entries of other files are kept). The tools needed by the actions and formats are
# checked first, and nothing is installed.
set -euo pipefail

usage() { sed -n '2,29p' "$0"; exit 1; }

ACTIONS="all"
FORMATS="all"
OUTDIR=""
GUESTOS=""
UPDATE=""
COMPRESS="-c"
OS_FAMILY=""
ROOT_PASSWORD="password"
while getopts "a:f:o:g:uzd:p:" opt; do
  case $opt in
    a) ACTIONS="$OPTARG" ;;
    f) FORMATS="$OPTARG" ;;
    o) OUTDIR="$OPTARG" ;;
    g) GUESTOS="$OPTARG" ;;
    u) UPDATE="--update" ;;
    z) COMPRESS="" ;;
    d) OS_FAMILY="$OPTARG" ;;
    p) ROOT_PASSWORD="$OPTARG" ;;
    *) usage ;;
  esac
done
shift $((OPTIND - 1))

IMAGE="${1:-}"
[ -f "$IMAGE" ] || { echo "Image not found: '$IMAGE'" >&2; usage; }
case "$OS_FAMILY" in ""|debian|rhel|suse) ;; *) echo "Invalid OS family: $OS_FAMILY" >&2; exit 1 ;; esac

# ACTIONS and FORMATS are lists separated by commas and/or spaces
DO_PATCH="" DO_SHRINK="" DO_CONVERT=""
for a in $(echo "$ACTIONS" | tr 'A-Z,;' 'a-z  '); do
  case "$a" in
    patch) DO_PATCH=1 ;;
    shrink) DO_SHRINK=1 ;;
    convert) DO_CONVERT=1 ;;
    all) DO_PATCH=1; DO_SHRINK=1; DO_CONVERT=1 ;;
    *) echo "Invalid action: '$a' (supported: patch, shrink, convert, all)" >&2; exit 1 ;;
  esac
done
[ -n "$DO_PATCH$DO_SHRINK$DO_CONVERT" ] || { echo "No action given" >&2; exit 1; }

WANT_VHD="" WANT_OVA="" WANT_XEN=""
for f in $(echo "$FORMATS" | tr 'A-Z,;' 'a-z  '); do
  case "$f" in
    vhd|hyperv) WANT_VHD=1 ;;
    ova|vmware) WANT_OVA=1 ;;
    xen|xenserver) WANT_XEN=1 ;;
    all) WANT_VHD=1; WANT_OVA=1; WANT_XEN=1 ;;
    *) echo "Invalid format: '$f' (supported: vhd, ova, xen, all, or a list like vhd,ova)" >&2; exit 1 ;;
  esac
done
[ -n "$WANT_VHD$WANT_OVA$WANT_XEN" ] || { echo "No output format given" >&2; exit 1; }
# the formats are only needed by the convert action
if [ -z "$DO_CONVERT" ]; then WANT_VHD=""; WANT_OVA=""; WANT_XEN=""; fi

# names: <name>-acs.<ext> for the copy that is patched and shrunk, <name>-acs.<format> for the conversions
IMAGE=$(readlink -f "$IMAGE")
[ -n "$OUTDIR" ] || OUTDIR=$(dirname "$IMAGE")
mkdir -p "$OUTDIR"
OUTDIR=$(readlink -f "$OUTDIR")
file=$(basename "$IMAGE")
if [[ "$file" == *.* ]]; then stem="${file%.*}"; ext=".${file##*.}"; else stem="$file"; ext=""; fi
case "$stem" in *-acs) OUT_BASE="$stem" ;; *) OUT_BASE="${stem}-acs" ;; esac
WORK_IMAGE="$OUTDIR/$OUT_BASE$ext"
# patch and shrink work on the copy, convert alone reads the original image
SOURCE="$IMAGE"
if [ -n "$DO_PATCH$DO_SHRINK" ]; then SOURCE="$WORK_IMAGE"; fi

export LIBGUESTFS_BACKEND=direct

# Hint on the package names for the missing tools, according to the package manager of the host
pkg_hint() {
  local what="$1"
  if command -v apt-get >/dev/null 2>&1; then
    [ "$what" = "libguestfs" ] && echo "apt install libguestfs-tools qemu-utils" || echo "apt install $what"
  elif command -v zypper >/dev/null 2>&1; then
    [ "$what" = "libguestfs" ] && echo "zypper install guestfs-tools libguestfs qemu-tools" || echo "zypper install $what"
  elif command -v dnf >/dev/null 2>&1 || command -v yum >/dev/null 2>&1; then
    [ "$what" = "libguestfs" ] && echo "dnf install guestfs-tools (libguestfs-tools-c on EL8) qemu-img" || echo "dnf install $what"
  else
    echo "install libguestfs tools and qemu-img"
  fi
}

# Check what the actions and formats need, before anything is created, and report all the
# problems at once. Packages are never installed here, as the script runs on Debian/Ubuntu,
# EL, SUSE, ...
check_dependencies() {
  local missing=() warnings=()
  local cmd

  for cmd in qemu-img cp mktemp sha512sum; do
    command -v "$cmd" >/dev/null 2>&1 || missing+=("$cmd not found ($(pkg_hint libguestfs))")
  done
  if [ -n "$DO_PATCH" ]; then
    for cmd in virt-customize virt-cat virt-ls virt-inspector; do
      command -v "$cmd" >/dev/null 2>&1 || missing+=("$cmd not found ($(pkg_hint libguestfs))")
    done
  fi
  if [ -n "$DO_SHRINK" ]; then
    command -v virt-sparsify >/dev/null 2>&1 || missing+=("virt-sparsify not found ($(pkg_hint libguestfs))")
  fi

  # the formats needed by qemu-img
  if command -v qemu-img >/dev/null 2>&1 && [ -n "$DO_CONVERT" ]; then
    local formats
    formats=$(qemu-img --help 2>&1 || true)
    if [ -n "$WANT_VHD" ]; then
      echo "$formats" | grep -qw vpc || missing+=("qemu-img does not support the vpc (VHD) format")
    fi
    if [ -n "$WANT_OVA" ]; then
      echo "$formats" | grep -qw vmdk || missing+=("qemu-img does not support the vmdk format")
    fi
  fi

  # the VMware guestos of the OVA is detected from the image, unless -g is given
  if [ -n "$WANT_OVA" ] && [ -z "$GUESTOS" ] && [ -z "$DO_PATCH" ]; then
    command -v virt-cat >/dev/null 2>&1 || missing+=("virt-cat not found, needed to detect the VMware guestos of the OVA from the image ($(pkg_hint libguestfs)), or use -g")
  fi

  # ovftool must be runnable, not only be present
  if [ -n "$WANT_OVA" ]; then
    if ! command -v ovftool >/dev/null 2>&1; then
      missing+=("ovftool not found, required for OVA (VMware OVF Tool, add it to PATH)")
    elif ! ovftool --version >/dev/null 2>&1; then
      missing+=("ovftool cannot be run (check its libraries and that the whole ovftool directory is installed)")
    fi
  fi

  # the XenServer VHD: bzip2, and the 32-bit vhd-util with faketime (as in tools/appliance/build.sh)
  if [ -n "$WANT_XEN" ]; then
    command -v bzip2 >/dev/null 2>&1 || missing+=("bzip2 not found, required for the XenServer VHD (install bzip2)")
    command -v faketime >/dev/null 2>&1 || missing+=("faketime not found, required for the XenServer VHD (install faketime)")
    if ! command -v vhd-util >/dev/null 2>&1; then
      missing+=("vhd-util not found, required for the XenServer VHD (see the comments in tools/appliance/build.sh, or http://packages.shapeblue.com/systemvmtemplate/vhd-util)")
    else
      # vhd-util is a 32-bit binary: it needs the i386 libraries, and faketime must be preloadable into it
      local vhd_out
      vhd_out=$(ldd "$(command -v vhd-util)" 2>&1 || true)
      if echo "$vhd_out" | grep -qE "not found|not a dynamic|No such file"; then
        missing+=("vhd-util cannot be run, missing libraries (32-bit libuuid and libc, and libvhd.so.1.0 in the ldconfig path)")
      elif command -v faketime >/dev/null 2>&1; then
        vhd_out=$(faketime '2010-01-01' vhd-util 2>&1 || true)
        echo "$vhd_out" | grep -q "cannot be preloaded" && \
          missing+=("faketime cannot be used with the 32-bit vhd-util (install the 32-bit faketime library, e.g. faketime:i386)")
      fi
    fi
  fi

  # the image must be a readable qcow2 file
  local info vsize_gb=1
  if command -v qemu-img >/dev/null 2>&1; then
    if ! info=$(qemu-img info --output=json "$IMAGE" 2>&1); then
      missing+=("cannot read the image: $(echo "$info" | head -1)")
    else
      echo "$info" | grep -q '"format": *"qcow2"' || missing+=("$IMAGE is not a qcow2 image")
      vsize_gb=$(echo "$info" | sed -n 's/.*"virtual-size": *\([0-9]*\).*/\1/p' | head -1)
      vsize_gb=$(( ${vsize_gb:-0} / 1024 / 1024 / 1024 + 1 ))
    fi
  fi

  # the architecture of the guest: commands cannot be run in a guest of another architecture
  # (virt-customize --install/--run-command), and the VHD/OVA are for x86_64 hypervisors
  if command -v virt-inspector >/dev/null 2>&1 && [ -z "${missing[*]:-}" ]; then
    local guest_arch host_arch
    guest_arch=$(virt-inspector -a "$IMAGE" 2>/dev/null | sed -n 's#.*<arch>\(.*\)</arch>.*#\1#p' | head -1)
    host_arch=$(uname -m)
    if [ -n "$guest_arch" ] && [ "$guest_arch" != "$host_arch" ]; then
      if [ -n "$DO_PATCH" ]; then
        missing+=("the image is $guest_arch but the host is $host_arch: virt-customize cannot run commands (install packages) in the guest, so the patch action is not possible here; run it on a $guest_arch host, or use -a shrink,convert")
      fi
    fi
    if [ -n "$guest_arch" ] && [ "$guest_arch" != "x86_64" ] && [ -n "$WANT_VHD$WANT_OVA$WANT_XEN" ]; then
      warnings+=("the image is $guest_arch, but the VHD (Hyper-V, XenServer) and the OVA (VMware) are normally used with x86_64 images")
    fi
  fi

  # the output
  [ -w "$OUTDIR" ] || missing+=("$OUTDIR is not writable")
  if [ -n "$DO_PATCH$DO_SHRINK" ]; then
    if [ "$WORK_IMAGE" = "$IMAGE" ]; then
      missing+=("$IMAGE is already an -acs image, the original image is never modified (use convert only, or pass the original image)")
    elif [ -e "$WORK_IMAGE" ]; then
      missing+=("$WORK_IMAGE already exists, remove it or move it away first")
    fi
  fi

  # disk space in the output directory: the copy, the sparsified and converted copies, the VHD, the VMDK and the OVA
  local need_gb=0 free_gb
  [ -n "$DO_PATCH$DO_SHRINK" ] && need_gb=$(( need_gb + vsize_gb ))
  [ -n "$DO_SHRINK" ] && need_gb=$(( need_gb + 2 * vsize_gb ))
  [ -n "$WANT_VHD" ] && need_gb=$(( need_gb + vsize_gb ))
  [ -n "$WANT_OVA" ] && need_gb=$(( need_gb + 2 * vsize_gb ))
  [ -n "$WANT_XEN" ] && need_gb=$(( need_gb + 2 * vsize_gb ))
  free_gb=$(df -P -BG "$OUTDIR" | awk 'NR==2 {gsub("G","",$4); print $4}')
  [ -n "$free_gb" ] && [ "$free_gb" -lt "$need_gb" ] 2>/dev/null && \
    warnings+=("only ${free_gb}G free in $OUTDIR, up to ${need_gb}G may be needed")

  if [ -n "$DO_PATCH$DO_SHRINK" ]; then
    # libguestfs builds its appliance in TMPDIR (or LIBGUESTFS_CACHEDIR), which needs about 2G
    local cache_dir="${LIBGUESTFS_CACHEDIR:-${TMPDIR:-/var/tmp}}"
    local cache_free
    cache_free=$(df -P -BG "$cache_dir" 2>/dev/null | awk 'NR==2 {gsub("G","",$4); print $4}')
    if [ -n "$cache_free" ] && [ "$cache_free" -lt 2 ] 2>/dev/null; then
      missing+=("only ${cache_free}G free in $cache_dir for the libguestfs appliance (set LIBGUESTFS_CACHEDIR and TMPDIR to a bigger disk)")
    fi

    # the libguestfs appliance is built from the kernel of the host (not of the image): on Ubuntu
    # /boot/vmlinuz-* is only readable by root, so supermin fails for other users
    if [ "$(id -u)" != "0" ] && ls /boot/vmlinuz-* >/dev/null 2>&1; then
      local k
      for k in /boot/vmlinuz-*; do [ -r "$k" ] && break; k=; done
      [ -n "${k:-}" ] || warnings+=("the host kernel in /boot is not readable by $(id -un), run as root or chmod a+r /boot/vmlinuz-*")
    fi
  fi

  local item
  for item in "${warnings[@]}"; do echo "WARN: $item" >&2; done
  if [ ${#missing[@]} -gt 0 ]; then
    for item in "${missing[@]}"; do echo "ERROR: $item" >&2; done
    echo "ERROR: ${#missing[@]} dependency problem(s), see above" >&2
    exit 1
  fi
}

# Read /etc/os-release from inside the image (read-only)
load_os_release() {
  local osr
  osr=$(virt-cat -a "$IMAGE" /etc/os-release 2>/dev/null || virt-cat -a "$IMAGE" /usr/lib/os-release 2>/dev/null || true)
  osr_value() { echo "$osr" | sed -n "s/^$1=//p" | head -1 | tr -d "\"'" ; }
  OS_ID=$(osr_value ID | tr 'A-Z' 'a-z')
  OS_ID_LIKE=$(osr_value ID_LIKE | tr 'A-Z' 'a-z')
  OS_VERSION_ID=$(osr_value VERSION_ID)
  OS_NAME=$(osr_value PRETTY_NAME)
}

# Map /etc/os-release to a VMware guestos id (the ids known by ESXi 7/8; newer releases of a
# distribution use the newest id, as older vSphere versions do not know the newest ids)
vmware_guestos() {
  local id="$OS_ID" like="$OS_ID_LIKE" major="${OS_VERSION_ID%%.*}"
  local n
  case "$id" in
    debian)
      n=$major; [[ "$n" =~ ^[0-9]+$ ]] || n=12
      [ "$n" -gt 12 ] && n=12; [ "$n" -lt 8 ] && n=8
      echo "debian${n}-64"; return ;;
    ubuntu) echo "ubuntu-64"; return ;;
    rhel|centos|ol|oracle)
      n=$major; [[ "$n" =~ ^[0-9]+$ ]] || n=9
      [ "$n" -gt 9 ] && n=9; [ "$n" -lt 6 ] && n=6
      case "$id" in
        rhel) echo "rhel${n}-64" ;;
        centos) echo "centos${n}-64" ;;
        *) echo "oraclelinux${n}-64" ;;
      esac
      return ;;
    rocky) echo "rockylinux-64"; return ;;
    almalinux|alma) echo "almalinux-64"; return ;;
    fedora) echo "fedora-64"; return ;;
    sles|sled|sle-micro)
      n=$major; [[ "$n" =~ ^[0-9]+$ ]] || n=15
      [ "$id" = "sle-micro" ] && n=15
      [ "$n" -gt 15 ] && n=15; [ "$n" -lt 11 ] && n=11
      echo "sles${n}-64"; return ;;
    opensuse*|suse) echo "opensuse-64"; return ;;
  esac
  # variants: use the family of ID_LIKE
  case " $like " in
    *" ubuntu "*) echo "ubuntu-64"; return ;;
    *" debian "*) echo "debian12-64"; return ;;
    *" rhel "*|*" fedora "*)
      n=$major; [[ "$n" =~ ^[0-9]+$ ]] || n=9
      [ "$n" -gt 9 ] && n=9; [ "$n" -lt 6 ] && n=6
      echo "rhel${n}-64"; return ;;
    *" suse "*) echo "sles15-64"; return ;;
  esac
  echo "otherlinux-64"
}

# Detect the OS family from the os-release of the image, to decide the packages and the cleanup
detect_os() {
  if [ -z "$OS_FAMILY" ]; then
    local id
    for id in $OS_ID $OS_ID_LIKE; do
      case "$id" in
        debian|ubuntu|linuxmint|raspbian|kali) OS_FAMILY=debian ;;
        rhel|centos|fedora|rocky|almalinux|alma|ol|oracle|amzn|scientific|eurolinux|cloudlinux|anolis|openeuler) OS_FAMILY=rhel ;;
        suse|sles|sled|sle-micro|opensuse|opensuse-leap|opensuse-tumbleweed|opensuse-microos) OS_FAMILY=suse ;;
      esac
      [ -n "$OS_FAMILY" ] && break
    done
  fi
  if [ -z "$OS_FAMILY" ]; then
    echo "ERROR: unsupported OS in the image (ID='${OS_ID}', ID_LIKE='${OS_ID_LIKE}'), use -d debian|rhel|suse to force a family" >&2
    exit 1
  fi
  echo "Detected OS: ${OS_NAME:-$OS_ID} (family: $OS_FAMILY)"

  local base_cleanup='journalctl --vacuum-time=1s; rm -rf /var/log/*; rm -f /root/.bash_history'
  case "$OS_FAMILY" in
    debian)
      # nfs-common is the NFS client, the cloud images do not have it and the tests mount NFS in the guest
      PACKAGES="qemu-guest-agent,open-vm-tools,xenstore-utils,libxenstore4,nfs-common"
      CLEANUP="apt clean; apt autoclean; apt autoremove --purge -y; $base_cleanup" ;;
    rhel)
      # there is no xenstore package in the EL repositories
      PACKAGES="qemu-guest-agent,open-vm-tools"
      CLEANUP="(dnf clean all || yum clean all); rm -rf /var/cache/dnf /var/cache/yum; $base_cleanup" ;;
    suse)
      PACKAGES="qemu-guest-agent,open-vm-tools,xen-tools-domU"
      CLEANUP="zypper --non-interactive clean --all; $base_cleanup" ;;
  esac
}

do_patch() {
  echo "== Patching $WORK_IMAGE =="
  local tmp
  tmp=$(mktemp -d)
  CLEAN_PATHS+=("$tmp")
  echo "policy: enabled" > "$tmp/ds-identify.cfg"
  cat > "$tmp/cloudstack.cfg" <<'EOF'
datasource_list: [ ConfigDrive, CloudStack, None ]
datasource:
    CloudStack: {}
    None: {}
EOF
  # sshd uses the first value it reads, so 40- loads before cloud-init's 50-cloud-init.conf
  printf 'PermitRootLogin yes\nPasswordAuthentication yes\n' > "$tmp/40-password-login.conf"

  # the drop-in only works if sshd_config includes sshd_config.d (not in EL8, Ubuntu 20.04, ...);
  # otherwise put the settings at the top of sshd_config, as the first value wins
  local sshd_fix='f=/etc/ssh/sshd_config; d=/etc/ssh/sshd_config.d/40-password-login.conf;
if [ -f "$f" ] && ! grep -qiE "^[[:space:]]*Include[[:space:]].*sshd_config\.d" "$f"; then
  { cat "$d"; cat "$f"; } > "$f.new" && cat "$f.new" > "$f" && rm -f "$f.new"
fi'

  virt-customize -a "$WORK_IMAGE" \
    $UPDATE \
    --install "$PACKAGES" \
    --root-password "password:$ROOT_PASSWORD" \
    --upload "$tmp/ds-identify.cfg:/etc/cloud/ds-identify.cfg" \
    --upload "$tmp/cloudstack.cfg:/etc/cloud/cloud.cfg.d/cloudstack.cfg" \
    --mkdir /etc/ssh/sshd_config.d \
    --upload "$tmp/40-password-login.conf:/etc/ssh/sshd_config.d/40-password-login.conf" \
    --run-command "$sshd_fix" \
    --run-command "$CLEANUP"
  rm -rf "$tmp"

  echo "--- verification ---"
  virt-cat -a "$WORK_IMAGE" /etc/cloud/ds-identify.cfg /etc/cloud/cloud.cfg.d/cloudstack.cfg /etc/ssh/sshd_config.d/40-password-login.conf
  local d
  for d in /usr/sbin /usr/bin; do
    virt-ls -a "$WORK_IMAGE" $d | grep -E '^(qemu-ga|vmtoolsd|xenstore)' | sed "s#^#$d/#" || true
  done
}

do_shrink() {
  echo "== Shrinking $WORK_IMAGE =="
  local sparse out
  sparse=$(mktemp "$OUTDIR/.sparse.XXXXXX")
  out=$(mktemp "$OUTDIR/.shrink.XXXXXX")
  CLEAN_PATHS+=("$sparse" "$out")
  rm -f "$sparse"
  virt-sparsify --convert qcow2 "$WORK_IMAGE" "$sparse"
  qemu-img convert $COMPRESS -f qcow2 -O qcow2 "$sparse" "$out"
  qemu-img check "$out" >/dev/null
  chmod --reference="$WORK_IMAGE" "$out"
  mv -f "$out" "$WORK_IMAGE"
  rm -f "$sparse"
  qemu-img info "$WORK_IMAGE" | grep -E 'virtual size|disk size'
}

do_convert_vhd() {
  echo "== Converting to VHD =="
  qemu-img convert -f qcow2 -O vpc "$SOURCE" "$OUTDIR/$OUT_BASE.vhd"
  chmod a+r "$OUTDIR/$OUT_BASE.vhd"
  echo "Created $OUTDIR/$OUT_BASE.vhd"
}

do_convert_xen() {
  echo "== Converting to the XenServer VHD =="
  local work
  work=$(mktemp -d "$OUTDIR/.xen-work.XXXXXX")
  CLEAN_PATHS+=("$work")
  qemu-img convert -f qcow2 -O raw "$SOURCE" "$work/img.raw"
  vhd-util convert -s 0 -t 1 -i "$work/img.raw" -o "$work/stagefixed.vhd"
  faketime '2010-01-01' vhd-util convert -s 1 -t 2 -i "$work/stagefixed.vhd" -o "$work/$OUT_BASE-xen.vhd"
  bzip2 -f "$work/$OUT_BASE-xen.vhd"
  mv -f "$work/$OUT_BASE-xen.vhd.bz2" "$OUTDIR/$OUT_BASE-xen.vhd.bz2"
  chmod a+r "$OUTDIR/$OUT_BASE-xen.vhd.bz2"
  rm -rf "$work"
  echo "Created $OUTDIR/$OUT_BASE-xen.vhd.bz2"
}

do_convert_ova() {
  echo "== Converting to OVA =="
  local work
  work=$(mktemp -d "$OUTDIR/.ova-work.XXXXXX")
  CLEAN_PATHS+=("$work")

  qemu-img convert -f qcow2 -O vmdk "$SOURCE" "$work/$OUT_BASE.vmdk"

  cat > "$work/$OUT_BASE.vmx" <<EOF
.encoding = "UTF-8"
displayname = "$OUT_BASE"
guestos = "$GUESTOS"
virtualhw.version = "10"
config.version = "8"
numvcpus = "2"
cpuid.coresPerSocket = "1"
memsize = "1024"
pciBridge0.present = "TRUE"
pciBridge4.present = "TRUE"
pciBridge4.virtualDev = "pcieRootPort"
pciBridge4.functions = "8"
pciBridge5.present = "TRUE"
pciBridge5.virtualDev = "pcieRootPort"
pciBridge5.functions = "8"
pciBridge6.present = "TRUE"
pciBridge6.virtualDev = "pcieRootPort"
pciBridge6.functions = "8"
pciBridge7.present = "TRUE"
pciBridge7.virtualDev = "pcieRootPort"
pciBridge7.functions = "8"
vmci0.present = "TRUE"
floppy0.present = "TRUE"
floppy0.fileType = "device"
floppy0.autodetect = "FALSE"
floppy0.startConnected = "FALSE"
floppy0.clientDevice = "TRUE"
floppy0.allowguestconnectioncontrol = "true"
ide1:0.clientDevice = "TRUE"
ide1:0.present = "TRUE"
ide1:0.deviceType = "cdrom-raw"
ide1:0.autodetect = "TRUE"
ide1:0.startConnected = "FALSE"
ide1:0.exclusive = "false"
ide1:0.allowguestconnectioncontrol = "true"
svga.autodetect = "false"
mks.enable3d = "false"
mks.use3dRenderer = "automatic"
svga.vramSize = "4194304"
scsi0:0.present = "TRUE"
scsi0:0.deviceType = "disk"
scsi0:0.fileName = "$OUT_BASE.vmdk"
scsi0:0.allowguestconnectioncontrol = "false"
scsi0:0.writeThrough = "false"
scsi0:0.mode = "persistent"
scsi0.virtualDev = "pvscsi"
scsi0.present = "TRUE"
vmci0.unrestricted = "false"
serial0.startConnected = "FALSE"
serial0.present = "TRUE"
serial0.autodetect = "FALSE"
serial0.fileName = "Serial Port 3"
serial0.yieldOnMsrRead = "false"
serial0.allowguestconnectioncontrol = "true"
ethernet0.present = "TRUE"
ethernet0.virtualDev = "vmxnet3"
ethernet0.connectionType = "bridged"
ethernet0.startConnected = "TRUE"
ethernet0.addressType = "generated"
ethernet0.wakeonpcktrcv = "true"
ethernet0.allowguestconnectioncontrol = "true"
vcpu.hotadd = "false"
vcpu.hotremove = "false"
mem.hotadd = "false"
firmware = "bios"
tools.syncTime = "false"
toolscripts.afterpoweron = "true"
toolscripts.afterresume = "true"
toolscripts.beforepoweroff = "true"
toolscripts.beforesuspend = "true"
tools.upgrade.policy = "manual"
powerType.powerOff = "preset"
powerType.reset = "preset"
powerType.suspend = "preset"
vhv.enable = "false"
chipset.onlineStandby = "FALSE"
EOF

  rm -f "$OUTDIR/$OUT_BASE.ova"
  ovftool "$work/$OUT_BASE.vmx" "$OUTDIR/$OUT_BASE.ova"
  # make readable so that apache2/httpd can serve it
  chmod a+r "$OUTDIR/$OUT_BASE.ova"
  rm -rf "$work"
  echo "Created $OUTDIR/$OUT_BASE.ova"
}

###
### Main
###

# remove the incomplete copy if patch or shrink fail, and the temporary files of any failure
CLEAN_PATHS=()
WORK_BUSY=""
cleanup() {
  local rc=$?
  local p
  for p in "${CLEAN_PATHS[@]}"; do rm -rf "$p"; done
  if [ $rc -ne 0 ] && [ -n "$WORK_BUSY" ]; then
    rm -f "$WORK_IMAGE"
    echo "ERROR: failed, removed the incomplete $WORK_IMAGE" >&2
  fi
}
trap cleanup EXIT

check_dependencies
# detect the OS before anything is created, so that an unsupported OS stops here
if [ -n "$DO_PATCH" ] || { [ -n "$WANT_OVA" ] && [ -z "$GUESTOS" ]; }; then
  load_os_release
fi
[ -n "$DO_PATCH" ] && detect_os
if [ -n "$WANT_OVA" ] && [ -z "$GUESTOS" ]; then
  GUESTOS=$(vmware_guestos)
  echo "VMware guestos: $GUESTOS (from ID='${OS_ID}', ID_LIKE='${OS_ID_LIKE}', VERSION_ID='${OS_VERSION_ID}')"
fi

if [ -n "$DO_PATCH$DO_SHRINK" ]; then
  cp --reflink=auto "$IMAGE" "$WORK_IMAGE"
  WORK_BUSY=1
  echo "Created $WORK_IMAGE from $IMAGE (the original is not modified)"
fi
[ -n "$DO_PATCH" ] && do_patch
[ -n "$DO_SHRINK" ] && do_shrink
WORK_BUSY=""
if [ -n "$DO_CONVERT" ]; then
  [ -n "$WANT_VHD" ] && do_convert_vhd
  [ -n "$WANT_OVA" ] && do_convert_ova
  if [ -n "$WANT_XEN" ]; then
    do_convert_xen
    # the plain .vhd is not kept when the XenServer .vhd.bz2 is built too
    if [ -n "$WANT_VHD" ]; then
      rm -f "$OUTDIR/$OUT_BASE.vhd"
      echo "Removed $OUTDIR/$OUT_BASE.vhd, as $OUT_BASE-xen.vhd.bz2 was created"
    fi
  fi
fi

# SHA512SUMS of the output files, when all is done; the entries of other files are kept
make_sha512sums() {
  local f n h
  local -A mine=()
  local -a names=()
  for f in "$(basename "$WORK_IMAGE")" "$OUT_BASE.vhd" "$OUT_BASE.ova" "$OUT_BASE-xen.vhd.bz2"; do
    [ -f "$OUTDIR/$f" ] || continue
    mine[$f]=1
    names+=("$f")
  done
  [ ${#names[@]} -gt 0 ] || return 0
  local tmp
  tmp=$(mktemp "$OUTDIR/.sums.XXXXXX")
  CLEAN_PATHS+=("$tmp")
  if [ -f "$OUTDIR/SHA512SUMS" ]; then
    while read -r h n; do
      [ -n "$n" ] && [ -z "${mine[${n#\*}]:-}" ] && echo "$h  $n" >> "$tmp"
    done < "$OUTDIR/SHA512SUMS"
  fi
  (cd "$OUTDIR" && sha512sum "${names[@]}") >> "$tmp"
  chmod a+r "$tmp"
  mv -f "$tmp" "$OUTDIR/SHA512SUMS"
  echo "Created $OUTDIR/SHA512SUMS"
}
make_sha512sums

echo "== Done =="
ls -lh "$WORK_IMAGE" "$OUTDIR/$OUT_BASE".vhd "$OUTDIR/$OUT_BASE".ova "$OUTDIR/$OUT_BASE"-xen.vhd.bz2 "$OUTDIR/SHA512SUMS" 2>/dev/null || true
exit 0
