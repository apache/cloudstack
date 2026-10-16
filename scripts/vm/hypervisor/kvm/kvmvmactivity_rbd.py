#!/usr/bin/env python3
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

# Ceph RBD flavor of kvmvmactivity.sh.
#
# On NFS/SharedMountPoint storage, VM disk activity is detected via the mtime
# of the volume files on the shared mount point. RBD volumes aren't files on
# a mount point, so instead activity is detected via RBD watchers: as long as
# qemu has an RBD image open (i.e. a VM using that volume is running
# somewhere), the image will have a live watcher. The most recent
# suspect-time/watcher-state is persisted as a RADOS object (per host)
# in place of the "ac-<host>" file used by the NFS/SMP scripts.
#
# The volumes are given in -u, or in a file with -U, which has no limit on the number
# of volumes: one argument of a command is limited to 128 KiB, which is about 3500
# volumes. The file has the uuids separated by commas or by lines.
#
# The objects and the watchers are read and written over one connection to the Ceph
# cluster with the Python bindings of librados and librbd (python3-rados and
# python3-rbd), which is much faster than "rbd status" for each volume when a host has
# many volumes, see kvmha_rbd.py. The cephx key is not an argument, as it would be
# visible in the process list: it is read from the environment variable CEPH_KEY.
#
# Exit status: 0 with ALIVE or DEAD in the output, 2 if the arguments are not
# valid or if the state of the host could not be found out, which is not a DEAD host.

import argparse
import os
import re
import sys
import time

from kvmha_rbd import BindingsError, Ceph, CephError, WatchersError, KEY_ENV, silence_standard_error


class Parser(argparse.ArgumentParser):
    def error(self, message):
        self.print_usage(sys.stderr)
        sys.exit(1)


def alive():
    print("=====> ALIVE <=====")
    sys.exit(0)


def fail(message):
    print("=====> %s <======" % message)
    sys.exit(2)


def main():
    parser = Parser(description="Checks the VM activity of a KVM host on Ceph RBD storage", add_help=False)
    parser.add_argument("-s", dest="mon_hosts", default="", help="ceph monitor host(s), comma separated")
    parser.add_argument("-o", dest="pool", default="", help="ceph/rbd pool name")
    parser.add_argument("-n", dest="user", default="", help="cephx auth user (optional, the key is then in " + KEY_ENV + ")")
    parser.add_argument("-h", dest="host", default="", help="host")
    parser.add_argument("-u", dest="images", default="", help="volume (rbd image) uuid list, separated by commas")
    parser.add_argument("-U", dest="images_file", default="",
                        help="file with the volume (rbd image) uuid list, separated by commas or by lines, instead of -u")
    parser.add_argument("-t", dest="time", default="",
                        help="current time in seconds (accepted for compatibility with kvmvmactivity.sh, not used)")
    parser.add_argument("-d", dest="suspect_time", default="", help="suspect time")
    parser.add_argument("--help", action="help")
    args = parser.parse_args()

    key = os.environ.pop(KEY_ENV, "")
    if not args.mon_hosts or not args.pool or not args.suspect_time:
        sys.exit(2)
    # the host (its GUID) names the heartbeat and activity objects
    if not args.host:
        sys.exit(2)
    if args.user and not key:
        sys.exit(2)
    if not re.fullmatch(r"[0-9]+", args.suspect_time):
        sys.exit(2)
    suspect_time = int(args.suspect_time)
    volumes = args.images
    if args.images_file:
        try:
            with open(args.images_file) as images_file:
                volumes = images_file.read()
        except OSError:
            sys.exit(2)

    silence_standard_error()
    try:
        ceph = Ceph(args.mon_hosts, args.pool, args.user, key, watchers=True)
    except BindingsError as error:
        fail("Unable to check the VM activity, %s" % error)
    try:
        check(ceph, args.pool, args.host, volumes, suspect_time)
    finally:
        ceph.close()


def check(ceph, pool, host, volumes, suspect_time):
    heartbeat_object = "KVMHA-hb-" + host
    activity_object = "KVMHA-ac-" + host

    # the reason why the heartbeat did not show that the host is alive, added to the messages about a DEAD host
    heartbeat_note = ""

    def dead(reason):
        print("=====> Considering host as DEAD due to %s.%s <======" % (reason, heartbeat_note))
        sys.exit(0)

    # First check: heartbeat object, same as kvmheartbeat_rbd.py
    now = int(time.time())
    try:
        heartbeat = ceph.read(heartbeat_object).strip()
        if not re.fullmatch(r"[0-9]+", heartbeat):
            heartbeat_note = " The RADOS object [%s] does not hold a heartbeat." % heartbeat_object
        else:
            age = now - int(heartbeat)
            if age < 61:
                alive()
            heartbeat_note = " The heartbeat in RADOS object [%s] is [%d] seconds old." % (heartbeat_object, age)
    except CephError:
        heartbeat_note = " The RADOS object [%s] could not be read, it does not exist or Ceph can not be reached." % heartbeat_object

    if not volumes.strip():
        dead("empty UUIDList")

    # Second check: RBD watcher based disk activity check.
    # If any of the host's volumes still has a live watcher, something (most
    # likely qemu on the host being checked) is actively using it right now.
    images = volumes.replace("\n", ",").split(",")
    images = ["".join(image.split()) for image in images]
    images = [image for image in images if image]
    has_watcher = False
    existing = 0
    try:
        for image in images:
            watched = ceph.has_watcher(image)
            if watched is None:
                # a volume which was deleted, but is still in the list, does not matter for the others
                continue
            existing += 1
            if watched:
                has_watcher = True
                break
    except WatchersError as error:
        # when the watchers can not be listed, it is unknown whether the host is alive: the script fails,
        # which is not a DEAD host
        fail("Unable to get the watchers of the image [%s/%s]" % (pool, error))
    if existing == 0:
        # none of the images could be looked at, maybe the names are wrong: it is not known whether the host is alive
        fail("None of the images of the volumes in pool [%s] exists" % pool)
    latest_update_time = now if has_watcher else 0

    state = ""
    try:
        if ceph.exists(activity_object):
            state = ceph.read(activity_object)
    except CephError:
        fail("Unable to read the RADOS object [%s]" % activity_object)
    # a state which is not "<suspect time>:<update time>" (with an optional third field of older versions)
    # is ignored, as if there was none
    match = re.fullmatch(r"([0-9]+):([0-9]+)(:[0-9]+)?", state.strip())

    try:
        ceph.write(activity_object, "%d:%d\n" % (suspect_time, latest_update_time))
    except CephError:
        # without the new state the next check would use an old one, so it is not known whether the host is alive
        fail("Unable to write the RADOS object [%s]" % activity_object)

    if not match:
        if latest_update_time > suspect_time:
            alive()
        dead("RADOS object [%s] did not exist or holds no state, and condition [latestUpdateTime -gt SuspectTime] "
             "has not been satisfied" % activity_object)

    last_suspect_time = int(match.group(1))
    last_update_time = int(match.group(2))
    if suspect_time - last_suspect_time < 0:
        if latest_update_time > suspect_time:
            alive()
        dead("RADOS object [%s] exists, condition [suspectTimeDiff -lt 0] was satisfied and "
             "[latestUpdateTime -gt SuspectTime] has not been satisfied" % activity_object)
    if latest_update_time > last_update_time:
        alive()
    dead("RADOS object [%s] exists, condition [suspectTimeDiff -lt 0] was not satisfied and "
         "[latestUpdateTime -gt lastUpdateTime] has not been satisfied" % activity_object)


if __name__ == "__main__":
    main()
