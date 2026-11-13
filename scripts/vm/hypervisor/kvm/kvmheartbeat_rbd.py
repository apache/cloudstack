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

# Ceph RBD flavor of kvmheartbeat.sh/kvmsmpheartbeat.sh.
#
# There is no shared POSIX mount point to write a heartbeat file to when the
# primary storage pool is Ceph RBD, so the heartbeat timestamp is instead
# stored as a small RADOS object (one object per host) in the same RBD pool.
# Any host with a working path to the Ceph cluster can write/read this object,
# which gives the same semantics as the NFS/SharedMountPoint heartbeat file.
#
# The object is written and read with the Python bindings of librados
# (python3-rados), see kvmha_rbd.py. The cephx key is not an argument, as it would be visible in the process list: it is
# read from the environment variable CEPH_KEY.
#
# A host is only reported as DEAD when its heartbeat is there and stale. If there is none (the agent of the host is not
# upgraded yet, or has no bindings, or the first heartbeat is not written yet), or if it can not be read, the state of the
# host is not known and the exit status is 2, as for a Ceph cluster which can not be reached.

import argparse
import os
import re
import subprocess
import sys
import time

from kvmha_rbd import BindingsError, Ceph, CephError, DEFAULT_WRITE_TIMEOUT, KEY_ENV, ObjectNotFoundError, check_bindings, heartbeat_max_age, silence_standard_error


class Parser(argparse.ArgumentParser):
    def error(self, message):
        self.print_usage(sys.stderr)
        sys.exit(1)


def main():
    parser = Parser(description="Writes or checks the heartbeat of a KVM host on Ceph RBD storage", add_help=False)
    parser.add_argument("-s", dest="mon_hosts", default="", help="ceph monitor host(s), comma separated")
    parser.add_argument("-o", dest="pool", default="", help="ceph/rbd pool name")
    parser.add_argument("-n", dest="user", default="", help="cephx auth user (optional, the key is then in " + KEY_ENV + ")")
    parser.add_argument("-h", dest="host", default="", help="host")
    parser.add_argument("-r", dest="check", action="store_true", help="read the heartbeat, instead of writing it")
    parser.add_argument("-c", dest="cleanup", action="store_true", help="cleanup: reboot the host")
    parser.add_argument("-t", dest="interval", default="", help="interval in seconds at which the heartbeat is written, when it is read")
    parser.add_argument("-w", dest="write_timeout", default="",
                        help="seconds after which the agent stops a write of the heartbeat, when it is read, default %d" % DEFAULT_WRITE_TIMEOUT)
    parser.add_argument("--check-bindings", dest="check_bindings", action="store_true",
                        help="only check that the Python bindings of librados and librbd are installed")
    parser.add_argument("--help", action="help")
    args = parser.parse_args()

    if args.check_bindings:
        # asked by the agent when it registers a storage pool for HA, the RBD scripts need both bindings
        try:
            check_bindings(watchers=True)
        except BindingsError as error:
            print("=====> The Python bindings of Ceph are missing, %s <======" % error)
            sys.exit(1)
        return

    # first of all: the Python bindings are only suggested by the agent packages. The self-fencing (-c) does not
    # need them, as it does not use Ceph
    if not args.cleanup:
        try:
            check_bindings()
        except BindingsError as error:
            if args.check:
                # not a proof that the host is dead, so DEAD is not reported
                print("=====> Unable to check the heartbeat, %s <======" % error)
                sys.exit(2)
            print("=====> Unable to write the heartbeat, %s <======" % error)
            sys.exit(1)

    key = os.environ.pop(KEY_ENV, "")
    if not args.mon_hosts or not args.pool:
        sys.exit(1)
    # the host (its GUID) names the heartbeat object, so it is required except for the self-fencing (-c)
    if not args.cleanup and not args.host:
        sys.exit(1)
    if args.user and not key:
        sys.exit(1)
    if args.check and not re.fullmatch(r"[0-9]+", args.interval):
        sys.exit(1)
    if args.write_timeout and not re.fullmatch(r"[0-9]+", args.write_timeout):
        sys.exit(1)
    write_timeout = int(args.write_timeout) if args.write_timeout else DEFAULT_WRITE_TIMEOUT

    if args.cleanup:
        reboot()
        return

    heartbeat_object = "KVMHA-hb-" + args.host
    silence_standard_error()
    ceph = Ceph(args.mon_hosts, args.pool, args.user, key)
    try:
        if args.check:
            check(ceph, heartbeat_object, args.pool, int(args.interval), write_timeout)
            return
        try:
            ceph.write(heartbeat_object, "%d\n" % int(time.time()))
        except CephError as error:
            print("=====> Unable to write the RADOS object [%s] in pool [%s]: %s <======" % (heartbeat_object, args.pool, error))
            sys.exit(1)
    finally:
        ceph.close()


def check(ceph, heartbeat_object, pool, interval, write_timeout):
    now = int(time.time())
    try:
        heartbeat = ceph.read(heartbeat_object).strip()
    except ObjectNotFoundError:
        # the host never wrote a heartbeat: its agent is not upgraded, or has no bindings, or the GUID is another one,
        # or it is the first cycle. That is not a proof that the host is dead
        print("=====> Unable to tell whether the host is alive, the RADOS object [%s] in pool [%s] does not exist <======"
              % (heartbeat_object, pool))
        sys.exit(2)
    except CephError as error:
        # Ceph can not be reached from this host, which says nothing about the host which is checked, as for the
        # VM activity check
        print("=====> Unable to read the RADOS object [%s] in pool [%s]: %s <======" % (heartbeat_object, pool, error))
        sys.exit(2)
    if not re.fullmatch(r"[0-9]+", heartbeat):
        print("=====> Unable to tell whether the host is alive, the RADOS object [%s] in pool [%s] does not hold a heartbeat <======"
              % (heartbeat_object, pool))
        sys.exit(2)
    age = now - int(heartbeat)
    max_age = heartbeat_max_age(interval, write_timeout)
    if age > max_age:
        print("=====> Considering host as DEAD because last write to RADOS object [%s] in pool [%s] was [%d] seconds ago, "
              "but the max age is [%d] (the interval of [%d] and the time which a write can take of [%d]) <======"
              % (heartbeat_object, pool, age, max_age, interval, write_timeout))
    else:
        print("=====> ALIVE <=====")


def reboot():
    subprocess.call(["/usr/bin/logger", "-t", "heartbeat",
                     "kvmheartbeat_rbd.py will reboot system because it was unable to write the heartbeat to the Ceph RBD storage."])
    # not waited for, the storage may be hanging
    subprocess.Popen(["sync"])
    time.sleep(5)
    try:
        with open("/proc/sysrq-trigger", "w") as trigger:
            trigger.write("b")
    except OSError:
        sys.exit(1)


if __name__ == "__main__":
    main()
