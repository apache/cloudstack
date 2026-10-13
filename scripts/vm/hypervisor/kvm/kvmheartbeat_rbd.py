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
# The cephx key is not an argument, as it would be visible in the process list:
# it is read from the environment variable CEPH_KEY, and is given to rados in
# the environment variable CEPH_ARGS.

import argparse
import os
import re
import subprocess
import sys
import time

KEY_ENV = "CEPH_KEY"


class Parser(argparse.ArgumentParser):
    def error(self, message):
        self.print_usage(sys.stderr)
        sys.exit(1)


def rados(args, env, data=None):
    """Runs rados and returns its exit status and its output."""
    try:
        result = subprocess.run(["rados"] + args, input=data, stdout=subprocess.PIPE,
                                stderr=subprocess.DEVNULL, env=env, universal_newlines=True)
    except OSError:
        return 127, ""
    return result.returncode, result.stdout


def main():
    parser = Parser(description="Writes or checks the heartbeat of a KVM host on Ceph RBD storage", add_help=False)
    parser.add_argument("-s", dest="mon_hosts", default="", help="ceph monitor host(s), comma separated")
    parser.add_argument("-o", dest="pool", default="", help="ceph/rbd pool name")
    parser.add_argument("-n", dest="user", default="", help="cephx auth user (optional, the key is then in " + KEY_ENV + ")")
    parser.add_argument("-h", dest="host", default="", help="host")
    parser.add_argument("-r", dest="check", action="store_true", help="read the heartbeat, instead of writing it")
    parser.add_argument("-c", dest="cleanup", action="store_true", help="cleanup: reboot the host")
    parser.add_argument("-t", dest="interval", default="", help="maximum age of the heartbeat in seconds, when it is read")
    parser.add_argument("--help", action="help")
    args = parser.parse_args()

    key = os.environ.pop(KEY_ENV, "")
    if not args.mon_hosts or not args.pool:
        sys.exit(1)
    # the host IP names the heartbeat object, so it is required except for the self-fencing (-c)
    if not args.cleanup and not args.host:
        sys.exit(1)
    if args.user and not key:
        sys.exit(1)
    if args.check and not re.fullmatch(r"[0-9]+", args.interval):
        sys.exit(1)

    if args.cleanup:
        print_and_reboot()
        return

    env = dict(os.environ)
    options = ["-p", args.pool, "--mon-host", args.mon_hosts]
    if args.user:
        options += ["--id", args.user]
        env["CEPH_ARGS"] = (env.get("CEPH_ARGS", "") + " --key=" + key).strip()

    heartbeat_object = "KVMHA-hb-" + args.host

    if args.check:
        sys.exit(check(options, env, heartbeat_object, args.pool, int(args.interval)))

    status, _ = rados(options + ["put", heartbeat_object, "-"], env, "%d\n" % time.time())
    sys.exit(0 if status == 0 else 1)


def check(options, env, heartbeat_object, pool, interval):
    now = int(time.time())
    status, output = rados(options + ["get", heartbeat_object, "-"], env)
    heartbeat = output.strip()
    if not re.fullmatch(r"[0-9]+", heartbeat):
        # Either the RADOS object doesn't exist yet (host never wrote a heartbeat)
        # or the Ceph cluster can't be reached right now. Either way we can't
        # confirm the host is alive, so fail safe and report it as DEAD.
        print("=====> Considering host as DEAD because RADOS object [%s] in pool [%s] could not be read <======"
              % (heartbeat_object, pool))
        return 0
    age = now - int(heartbeat)
    if age > interval:
        print("=====> Considering host as DEAD because last write to RADOS object [%s] in pool [%s] was [%d] seconds ago, "
              "but the max interval is [%d] <======" % (heartbeat_object, pool, age, interval))
    else:
        print("=====> ALIVE <=====")
    return 0


def print_and_reboot():
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
