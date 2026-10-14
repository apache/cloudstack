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

# Access to Ceph for kvmheartbeat_rbd.py and kvmvmactivity_rbd.py.
#
# The objects of the heartbeat and of the activity state, and the watchers of the
# volumes, are read and written over one connection to the Ceph cluster with the
# Python bindings of librados and librbd (the packages python3-rados and python3-rbd).
# An operation which does not finish in TIMEOUT seconds fails, so that it does not
# block forever if the cluster can not be reached.
#
# The cephx key is not an argument of the scripts, as it would be visible in the
# process list: the scripts read it from the environment variable CEPH_KEY, and give
# it to the bindings in their configuration.
#
# Nothing is written to the standard error output: the agent merges it with the
# standard output and reads only the first line of the result, so a message of
# librados would hide the result of the script.

import os

try:
    import rados
except ImportError:
    rados = None

try:
    import rbd
except ImportError:
    rbd = None

KEY_ENV = "CEPH_KEY"
TIMEOUT = 30
MAX_OBJECT_SIZE = 1024 * 1024


def silence_standard_error():
    """Sends the standard error output, which librados may write to, to /dev/null."""
    try:
        devnull = os.open(os.devnull, os.O_WRONLY)
        os.dup2(devnull, 2)
        os.close(devnull)
    except OSError:
        pass


class BindingsError(Exception):
    """The Python bindings of librados or librbd are not installed or can not be used."""


class CephError(Exception):
    """The cluster could not be connected, or an object could not be read, written or found out if it exists."""


class ObjectNotFoundError(CephError):
    """The object does not exist, which is different from a cluster which can not be reached."""


class WatchersError(Exception):
    """The watchers of a volume could not be listed: it is not known whether the host is alive."""


class Ceph:
    def __init__(self, mon_hosts, pool, user, key, watchers=False):
        """
        Connects to the cluster. Raises BindingsError if the bindings can not be used, in which case nothing can
        be done. If the cluster can not be connected, the operations raise CephError.

        :param watchers: whether the watchers of volumes are listed, which needs librbd
        """
        if rados is None:
            raise BindingsError("the package python3-rados is not installed")
        if watchers and (rbd is None or not hasattr(rbd.Image, "watchers_list")):
            raise BindingsError("the package python3-rbd is not installed, or is too old to list the watchers of a volume")
        self.pool = pool
        self.cluster = None
        self.ioctx = None
        conf = {"mon_host": mon_hosts, "rados_osd_op_timeout": str(TIMEOUT), "rados_mon_op_timeout": str(TIMEOUT),
                "client_mount_timeout": str(TIMEOUT), "log_to_stderr": "false", "err_to_stderr": "false"}
        if user:
            conf["key"] = key
        cluster = rados.Rados(rados_id=user if user else None, conf=conf)
        try:
            cluster.connect(timeout=TIMEOUT)
            self.ioctx = cluster.open_ioctx(pool)
            self.cluster = cluster
        except rados.Error:
            cluster.shutdown()

    def close(self):
        if self.ioctx is not None:
            self.ioctx.close()
        if self.cluster is not None:
            self.cluster.shutdown()
        self.ioctx = None
        self.cluster = None

    def _connected(self, name):
        if self.ioctx is None:
            raise CephError(name)
        return self.ioctx

    def exists(self, name):
        """Returns whether the object exists, or raises CephError if it is not known."""
        ioctx = self._connected(name)
        try:
            ioctx.stat(name)
            return True
        except rados.ObjectNotFound:
            return False
        except rados.Error:
            raise CephError(name)

    def read(self, name):
        """Returns the content of the object, or raises ObjectNotFoundError or CephError."""
        ioctx = self._connected(name)
        try:
            size = min(ioctx.stat(name)[0], MAX_OBJECT_SIZE)
            return ioctx.read(name, size).decode("utf-8", "replace") if size else ""
        except rados.ObjectNotFound:
            raise ObjectNotFoundError(name)
        except rados.Error:
            raise CephError(name)

    def write(self, name, content):
        """Writes the object, or raises CephError."""
        ioctx = self._connected(name)
        try:
            ioctx.write_full(name, content.encode("utf-8"))
        except rados.Error:
            raise CephError(name)

    def has_watcher(self, image):
        """
        Returns whether the volume has a watcher, None if the image does not exist (for example a volume which is
        deleted but still in the list of the management server), or raises WatchersError.
        """
        if self.ioctx is None:
            raise WatchersError(image)
        try:
            # read_only is needed: a volume which is not opened read-only gets a watcher of this script, which
            # would make every host look alive
            with rbd.Image(self.ioctx, image, read_only=True) as volume:
                return any(True for _ in volume.watchers_list())
        except rbd.ImageNotFound:
            return None
        except (rados.Error, rbd.Error):
            raise WatchersError(image)
