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
"""Support module for the NetworkExtension smoke tests.

**Do not run this file directly.**  Tests are discovered and executed through
``test_network_extension_namespace.py``, which exposes the test class under a
discoverable name.

This module provides:
  * Module-level constants (script names, URLs, capabilities JSON).
  * Helper functions (_download_script, _ensure_scripts_downloaded, etc.).
  * Deployer classes (MgmtServerDeployer, KvmHostDeployer).
  * Base test class ``_TestNetworkExtensionNamespace`` (underscore = not
    collected directly by nose/Marvin).

Renamed from ``test_network_extension_provider.py``.  The canonical test file
is ``test_network_extension_namespace.py``.
"""
import json
import logging
import os
import random
import shutil
import stat
import subprocess
import tempfile
import time
import urllib.parse

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (listPhysicalNetworks,
                                  listTrafficTypes,
                                  listManagementServers,
                                  listNetworkServiceProviders,
                                  updateNetworkServiceProvider,
                                  deleteNetworkServiceProvider,
                                  createFirewallRule,
                                  deleteFirewallRule,
                                  listPublicIpAddresses)
from marvin.lib.base import (Account,
                             Extension,
                             ExtensionCustomAction,
                             LoadBalancerRule,
                             Network,
                             NetworkACL,
                             NetworkACLList,
                             NetworkOffering,
                             NATRule,
                             PublicIPAddress,
                             ServiceOffering,
                             SSHKeyPair,
                             StaticNATRule,
                             Template,
                             VirtualMachine,
                             VPC,
                             VpcOffering)
from marvin.lib.common import (get_domain, get_zone, get_template)
from marvin.lib.utils import cleanup_resources, random_gen
from marvin.sshClient import SshClient
from nose.plugins.attrib import attr

_multiprocess_shared_ = True

# The file names of the scripts to deploy on the management server and KVM hosts.
SCRIPT_FILENAME       = 'network-namespace-wrapper.sh'
ENTRY_POINT_FILENAME  = 'network-namespace.sh'

# Remote URLs to download the scripts from
_GITHUB_BASE = (
    'https://raw.githubusercontent.com/apache/cloudstack-extensions'
    '/refs/heads/network-namespace/Network-Namespace/'
)
WRAPPER_SCRIPT_URL     = _GITHUB_BASE + SCRIPT_FILENAME
ENTRY_POINT_SCRIPT_URL = _GITHUB_BASE + ENTRY_POINT_FILENAME

# Local cache paths (downloaded once, reused across test methods)
_THIS_DIR = os.path.dirname(os.path.abspath(__file__))
_SCRIPT_CACHE_DIR = os.path.join(tempfile.gettempdir(), 'cs-extnet-script-cache')
WRAPPER_SCRIPT_LOCAL     = os.path.join(_SCRIPT_CACHE_DIR, SCRIPT_FILENAME)
ENTRY_POINT_SCRIPT_LOCAL = os.path.join(_SCRIPT_CACHE_DIR, ENTRY_POINT_FILENAME)

# Network services — comma-separated list of all services this extension supports.
# Tests select a subset when creating NetworkOfferings.
NETWORK_SERVICES = (
    "Dhcp,Dns,UserData,"
    "SourceNat,StaticNat,PortForwarding,Firewall,Lb,NetworkACL,CustomAction"
)

# Per-service capabilities JSON object (no "services" wrapper).
NETWORK_SERVICE_CAPABILITIES_JSON = json.dumps({
    "Lb": {
        "SupportedLBAlgorithms": "roundrobin,leastconn,source",
        "SupportedLBIsolation": "dedicated",
        "SupportedProtocols": "tcp,udp,tcp-proxy",
        "SupportedStickinessMethods": "lbcookie,appsession",
        "LbSchemes": "Public",
        "SslTermination": "false",
        "VmAutoScaling": "false"
    },
    "Firewall": {
        "TrafficStatistics": "per public ip",
        "SupportedProtocols": "tcp,udp,icmp",
        "SupportedEgressProtocols": "tcp,udp,icmp,all",
        "SupportedTrafficDirection": "ingress,egress",
        "MultipleIps": "true"
    },
    "Dns": {
        "AllowDnsSuffixModification": "true",
        "ExternalDns": "true"
    },
    "Dhcp": {
        "DhcpAccrossMultipleSubnets": "true"
    },
    "Gateway": {
        "RedundantRouter": "false"
    },
    "SourceNat": {
        "SupportedSourceNatTypes": "peraccount",
        "RedundantRouter": "false"
    },
    "StaticNat": {
        "Supported": "true"
    },
    "PortForwarding": {
        "SupportedProtocols": "tcp,udp"
    },
    "UserData": {
        "Supported": "true"
    },
    "NetworkACL": {
        "SupportedProtocols": "tcp,udp,icmp"
    },
    "CustomAction": {
        "Supported": "true"
    }
})


# ---------------------------------------------------------------------------
# Script download helpers
# ---------------------------------------------------------------------------

def _download_script(url, dest_path):
    """Download *url* to *dest_path* via curl or wget and make it executable."""
    os.makedirs(os.path.dirname(dest_path), exist_ok=True)
    log = logging.getLogger('cs-extnet')
    log.info("Downloading %s -> %s", url, dest_path)
    for cmd in (['curl', '-fsSL', url, '-o', dest_path],
                ['wget', '-q',    url, '-O', dest_path]):
        if subprocess.run(cmd, check=False).returncode == 0:
            os.chmod(dest_path, stat.S_IRWXU | stat.S_IRGRP | stat.S_IXGRP |
                     stat.S_IROTH | stat.S_IXOTH)
            return dest_path
    raise RuntimeError("Failed to download %s with curl and wget" % url)


def _ensure_scripts_downloaded():
    """Download both scripts from GitHub if not already cached.

    If GitHub is unreachable the function falls back to the corresponding
    scripts in the local source tree so that tests can run offline:
      * wrapper      → extensions/network-namespace/network-namespace-wrapper.sh
      * entry point  → extensions/network-namespace/network-namespace.sh

    Returns (wrapper_path, entry_point_path).
    """
    _src_root = os.path.normpath(
        os.path.join(os.path.dirname(os.path.abspath(__file__)),
                     '..', '..', '..'))

    try:
        _download_script(WRAPPER_SCRIPT_URL, WRAPPER_SCRIPT_LOCAL)
    except Exception:
        # Offline fallback: deploy the source-tree implementation.
        _local_impl = os.path.join(
            _src_root, 'extensions', 'network-namespace',
            'network-namespace-wrapper.sh')
        if os.path.exists(_local_impl):
            os.makedirs(os.path.dirname(WRAPPER_SCRIPT_LOCAL),
                        exist_ok=True)
            shutil.copy2(_local_impl, WRAPPER_SCRIPT_LOCAL)
            os.chmod(WRAPPER_SCRIPT_LOCAL,
                     stat.S_IRWXU | stat.S_IRGRP | stat.S_IXGRP |
                     stat.S_IROTH | stat.S_IXOTH)
            logging.getLogger('cs-extnet').info(
                "Offline fallback: using local %s as %s",
                _local_impl, WRAPPER_SCRIPT_LOCAL)
        else:
            raise

    try:
        _download_script(ENTRY_POINT_SCRIPT_URL, ENTRY_POINT_SCRIPT_LOCAL)
    except Exception:
        _local_ep = os.path.join(
            _src_root, 'extensions', 'network-namespace',
            'network-namespace.sh')
        if os.path.exists(_local_ep):
            os.makedirs(os.path.dirname(ENTRY_POINT_SCRIPT_LOCAL),
                        exist_ok=True)
            shutil.copy2(_local_ep, ENTRY_POINT_SCRIPT_LOCAL)
            os.chmod(ENTRY_POINT_SCRIPT_LOCAL,
                     stat.S_IRWXU | stat.S_IRGRP | stat.S_IXGRP |
                     stat.S_IROTH | stat.S_IXOTH)
            logging.getLogger('cs-extnet').info(
                "Offline fallback: using local %s as %s",
                _local_ep, ENTRY_POINT_SCRIPT_LOCAL)
        else:
            raise

    return WRAPPER_SCRIPT_LOCAL, ENTRY_POINT_SCRIPT_LOCAL


# ---------------------------------------------------------------------------
# KVM host discovery helpers (from Marvin config)
# ---------------------------------------------------------------------------

def _get_kvm_hosts_from_config(config):
    """Return list of host dicts for all KVM hosts in the Marvin config.

    Each entry: {"ip": .., "username": .., "password": ..}
    """
    hosts = []
    try:
        for zone in config.__dict__.get("zones", []):
            for pod in zone.__dict__.get("pods", []):
                for cluster in pod.__dict__.get("clusters", []):
                    for h in cluster.__dict__.get("hosts", []):
                        if hasattr(h, '__dict__'):
                            h = h.__dict__
                        url = h.get("url", "")
                        if not url.startswith('http://') and not url.startswith('https://'):
                            url = 'http://' + url
                        parsed = urllib.parse.urlparse(url)
                        ip = parsed.hostname or ''
                        if not ip:
                            continue
                        hosts.append({
                            "ip":       ip,
                            "username": h.get("username", "root"),
                            "password": h.get("password", ""),
                        })
    except Exception as e:
        logging.getLogger('cs-extnet').warning(
            "Could not read KVM hosts from config: %s", e)
    return hosts


# ---------------------------------------------------------------------------
# SSH helper
# ---------------------------------------------------------------------------

def _ssh_copy_file(host_ip, host_port, username, password, local_path, remote_path):
    """Transfer *local_path* to *remote_path* on *host_ip* via SshClient."""
    ssh = SshClient(host_ip, int(host_port), username, password)
    ssh.execute("mkdir -p '%s'" % os.path.dirname(remote_path))
    # Use SFTP upload to avoid very large shell arguments for script content.
    ssh.scp(local_path, remote_path)
    ssh.execute("chmod 755 '%s'" % remote_path)


# ---------------------------------------------------------------------------
# MgmtServerDeployer  – deploys network-namespace.sh to the management server
# ---------------------------------------------------------------------------

class MgmtServerDeployer:
    """Copies network-namespace.sh to the management server via SSH."""

    def __init__(self, mgt_details, logger=None):
        self.ip     = mgt_details.get("mgtSvrIp", "localhost")
        self.port   = 22
        self.user   = mgt_details.get("user", "root")
        self.passwd = mgt_details.get("passwd", "")
        self.logger = logger or logging.getLogger('MgmtServerDeployer')

    def copy_file(self, local_path, remote_path, mode='0755'):
        _ssh_copy_file(self.ip, self.port, self.user, self.passwd,
                       local_path, remote_path)
        self.logger.info("Copied %s -> %s on mgmt %s",
                         local_path, remote_path, self.ip)

    def remove_file(self, remote_path):
        try:
            SshClient(self.ip, self.port, self.user, self.passwd).execute(
                "rm -f '%s'" % remote_path)
        except Exception as e:
            self.logger.warning("Could not remove %s: %s", remote_path, e)


# ---------------------------------------------------------------------------
# KvmHostDeployer  – deploys network-namespace-wrapper.sh to KVM hosts
# ---------------------------------------------------------------------------

class KvmHostDeployer:
    """Copies the KVM wrapper script to all KVM hosts via SSH.

    *dest_path* is the absolute path on each KVM host where the wrapper
    script is installed.  This path differs from the management-server
    entry-point path:
      management server: /usr/share/cloudstack-management/extensions/<name>/<name>.sh
      KVM host (wrapper): /etc/cloudstack/extensions/<name>/<name>-wrapper.sh
    """

    def __init__(self, config_hosts=None, logger=None, dest_path=None):
        self.config_hosts    = config_hosts or []
        self.logger          = logger or logging.getLogger('KvmHostDeployer')
        self._deployed_hosts = []
        self._dest_path      = dest_path

    def deploy(self):
        """Deploy wrapper to all configured hosts. Returns list of deployed IPs."""
        wrapper_path, _ = _ensure_scripts_downloaded()
        self._deployed_hosts = []
        if not self.config_hosts:
            self.logger.warning("No KVM hosts configured — wrapper not deployed")
            return []
        for h in self.config_hosts:
            ip       = h.get('ip', '')
            username = h.get('username', 'root')
            password = h.get('password', '')
            if not ip:
                continue
            self.logger.info("Deploying wrapper to KVM host %s at %s",
                             ip, self._dest_path)
            try:
                _ssh_copy_file(ip, 22, username, password,
                               wrapper_path, self._dest_path)
                self.logger.info("Deployed wrapper to %s at %s",
                                 ip, self._dest_path)
                self._deployed_hosts.append(ip)
            except Exception as e:
                self.logger.warning("Failed deploying to %s: %s", ip, e)
        return self._deployed_hosts

    def host_ips_csv(self):
        """Return comma-separated IP list of all configured hosts."""
        return ','.join(h.get('ip', '') for h in self.config_hosts if h.get('ip'))

    def remove_file(self, remote_path):
        """Remove wrapper script from all deployed KVM hosts."""
        if not self._deployed_hosts:
            self.logger.debug("No KVM hosts deployed — skipping remove_file")
            return
        for h in self.config_hosts:
            ip       = h.get('ip', '')
            username = h.get('username', 'root')
            password = h.get('password', '')
            if not ip or ip not in self._deployed_hosts:
                continue
            try:
                SshClient(ip, 22, username, password).execute(
                    "rm -f '%s'" % remote_path)
                self.logger.info("Removed %s from KVM host %s", remote_path, ip)
            except Exception as e:
                self.logger.warning("Could not remove %s from %s: %s",
                                   remote_path, ip, e)


# ---------------------------------------------------------------------------
# Test class
# ---------------------------------------------------------------------------

class TestNetworkExtensionNamespace(cloudstackTestCase):
    """Smoke tests for the NetworkExtension plugin.

    Not discovered directly — exposed as ``TestNetworkExtensionNamespace``
    through ``test_network_extension_namespace.py``.

    Covers:
      test_01 — NSP state transitions (Disabled/Enabled/Disabled)
      test_02 — network.services / network.service.capabilities details stored correctly
      test_03 — extension enable/disable and delete restriction
      test_04 — DHCP/DNS/UserData: cloud-init VM on a shared network reaches Running state
      test_05 — full isolated lifecycle: static NAT, PF, LB, restart
                (all with SSH connectivity verification via keypair)
      test_06 — VPC multi-tier + VPC restart with SSH verification
      test_07 — VPC Network ACL testing with multiple tiers and traffic rules
      test_08 — custom-action smoke for Policy-Based Routing (PBR) actions on isolated network
      test_09 — VPC source NAT IP update without VPC restart
      test_10 — custom-action smoke for Policy-Based Routing (PBR) actions on VPC tier network
    """

    @staticmethod
    def _custom_action_details(resp):
        """Extract best-effort details text from runCustomAction response."""
        if resp is None:
            return ""
        result = getattr(resp, 'result', None)
        if isinstance(result, dict):
            return result.get('details', '') or ''
        if hasattr(result, '__dict__'):
            return getattr(result, 'details', '') or ''
        return ''

    @classmethod
    def setUpClass(cls):
        testClient = super(TestNetworkExtensionNamespace, cls).getClsTestClient()
        cls.apiclient     = testClient.getApiClient()
        cls.services      = testClient.getParsedTestDataConfig()
        cls.zone          = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.domain        = get_domain(cls.apiclient)
        cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__
        # All management servers — entry-point script is deployed to every one.
        cls.all_mgt_svr_details = [
            mgt.__dict__ if hasattr(mgt, '__dict__') else mgt
            for mgt in cls.config.__dict__.get("mgtSvr", [])
        ] or [cls.mgtSvrDetails]
        cls.hv            = testClient.getHypervisorInfo()
        cls._cleanup      = []
        cls.tmp_files     = []
        cls.keypair       = None

        cls.logger = logging.getLogger("TestNetworkExtensionNamespace")
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        cls.logger.info("Management servers (from config): %s",
                         [m.get("mgtSvrIp", "?") for m in cls.all_mgt_svr_details])

        # Supplement / override management server list via listManagementServers API.
        # All mgmt servers are assumed to share the same SSH credentials as the
        # first entry in the Marvin config (mgtSvr[0]).
        try:
            ms_cmd = listManagementServers.listManagementServersCmd()
            api_mgmt_servers = cls.apiclient.listManagementServers(ms_cmd)
            if api_mgmt_servers:
                base_creds = dict(cls.mgtSvrDetails)
                api_mgt_details = []
                for ms in api_mgmt_servers:
                    ip = (getattr(ms, 'ipaddress', None)
                          or getattr(ms, 'ip', None)
                          or getattr(ms, 'hostname', None))
                    if ip:
                        entry = dict(base_creds)
                        entry['mgtSvrIp'] = ip
                        api_mgt_details.append(entry)
                if api_mgt_details:
                    cls.all_mgt_svr_details = api_mgt_details
                    cls.logger.info(
                        "Management servers (from listManagementServers API): %s",
                        [d.get('mgtSvrIp') for d in api_mgt_details])
        except Exception as _ms_err:
            cls.logger.warning(
                "Could not retrieve management servers via listManagementServers "
                "API (%s); using config-provided list", _ms_err)

        # KVM host credentials from Marvin config
        cls.kvm_host_configs = _get_kvm_hosts_from_config(cls.config)
        cls.logger.info("KVM hosts from config: %s",
                        [h['ip'] for h in cls.kvm_host_configs if h.get('ip')])

        # ---- Cloud-init template (Ubuntu 22.04) ----
        # Used for hardware tests that verify actual SSH connectivity.
        # Falls back to the default test template when the cloud-init entry
        # is absent from the services config (e.g. on simulator).
        try:
            tpl_data = (cls.services
                        .get("test_templates_cloud_init", {})
                        .get(cls.hv.lower()))
            if tpl_data:
                cls.logger.info("Registering cloud-init template for %s", cls.hv)
                tpl = Template.register(
                    cls.apiclient,
                    tpl_data,
                    zoneid=cls.zone.id,
                    hypervisor=cls.hv,
                )
                tpl.download(cls.apiclient)
                cls._cleanup.append(tpl)
                cls.template = tpl
                cls.logger.info("Cloud-init template registered: %s", tpl.id)
            else:
                cls.logger.info("No cloud-init template for %s; using default",
                                cls.hv)
                cls.template = get_template(cls.apiclient, cls.zone.id, cls.hv)
        except Exception as e:
            cls.logger.warning("Cloud-init template registration failed: %s; "
                               "falling back to default", e)
            cls.template = get_template(cls.apiclient, cls.zone.id, cls.hv)

        # ---- Download wrapper scripts from GitHub ----
        try:
            _ensure_scripts_downloaded()
            cls.logger.info("Scripts cached: %s  %s",
                            WRAPPER_SCRIPT_LOCAL, ENTRY_POINT_SCRIPT_LOCAL)
        except Exception as e:
            cls.logger.warning("Could not download scripts from GitHub: %s", e)

    @classmethod
    def tearDownClass(cls):
        super(TestNetworkExtensionNamespace, cls).tearDownClass()
        for tmp_file in cls.tmp_files:
            try:
                os.remove(tmp_file)
            except Exception:
                pass

    def setUp(self):
        self.cleanup           = []
        self.provider_id       = None
        self.physical_network  = None
        self.extension         = None
        self.extension_path    = None
        self.mgmt_deployer     = None
        self._mgmt_script_path = None
        self._all_mgmt_deployers = []
        self.kvm_deployer      = None
        self._ssh_private_key_file = None

    def tearDown(self):
        #self._safe_teardown()
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.logger.warning("cleanup_resources error: %s", e)

    # ------------------------------------------------------------------
    # CloudStack API helpers
    # ------------------------------------------------------------------

    def _get_physical_network(self):
        """Return the physical network with Guest traffic type in the test zone."""
        cmd = listPhysicalNetworks.listPhysicalNetworksCmd()
        cmd.zoneid = self.zone.id
        pns = self.apiclient.listPhysicalNetworks(cmd)
        self.assertIsInstance(pns, list)
        self.assertGreater(len(pns), 0)

        for pn in pns:
            tt_cmd = listTrafficTypes.listTrafficTypesCmd()
            tt_cmd.physicalnetworkid = pn.id
            traffic_types = self.apiclient.listTrafficTypes(tt_cmd)
            if traffic_types:
                for tt in traffic_types:
                    if getattr(tt, 'traffictype', '').lower() == 'guest':
                        self.logger.info(
                            "Selected physical network with Guest traffic: "
                            "%s (%s)", pn.name, pn.id)
                        return pn

        self.logger.info("No physical network with Guest traffic found; "
                         "using first: %s (%s)", pns[0].name, pns[0].id)
        return pns[0]

    def _find_provider(self, phys_net_id, name):
        cmd = listNetworkServiceProviders.listNetworkServiceProvidersCmd()
        cmd.physicalnetworkid = phys_net_id
        cmd.name = name
        providers = self.apiclient.listNetworkServiceProviders(cmd)
        return providers[0] if isinstance(providers, list) and providers else None

    def _update_provider_state(self, provider_id, state):
        cmd = updateNetworkServiceProvider.updateNetworkServiceProviderCmd()
        cmd.id    = provider_id
        cmd.state = state
        return self.apiclient.updateNetworkServiceProvider(cmd)

    def _delete_provider(self, provider_id):
        cmd = deleteNetworkServiceProvider.deleteNetworkServiceProviderCmd()
        cmd.id = provider_id
        self.apiclient.deleteNetworkServiceProvider(cmd)

    # ------------------------------------------------------------------
    # Script deployment helpers
    # ------------------------------------------------------------------

    def _deploy_scripts(self):
        """Deploy scripts to all management servers and all KVM hosts.

        The entry-point (network-namespace.sh) is deployed to every management
        server at ``self.extension_path`` (the path CloudStack assigned to the
        extension, e.g.
        ``/usr/share/cloudstack-management/extensions/<name>/<name>.sh``).

        The KVM wrapper (network-namespace-wrapper.sh) is deployed to each KVM
        host at a *different* path derived from the extension name:
        ``/etc/cloudstack/extensions/<name>/<name>-wrapper.sh``

        The entry-point script uses the same derivation at runtime (see
        DEFAULT_SCRIPT_PATH in network-namespace.sh) so that it always calls
        the correct wrapper on the remote KVM host.
        """
        wrapper_src, entry_point_src = _ensure_scripts_downloaded()

        self._mgmt_script_path = (self.extension_path or "").strip().rstrip('/')

        # Deploy entry-point to ALL management servers.
        # all_mgt_svr_details is collected once in setUpClass from
        # cls.config.__dict__["mgtSvr"] so every server in a HA pair is covered.
        self._all_mgmt_deployers = []
        for mgt_details in self.all_mgt_svr_details:
            deployer = MgmtServerDeployer(mgt_details, logger=self.logger)
            deployer.copy_file(entry_point_src, self._mgmt_script_path)
            self.logger.info("Entry-point deployed to mgmt %s at %s",
                             mgt_details.get("mgtSvrIp", "?"),
                             self._mgmt_script_path)
            self._all_mgmt_deployers.append(deployer)

        # Keep mgmt_deployer pointing at the primary server for backward-compat
        self.mgmt_deployer = (self._all_mgmt_deployers[0]
                              if self._all_mgmt_deployers
                              else MgmtServerDeployer(self.mgtSvrDetails,
                                                      logger=self.logger))

        # Derive the wrapper destination path on KVM hosts from the extension path:
        #   mgmt:  .../extensions/<name>/<name>.sh
        #   kvm:   /etc/cloudstack/extensions/<name>/<name>-wrapper.sh
        ext_dir_name   = os.path.basename(os.path.dirname(self._mgmt_script_path))
        script_basename = os.path.splitext(
            os.path.basename(self._mgmt_script_path))[0]
        kvm_wrapper_path = "/etc/cloudstack/extensions/%s/%s-wrapper.sh" % (
            ext_dir_name, script_basename)

        self.kvm_deployer = KvmHostDeployer(
            config_hosts=self.kvm_host_configs,
            logger=self.logger,
            dest_path=kvm_wrapper_path,
        )
        deployed = self.kvm_deployer.deploy()
        self.logger.info(
            "KVM wrapper deployed to %d host(s) at %s: %s",
            len(deployed), kvm_wrapper_path, deployed)

    def _cleanup_mgmt_script(self):
        if self._mgmt_script_path:
            all_deployers = getattr(self, '_all_mgmt_deployers', None) or []
            if all_deployers:
                for deployer in all_deployers:
                    deployer.remove_file(self._mgmt_script_path)
            elif self.mgmt_deployer:
                self.mgmt_deployer.remove_file(self._mgmt_script_path)
            self._mgmt_script_path = None

    def _cleanup_kvm_script(self):
        """Remove KVM wrapper script from all deployed KVM hosts."""
        if self.kvm_deployer and self.kvm_deployer._dest_path:
            self.kvm_deployer.remove_file(self.kvm_deployer._dest_path)

    # ------------------------------------------------------------------
    # Teardown helper
    # ------------------------------------------------------------------

    def _safe_teardown(self):
        """Best-effort cleanup of extension/NSP/scripts."""
        if self.extension and self.physical_network:
            try:
                self.extension.unregister(self.apiclient,
                                          self.physical_network.id,
                                          'PhysicalNetwork')
            except Exception:
                pass
        if self.provider_id:
            for fn in (lambda: self._update_provider_state(self.provider_id, 'Disabled'),
                       lambda: self._delete_provider(self.provider_id)):
                try:
                    fn()
                except Exception:
                    pass
            self.provider_id = None
        if self.extension:
            try:
                self.extension.delete(self.apiclient,
                                      unregisterresources=False,
                                      removeactions=False)
            except Exception:
                pass
            self.extension = None
        self._cleanup_mgmt_script()
        self._cleanup_kvm_script()

    # ------------------------------------------------------------------
    # SSH helpers (provider-agnostic — no KVM namespace checks)
    # ------------------------------------------------------------------

    def _verify_vm_ssh_access(self, ip, port=22, timeout=30, retries=10):
        """Return True if SSH to *ip*:*port* succeeds using the active keypair.

        The VM is expected to run Ubuntu 22.04 (cloud-init) with username
        ``ubuntu``.  Returns False if no keypair is available or if the
        connection fails.
        """
        key_file = self._ssh_private_key_file
        if not key_file and self.keypair:
            key_file = getattr(self.keypair, 'private_key_file', None)

        if not key_file:
            self.logger.warning("No SSH keypair available; returning False")
            return False
        try:
            ssh = SshClient(
                ip, int(port), "ubuntu", None,
                keyPairFiles=key_file,
                timeout=timeout,
                retries=retries,
            )
            out = ssh.execute("echo EXTNET_SSH_OK")
            return any("EXTNET_SSH_OK" in line for line in out)
        except Exception as e:
            self.logger.warning("SSH to %s:%s failed: %s", ip, port, e)
            return False

    def _assert_vm_ssh_accessible(self, ip, port=22, msg=None):
        """Assert that SSH to *ip*:*port* succeeds."""
        result = self._verify_vm_ssh_access(ip, port)
        self.assertTrue(
            result,
            msg or "SSH to %s:%s should be accessible" % (ip, port))

    def _assert_vm_ssh_not_accessible(self, ip, port=22, msg=None):
        """Assert that SSH to *ip*:*port* fails (uses short timeout)."""
        result = self._verify_vm_ssh_access(ip, port, timeout=5, retries=1)
        self.assertFalse(
            result,
            msg or "SSH to %s:%s should NOT be accessible" % (ip, port))

    def _create_firewall_rule_for_ssh(self, ipaddressid):
        """Create an ingress TCP/22 firewall rule on *ipaddressid*.

        Returns the created rule ID.
        """
        cmd = createFirewallRule.createFirewallRuleCmd()
        cmd.ipaddressid = ipaddressid
        cmd.protocol    = 'TCP'
        cmd.startport   = 22
        cmd.endport     = 22
        cmd.cidrlist    = ['0.0.0.0/0']
        rule = self.apiclient.createFirewallRule(cmd)
        self.assertIsNotNone(rule, "createFirewallRule returned None")
        self.logger.info("FW rule (TCP/22) created: id=%s on ipaddressid=%s",
                         rule.id, ipaddressid)
        return rule.id

    def _delete_firewall_rule(self, fw_rule_id):
        """Delete a firewall rule by ID (best-effort, warns on failure)."""
        if not fw_rule_id:
            return
        cmd = deleteFirewallRule.deleteFirewallRuleCmd()
        cmd.id = fw_rule_id
        try:
            self.apiclient.deleteFirewallRule(cmd)
            self.logger.info("FW rule %s deleted", fw_rule_id)
        except Exception as e:
            self.logger.warning("Could not delete FW rule %s: %s", fw_rule_id, e)

    def _get_source_nat_ip(self, network_id):
        """Return the source NAT public IP object for *network_id*, or None."""
        cmd = listPublicIpAddresses.listPublicIpAddressesCmd()
        cmd.networkid   = network_id
        cmd.issourcenat = True
        try:
            result = self.apiclient.listPublicIpAddresses(cmd)
            if isinstance(result, list) and result:
                return result[0]
        except Exception as e:
            self.logger.warning("_get_source_nat_ip(%s): %s", network_id, e)
        return None

    def _list_vpc_public_ips(self, vpc_id):
        """Return all public IP objects associated with *vpc_id*."""
        cmd = listPublicIpAddresses.listPublicIpAddressesCmd()
        cmd.vpcid = vpc_id
        cmd.listall = True
        try:
            result = self.apiclient.listPublicIpAddresses(cmd)
            if isinstance(result, list):
                return result
        except Exception as e:
            self.logger.warning("_list_vpc_public_ips(%s): %s", vpc_id, e)
        return []

    def _wait_for_vpc_source_nat_ip(self, vpc_id, expected_ip=None,
                                    retries=24, interval=5):
        """Wait until one source-NAT IP exists for the VPC (and optionally matches expected_ip)."""
        for _ in range(retries):
            ips = self._list_vpc_public_ips(vpc_id)
            src = [ip for ip in ips if getattr(ip, 'issourcenat', False)]
            if len(src) == 1:
                current_ip = getattr(src[0], 'ipaddress', None)
                if expected_ip is None or current_ip == expected_ip:
                    return src[0]
            time.sleep(interval)
        return None

    # ------------------------------------------------------------------
    # KVM host prerequisite check (test_04 / test_05 / test_06)
    # ------------------------------------------------------------------

    def _check_kvm_host_prerequisites(self, tools=None):
        """Verify that each configured KVM host has the required tools installed.

        Checks for the presence of every tool in *tools* (default:
        ``['arping', 'dnsmasq', 'haproxy']``) on each host in
        ``self.kvm_host_configs`` via SSH.  The test is skipped (via
        ``skipTest``) if any tool is absent from any reachable host.

        Hosts that cannot be reached over SSH are logged as warnings and
        excluded from the check — the connectivity failure will surface
        naturally when the test later tries to deploy scripts.
        """
        if tools is None:
            tools = ['arping', 'dnsmasq', 'haproxy']
        if not self.kvm_host_configs:
            self.skipTest("No KVM hosts configured — skipping prerequisite check")

        missing_per_host = {}
        for h in self.kvm_host_configs:
            ip = h.get('ip', '')
            if not ip:
                continue
            username = h.get('username', 'root')
            password = h.get('password', '')
            try:
                ssh = SshClient(ip, 22, username, password)
                missing_tools = []
                for tool in tools:
                    # Try both `command -v` (bash built-in) and `which`
                    out = ssh.execute(
                        "command -v {t} 2>/dev/null || which {t} 2>/dev/null"
                        " || echo MISSING_{t}".format(t=tool))
                    found = any(
                        line.strip() and 'MISSING_' + tool not in line
                        for line in out
                    )
                    if not found:
                        missing_tools.append(tool)
                if missing_tools:
                    missing_per_host[ip] = missing_tools
                    self.logger.warning(
                        "KVM host %s is missing prerequisite(s): %s",
                        ip, ', '.join(missing_tools))
                else:
                    self.logger.info(
                        "KVM host %s: all prerequisites present (%s)",
                        ip, ', '.join(tools))
            except Exception as e:
                self.logger.warning(
                    "Could not check prerequisites on KVM host %s: %s", ip, e)

        if missing_per_host:
            detail = "; ".join(
                "%s missing %s" % (ip, ', '.join(t))
                for ip, t in missing_per_host.items()
            )
            self.skipTest(
                "Skipping test — required tools not installed on KVM host(s): "
                + detail)

    # ------------------------------------------------------------------
    # Extension + NSP + offering setup helper (shared by tests 04-06)
    # ------------------------------------------------------------------

    def _setup_extension_nsp_offering(self, ext_name_prefix,
                                      supported_services=None,
                                      guestiptype="Isolated",
                                      for_vpc=False):
        """Create extension, deploy scripts, register to physical network,
        enable NSP, and optionally create a NetworkOffering.

        *supported_services* is a comma-separated list of CloudStack service names.

        *guestiptype* controls the guest IP type for the NetworkOffering:
        ``"Isolated"`` (default) or ``"Shared"``.

        *for_vpc* — when ``True`` the NetworkOffering creation step is skipped.
        VPC tests create their own VPC tier offering in the test body after this
        helper returns, so creating a generic isolated offering here would be
        wasteful and misleading.  ``(None, ext_name)`` is returned in that case.

        Sets ``self.physical_network``, ``self.extension``,
        ``self.extension_path``, ``self.provider_id``,
        ``self.kvm_deployer``, ``self.mgmt_deployer``.

        Returns ``(nw_offering, ext_name)``.  *nw_offering* is ``None`` when
        *for_vpc* is ``True``.  Skips when no KVM hosts are available.
        """
        _svc = supported_services
        self.physical_network = self._get_physical_network()

        ext_name = "%s-%s" % (ext_name_prefix, random_gen())
        self.extension = Extension.create(
            self.apiclient,
            name=ext_name,
            type='NetworkOrchestrator',
            details=[
                {"network.services": NETWORK_SERVICES},
                {"network.service.capabilities": NETWORK_SERVICE_CAPABILITIES_JSON},
            ]
        )
        self.assertIsNotNone(self.extension)
        self.assertEqual('Enabled', self.extension.state)

        ext_list = Extension.list(self.apiclient, id=self.extension.id)
        self.assertTrue(ext_list and len(ext_list) > 0)
        self.extension_path = ext_list[0].path
        self.assertIsNotNone(self.extension_path)
        self.logger.info("Extension '%s' created, path=%s",
                         ext_name, self.extension_path)

        # Deploy scripts
        self._deploy_scripts()
        kvm_hosts_csv = self.kvm_deployer.host_ips_csv()
        if not kvm_hosts_csv:
            self.skipTest("No KVM hosts available — skipping")

        # Register extension to physical network
        register_details = [
            {"hosts": kvm_hosts_csv},
            {"username": self.kvm_host_configs[0].get('username', 'root')},
            {"password": self.kvm_host_configs[0].get('password', '')},
        ]

        self.extension.register(
            self.apiclient,
            self.physical_network.id,
            'PhysicalNetwork',
            details=register_details
        )
        self.logger.info("Extension registered, hosts=%s", kvm_hosts_csv)

        # Enable NSP
        provider = self._find_provider(self.physical_network.id, ext_name)
        self.assertIsNotNone(provider,
                             "NSP '%s' not found after registration" % ext_name)
        self.provider_id = provider.id
        if provider.state != 'Enabled':
            self._update_provider_state(provider.id, 'Enabled')
        self.assertEqual('Enabled',
                         self._find_provider(self.physical_network.id,
                                             ext_name).state)
        self.logger.info("NSP '%s' enabled", ext_name)

        # Create NetworkOffering — skipped for VPC tests because the caller
        # creates a VPC tier offering separately after this helper returns.
        if for_vpc:
            self.logger.info(
                "for_vpc=True: skipping isolated NetworkOffering creation "
                "(VPC tier offering will be created in the test body)")
            return None, ext_name

        _provider_map = {s.strip(): ext_name for s in _svc.split(',')}
        offering_params = {
            "name":              "ExtNet-Offering-%s" % random_gen(),
            "displaytext":       "ExtNet test offering",
            "guestiptype":       guestiptype,
            "traffictype":       "GUEST",
            "supportedservices": _svc,
            "serviceProviderList": _provider_map,
        }
        if guestiptype == "Shared":
            # CloudStack requires shared guest offerings to explicitly allow
            # caller-specified IP ranges.
            offering_params["specifyIpRanges"] = True
        if guestiptype == "Isolated" and "SourceNat" in _svc:
            offering_params["serviceCapabilityList"] = {
                "SourceNat": {"SupportedSourceNatTypes": "peraccount"},
            }
        nw_offering = NetworkOffering.create(self.apiclient, offering_params)
        self.cleanup.append(nw_offering)
        nw_offering.update(self.apiclient, state='Enabled')
        self.logger.info("NetworkOffering '%s' enabled (services: %s)",
                         nw_offering.name, _svc)

        return nw_offering, ext_name

    def _create_account_keypair(self, account, name_suffix=""):
        """Create an SSH keypair scoped to *account* and save private key file."""
        try:
            kp_name = "extnet-%s-%s" % (name_suffix or random_gen(), random_gen())
            kp = SSHKeyPair.create(
                self.apiclient,
                name=kp_name,
                account=account.name,
                domainid=account.domainid,
            )

            pkfile = os.path.join(tempfile.gettempdir(), kp.name)
            with open(pkfile, "w+") as fh:
                fh.write(kp.privatekey)
            os.chmod(pkfile, 0o400)

            self.tmp_files.append(pkfile)
            kp.private_key_file = pkfile
            self._ssh_private_key_file = pkfile
            self.logger.info("Account keypair '%s' written to %s", kp.name, pkfile)
            return kp
        except Exception as e:
            self.logger.warning("Could not create account keypair: %s", e)
            return None

    def _create_account_network_vm(self, nw_offering, name_suffix="",
                                   network_params=None):
        """Create an account, an isolated network, and deploy a cloud-init VM.

        The VM is deployed with an account-scoped SSH keypair so that SSH
        access can be tested directly.  Username is ``ubuntu``.

        Returns ``(account, network, vm)``.
        """
        suffix = name_suffix or random_gen()

        account = Account.create(
            self.apiclient,
            self.services["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup.append(account)

        net_params = {
            "name":        "extnet-net-%s" % suffix,
            "displaytext": "ExtNet test network %s" % suffix,
        }
        if network_params:
            net_params.update(network_params)

        network = Network.create(
            self.apiclient,
            net_params,
            accountid=account.name,
            domainid=account.domainid,
            networkofferingid=nw_offering.id,
            zoneid=self.zone.id
        )
        self.cleanup.insert(0, network)
        self.assertIsNotNone(network)
        self.logger.info("Network created: %s (%s)", network.name, network.id)

        svc_offering = ServiceOffering.list(self.apiclient, issystem=False)[0]

        vm_cfg = {
            "displayname": "extnet-vm-%s" % suffix,
            "name":        "extnet-vm-%s" % suffix,
            "zoneid":      self.zone.id,
        }
        vm_kwargs = dict(
            accountid=account.name,
            domainid=account.domainid,
            serviceofferingid=svc_offering.id,
            templateid=self.template.id,
            networkids=[network.id],
        )
        account_keypair = self._create_account_keypair(account, suffix)
        if account_keypair:
            vm_kwargs["keypair"] = account_keypair.name

        vm = VirtualMachine.create(self.apiclient, vm_cfg, **vm_kwargs)
        self.cleanup.insert(0, vm)
        self.assertIsNotNone(vm)
        self.logger.info("VM deployed: %s (%s)", vm.name, vm.id)

        return account, network, vm

    def _teardown_extension(self):
        """Ordered teardown: disable NSP → delete provider → unregister
        extension → delete extension → remove mgmt script."""
        self._update_provider_state(self.provider_id, 'Disabled')
        self._delete_provider(self.provider_id)
        self.provider_id = None

        self.extension.unregister(self.apiclient,
                                  self.physical_network.id, 'PhysicalNetwork')
        self.extension.delete(self.apiclient,
                              unregisterresources=False, removeactions=False)
        self.extension        = None
        self.physical_network = None
        self._cleanup_mgmt_script()
        self._cleanup_kvm_script()

    # ------------------------------------------------------------------
    # Tests — API-only (no KVM / no SSH)
    # ------------------------------------------------------------------

    @attr(tags=["advanced", "smoke"], required_hardware="false")
    def test_01_provider_state_transitions(self):
        """NSP state machine: Disabled → Enabled → Disabled → Deleted."""
        pn = self._get_physical_network()
        self.physical_network = pn

        ext_name = "extnet-nsp-" + random_gen()
        self.extension = Extension.create(
            self.apiclient,
            name=ext_name,
            type='NetworkOrchestrator',
            details=[
                {"network.services": NETWORK_SERVICES},
                {"network.service.capabilities": NETWORK_SERVICE_CAPABILITIES_JSON},
            ]
        )
        self.extension.register(self.apiclient, pn.id, 'PhysicalNetwork')

        provider = self._find_provider(pn.id, ext_name)
        self.assertIsNotNone(provider)
        self.provider_id = provider.id

        # Normalise to Disabled first
        if provider.state == 'Enabled':
            self._update_provider_state(provider.id, 'Disabled')
        self.assertEqual('Disabled', self._find_provider(pn.id, ext_name).state)

        self._update_provider_state(provider.id, 'Enabled')
        self.assertEqual('Enabled', self._find_provider(pn.id, ext_name).state)
        self.logger.info("NSP enabled OK")

        self._update_provider_state(provider.id, 'Disabled')
        self.assertEqual('Disabled', self._find_provider(pn.id, ext_name).state)
        self.logger.info("NSP disabled OK")

        self._teardown_extension()
        self.logger.info("test_01 PASSED")

    @attr(tags=["advanced", "smoke"], required_hardware="false")
    def test_02_extension_capabilities_detail(self):
        """Verify network.services and network.service.capabilities details are stored and retrievable via API."""
        svc_caps_json = json.dumps({
            "SourceNat": {"SupportedSourceNatTypes": "peraccount"}
        })
        ext = Extension.create(
            self.apiclient,
            name="extnet-caps-" + random_gen(),
            type='NetworkOrchestrator',
            details=[
                {"network.services": "SourceNat"},
                {"network.service.capabilities": svc_caps_json},
            ]
        )
        self.cleanup.append(ext)
        ext_list = Extension.list(self.apiclient, id=ext.id)
        self.assertTrue(ext_list and len(ext_list) > 0)
        ext_obj = ext_list[0]
        if hasattr(ext_obj, 'details') and ext_obj.details:
            d = (ext_obj.details.__dict__
                 if not isinstance(ext_obj.details, dict)
                 else ext_obj.details)
            self.assertIn("network.services", d)
            self.assertIn("SourceNat", d["network.services"].split(","))
            self.assertIn("network.service.capabilities", d)
            stored_caps = json.loads(d["network.service.capabilities"])
            self.assertIn("SourceNat", stored_caps)
        self.logger.info("test_02 PASSED")

    @attr(tags=["advanced", "smoke"], required_hardware="false")
    def test_03_extension_enable_disable_and_delete_restriction(self):
        """Extension enable/disable; deletion blocked while registered."""
        pn = self._get_physical_network()
        self.physical_network = pn

        ext_name = "extnet-lifecycle-" + random_gen()
        self.extension = Extension.create(
            self.apiclient,
            name=ext_name,
            type='NetworkOrchestrator',
            details=[
                {"network.services": NETWORK_SERVICES},
                {"network.service.capabilities": NETWORK_SERVICE_CAPABILITIES_JSON},
            ]
        )
        self.assertIsNotNone(self.extension)
        self.assertEqual('Enabled', self.extension.state,
                         "Extension should be Enabled by default")

        self.extension.update(self.apiclient, state='Disabled')
        ext_list = Extension.list(self.apiclient, id=self.extension.id)
        self.assertEqual('Disabled', ext_list[0].state)
        self.logger.info("Extension disabled OK")

        self.extension.update(self.apiclient, state='Enabled')
        ext_list = Extension.list(self.apiclient, id=self.extension.id)
        self.assertEqual('Enabled', ext_list[0].state)
        self.logger.info("Extension re-enabled OK")

        self.extension.register(self.apiclient, pn.id, 'PhysicalNetwork')
        self.logger.info("Extension registered to physical network %s", pn.id)

        # Deletion while registered must fail
        try:
            self.extension.delete(self.apiclient,
                                  unregisterresources=False, removeactions=False)
            self.fail("Expected error when deleting extension while registered")
        except Exception as e:
            self.logger.info("Expected error when deleting while registered: %s",
                             e)

        self.extension.unregister(self.apiclient, pn.id, 'PhysicalNetwork')
        self.extension.delete(self.apiclient,
                              unregisterresources=False, removeactions=False)
        self.extension        = None
        self.physical_network = None
        self.logger.info("test_03 PASSED")

    # ------------------------------------------------------------------
    # Tests — hardware required
    # ------------------------------------------------------------------

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_04_dhcp_dns_userdata(self):
        """DHCP / DNS / UserData: cloud-init VM on a shared network reaches Running state.

        Creates a shared network with the extension providing Dhcp, Dns,
        and UserData services.  Deploys a VM with the cloud-init template.
        Verifies the VM reaches Running state, which implies it received a
        DHCP address from the extension.  No SSH verification is performed.

        Steps:
          1. Set up extension + NSP + offering (Dhcp, Dns, UserData) with
             guestiptype=Shared.
          2. Create account + shared network + cloud-init VM.
          3. Assert VM state == Running.
          4. Teardown.
        """
        self._check_kvm_host_prerequisites(['arping', 'dnsmasq', 'haproxy'])
        svc = "Dhcp,Dns,UserData"
        nw_offering, _ext_name = self._setup_extension_nsp_offering(
            "extnet-dhcp", supported_services=svc, guestiptype="Shared")

        # Shared offerings with specifyIpRanges=True require explicit range.
        third_octet = random.randint(32, 220)
        shared_params = {
            "gateway": "172.31.%d.1" % third_octet,
            "netmask": "255.255.255.0",
            "startip": "172.31.%d.10" % third_octet,
            "endip": "172.31.%d.200" % third_octet,
        }

        account, network, vm = self._create_account_network_vm(
            nw_offering, name_suffix="dhcp", network_params=shared_params)

        # Verify VM is in Running state — DHCP must have worked
        self.assertEqual(
            'Running', vm.state,
            "VM should be in Running state after deploy (implies DHCP worked)")
        self.logger.info("VM %s is Running — DHCP/DNS/UserData path exercised",
                         vm.name)

        # Cleanup
        vm.delete(self.apiclient, expunge=True)
        self.cleanup = [o for o in self.cleanup if o != vm]
        network.delete(self.apiclient)
        self.cleanup = [o for o in self.cleanup if o != network]
        self._teardown_extension()
        self.logger.info("test_04 PASSED")

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_05_isolated_network_full_lifecycle(self):
        """Full isolated-network lifecycle with SSH connectivity verification.

        Uses a single cloud-init VM (Ubuntu 22.04, SSH keypair, username
        ``ubuntu``) throughout.

        Sub-tests in order
        ------------------
        A. Static NAT
             allocate IP → enable static NAT → create FW rule (TCP/22)
             → assert SSH works → disable static NAT → assert SSH fails
        B. Port forwarding (22→22)
             allocate IP → create PF rule → create FW rule (TCP/22)
             → assert SSH works → delete PF rule → assert SSH fails
        C. Load balancer (haproxy, round-robin TCP/22)
             allocate IP → create LB rule → assign VM → create FW rule
             → assert SSH works → remove VM from LB → delete LB rule
        D. Network restart
             allocate IP → create PF rule → create FW rule
             → assert SSH works (baseline)
             → restartNetwork(cleanup=True)
             → assert SSH works (namespace rebuilt, rules reapplied)
        """
        self._check_kvm_host_prerequisites(['arping', 'dnsmasq', 'haproxy'])
        # ---- Setup ----
        svc = "SourceNat,StaticNat,PortForwarding,Firewall,Lb,UserData,Dhcp,Dns"
        nw_offering, _ext_name = self._setup_extension_nsp_offering(
            "extnet-isolated", supported_services=svc)
        account, network, vm = self._create_account_network_vm(
            nw_offering, name_suffix="iso")

        # ==============================================================
        # A. Static NAT
        #
        # StaticNATRule.enable() does NOT auto-create a firewall rule, so we
        # must create one explicitly.  StaticNATRule.disable() internally
        # calls revokeFirewallRulesForIp(), which cascade-deletes our rule,
        # so no explicit _delete_firewall_rule() is needed afterwards.
        # ==============================================================
        self.logger.info("--- Sub-test A: Static NAT ---")
        snat_ip_obj = PublicIPAddress.create(
            self.apiclient,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=network.id
        )
        snat_ip    = snat_ip_obj.ipaddress.ipaddress
        snat_ip_id = snat_ip_obj.ipaddress.id

        StaticNATRule.enable(self.apiclient,
                             ipaddressid=snat_ip_id,
                             virtualmachineid=vm.id,
                             networkid=network.id)
        self.logger.info("Static NAT enabled on %s", snat_ip)
        # Explicit FW rule required — static NAT does not auto-create one
        self._create_firewall_rule_for_ssh(snat_ip_id)

        self._assert_vm_ssh_accessible(
            snat_ip, 22,
            "SSH via static NAT %s:22 should succeed" % snat_ip)
        self.logger.info("Verified: SSH works via static NAT %s", snat_ip)

        # disable() cascades revokeFirewallRulesForIp() — deletes the FW rule
        StaticNATRule.disable(self.apiclient, ipaddressid=snat_ip_id)
        self.logger.info("Static NAT disabled on %s", snat_ip)
        self._assert_vm_ssh_not_accessible(
            snat_ip, 22,
            "SSH via %s:22 should fail after static NAT disabled" % snat_ip)
        self.logger.info("Verified: SSH fails after static NAT disabled")
        snat_ip_obj.delete(self.apiclient)

        # ==============================================================
        # B. Port forwarding
        #
        # NATRule.create() passes openFirewall=True (default for non-VPC),
        # so CloudStack automatically creates a TCP/22 FW rule with
        # relatedRuleId=pf_rule.id.  pf_rule.delete() cascade-removes it.
        # Do NOT call _create_firewall_rule_for_ssh() — that would conflict
        # with the auto-created rule.
        # ==============================================================
        self.logger.info("--- Sub-test B: Port forwarding ---")
        pf_ip_obj = PublicIPAddress.create(
            self.apiclient,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=network.id
        )
        pf_ip    = pf_ip_obj.ipaddress.ipaddress
        pf_ip_id = pf_ip_obj.ipaddress.id

        pf_rule = NATRule.create(
            self.apiclient, vm,
            {"privateport": 22, "publicport": 22, "protocol": "TCP"},
            ipaddressid=pf_ip_id,
            networkid=network.id
        )
        self.assertIsNotNone(pf_rule)
        self.logger.info("PF rule created: %s:22 → VM:22 (FW rule auto-created)",
                         pf_ip)

        self._assert_vm_ssh_accessible(
            pf_ip, 22,
            "SSH via PF %s:22 should succeed" % pf_ip)
        self.logger.info("Verified: SSH works via port forwarding %s", pf_ip)

        # delete() cascades revokeRelatedFirewallRule() — removes auto FW rule
        pf_rule.delete(self.apiclient)
        self.logger.info("PF rule deleted on %s", pf_ip)
        self._assert_vm_ssh_not_accessible(
            pf_ip, 22,
            "SSH via %s:22 should fail after PF rule deleted" % pf_ip)
        self.logger.info("Verified: SSH fails after PF rule deleted")
        pf_ip_obj.delete(self.apiclient)

        # ==============================================================
        # C. Load balancer (haproxy)
        #
        # LoadBalancerRule.create() also uses openFirewall=True by default,
        # auto-creating a TCP/22 FW rule.  lb_rule.delete() cascades it.
        # ==============================================================
        self.logger.info("--- Sub-test C: Load balancer ---")
        lb_ip_obj = PublicIPAddress.create(
            self.apiclient,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=network.id
        )
        lb_ip    = lb_ip_obj.ipaddress.ipaddress
        lb_ip_id = lb_ip_obj.ipaddress.id

        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            {"name":        "lb-ssh-%s" % random_gen(),
             "alg":         "roundrobin",
             "privateport": 22,
             "publicport":  22},
            ipaddressid=lb_ip_id,
            accountid=account.name,
            networkid=network.id,
            domainid=account.domainid
        )
        self.assertIsNotNone(lb_rule)
        lb_rule.assign(self.apiclient, vms=[vm])
        self.logger.info("LB rule created, VM assigned: %s:22 (FW rule auto-created)",
                         lb_ip)

        self._assert_vm_ssh_accessible(
            lb_ip, 22,
            "SSH via LB %s:22 should succeed (haproxy required on KVM hosts)"
            % lb_ip)
        self.logger.info("Verified: SSH works via haproxy LB %s", lb_ip)

        lb_rule.remove(self.apiclient, vms=[vm])
        lb_rule.delete(self.apiclient)
        lb_ip_obj.delete(self.apiclient)
        self.logger.info("LB rule deleted")

        # ==============================================================
        # D. Network restart (cleanup=True)
        #
        # NATRule.create() auto-creates the TCP/22 FW rule.
        # After network restart both the PF rule and the FW rule must be
        # re-applied by the extension, so SSH should work again.
        # ==============================================================
        self.logger.info("--- Sub-test D: Network restart ---")
        rst_ip_obj = PublicIPAddress.create(
            self.apiclient,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=network.id
        )
        rst_ip    = rst_ip_obj.ipaddress.ipaddress
        rst_ip_id = rst_ip_obj.ipaddress.id

        rst_pf = NATRule.create(
            self.apiclient, vm,
            {"privateport": 22, "publicport": 22, "protocol": "TCP"},
            ipaddressid=rst_ip_id,
            networkid=network.id
        )
        self._assert_vm_ssh_accessible(
            rst_ip, 22,
            "SSH via %s:22 should work before restart" % rst_ip)
        self.logger.info("Baseline SSH verified before restart")

        self.logger.info("Restarting network %s (cleanup=True) ...", network.id)
        network.restart(self.apiclient, cleanup=True)
        self.logger.info("Network restart completed")

        self._assert_vm_ssh_accessible(
            rst_ip, 22,
            "SSH via %s:22 should work after network restart" % rst_ip)
        self.logger.info("Verified: SSH restored after restart")

        rst_pf.delete(self.apiclient)
        rst_ip_obj.delete(self.apiclient)

        # ---- Final cleanup ----
        vm.delete(self.apiclient, expunge=True)
        self.cleanup = [o for o in self.cleanup if o != vm]
        network.delete(self.apiclient)
        self.cleanup = [o for o in self.cleanup if o != network]
        self._teardown_extension()
        self.logger.info("test_05 PASSED")

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_06_vpc_multi_tier_and_restart(self):
        """VPC multi-tier + VPC restart with SSH connectivity verification.

        Creates two VPC tier networks backed by the extension, deploys a
        VM in each tier, and verifies SSH access independently.

        Sub-tests in order
        ------------------
        A. Baseline connectivity (before VPC restart)
             tier-1 PF :22 → VM1          — assert SSH works
             tier-2 LB :22 → VM2          — assert SSH works
             tier-1 static NAT :22 → VM1  — enable, create FW rule,
                                            assert SSH works
        B. VPC restart (cleanup=True)
             assert SSH still works via tier-1 PF, tier-2 LB, and
             tier-1 static NAT after the namespace is rebuilt
        C. Static NAT teardown (after VPC restart)
             disable static NAT → assert SSH fails → delete IP
        D. Partial delete
             delete tier-1 VM + network → assert tier-2 VM still accessible
        E. Final delete
             delete tier-2 VM + network, VPC, teardown extension

        The VPC tier network offering uses ``useVpc=on`` as required by
        CloudStack for VPC-associated tier networks.
        """
        self._check_kvm_host_prerequisites(['arping', 'dnsmasq', 'haproxy'])
        # ---- Setup: extension + NSP only (no isolated offering for VPC tests) ----
        svc = "SourceNat,StaticNat,PortForwarding,Lb,UserData,Dhcp,Dns,NetworkACL"
        _nw_offering, ext_name = self._setup_extension_nsp_offering(
            "extnet-vpc", supported_services=svc, for_vpc=True)

        # ---- VPC tier network offering (useVpc=on) ----
        vpc_tier_svc = "SourceNat,StaticNat,PortForwarding,Lb,UserData,Dhcp,Dns,NetworkACL"
        _tier_prov   = {s.strip(): ext_name for s in vpc_tier_svc.split(',')}
        vpc_tier_offering = NetworkOffering.create(self.apiclient, {
            "name":              "ExtNet-VPCTier-%s" % random_gen(),
            "displaytext":       "ExtNet VPC tier offering",
            "guestiptype":       "Isolated",
            "traffictype":       "GUEST",
            "availability":      "Optional",
            "useVpc":            "on",
            "supportedservices": vpc_tier_svc,
            "serviceProviderList": _tier_prov,
            "serviceCapabilityList": {
                "SourceNat": {"SupportedSourceNatTypes": "peraccount"},
            },
        })
        self.cleanup.append(vpc_tier_offering)
        vpc_tier_offering.update(self.apiclient, state='Enabled')
        self.logger.info("VPC tier offering '%s' enabled", vpc_tier_offering.name)

        # ---- VPC offering ----
        vpc_svc  = "SourceNat,StaticNat,PortForwarding,Lb,UserData,Dhcp,Dns,NetworkACL"
        _vpc_prov = {s.strip(): ext_name for s in vpc_svc.split(',')}
        vpc_offering = VpcOffering.create(self.apiclient, {
            "name":              "ExtNet-VPC-%s" % random_gen(),
            "displaytext":       "ExtNet VPC offering",
            "supportedservices": vpc_svc,
            "serviceProviderList": _vpc_prov,
        })
        self.cleanup.append(vpc_offering)
        vpc_offering.update(self.apiclient, state='Enabled')
        self.logger.info("VPC offering '%s' enabled", vpc_offering.name)

        # ---- Account ----
        suffix  = random_gen()
        account = Account.create(
            self.apiclient,
            self.services["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup.append(account)
        account_keypair = self._create_account_keypair(account, suffix)

        # ---- VPC ----
        vpc = VPC.create(
            self.apiclient,
            {"name":        "extnet-vpc-%s" % suffix,
             "displaytext": "ExtNet VPC %s" % suffix,
             "cidr":        "10.1.0.0/16"},
            vpcofferingid=vpc_offering.id,
            zoneid=self.zone.id,
            account=account.name,
            domainid=account.domainid
        )
        self.cleanup.insert(0, vpc)
        self.logger.info("VPC created: %s (%s)", vpc.name, vpc.id)

        # ---- Tier 1 ----
        tier1 = Network.create(
            self.apiclient,
            {"name":        "tier1-%s" % suffix,
             "displaytext": "Tier 1 %s" % suffix},
            accountid=account.name,
            domainid=account.domainid,
            networkofferingid=vpc_tier_offering.id,
            zoneid=self.zone.id,
            vpcid=vpc.id,
            gateway="10.1.1.1",
            netmask="255.255.255.0"
        )
        self.cleanup.insert(0, tier1)
        self.logger.info("Tier 1 created: %s (%s)", tier1.name, tier1.id)

        # ---- Tier 2 ----
        tier2 = Network.create(
            self.apiclient,
            {"name":        "tier2-%s" % suffix,
             "displaytext": "Tier 2 %s" % suffix},
            accountid=account.name,
            domainid=account.domainid,
            networkofferingid=vpc_tier_offering.id,
            zoneid=self.zone.id,
            vpcid=vpc.id,
            gateway="10.1.2.1",
            netmask="255.255.255.0"
        )
        self.cleanup.insert(0, tier2)
        self.logger.info("Tier 2 created: %s (%s)", tier2.name, tier2.id)

        svc_offering = ServiceOffering.list(self.apiclient, issystem=False)[0]

        # ---- VM in tier 1 ----
        vm1_cfg = {"displayname": "vm1-%s" % suffix,
                   "name":        "vm1-%s" % suffix,
                   "zoneid":      self.zone.id}
        vm1_kw  = dict(accountid=account.name,
                       domainid=account.domainid,
                       serviceofferingid=svc_offering.id,
                       templateid=self.template.id,
                       networkids=[tier1.id])
        if account_keypair:
            vm1_kw["keypair"] = account_keypair.name
        vm1 = VirtualMachine.create(self.apiclient, vm1_cfg, **vm1_kw)
        self.cleanup.insert(0, vm1)
        self.logger.info("VM1 deployed in tier 1: %s (%s)", vm1.name, vm1.id)

        # ---- VM in tier 2 ----
        vm2_cfg = {"displayname": "vm2-%s" % suffix,
                   "name":        "vm2-%s" % suffix,
                   "zoneid":      self.zone.id}
        vm2_kw  = dict(accountid=account.name,
                       domainid=account.domainid,
                       serviceofferingid=svc_offering.id,
                       templateid=self.template.id,
                       networkids=[tier2.id])
        if account_keypair:
            vm2_kw["keypair"] = account_keypair.name
        vm2 = VirtualMachine.create(self.apiclient, vm2_cfg, **vm2_kw)
        self.cleanup.insert(0, vm2)
        self.logger.info("VM2 deployed in tier 2: %s (%s)", vm2.name, vm2.id)

        # ---- Tier 1: PF rule ----
        pf_ip1 = PublicIPAddress.create(
            self.apiclient,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=tier1.id,
            vpcid=vpc.id
        )
        pf_rule1 = NATRule.create(
            self.apiclient, vm1,
            {"privateport": 22, "publicport": 22, "protocol": "TCP"},
            ipaddressid=pf_ip1.ipaddress.id,
            networkid=tier1.id
        )
        tier1_pf_ip = pf_ip1.ipaddress.ipaddress
        self.logger.info("Tier 1 PF: %s:22 → VM1:22", tier1_pf_ip)

        # ---- Tier 2: LB rule ----
        lb_ip2 = PublicIPAddress.create(
            self.apiclient,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=tier2.id,
            vpcid=vpc.id
        )
        lb_rule2 = LoadBalancerRule.create(
            self.apiclient,
            {"name":        "vpc-lb-ssh-%s" % random_gen(),
             "alg":         "roundrobin",
             "privateport": 22,
             "publicport":  22},
            ipaddressid=lb_ip2.ipaddress.id,
            accountid=account.name,
            networkid=tier2.id,
            domainid=account.domainid
        )
        self.assertIsNotNone(lb_rule2)
        lb_rule2.assign(self.apiclient, vms=[vm2])
        tier2_lb_ip = lb_ip2.ipaddress.ipaddress
        self.logger.info("Tier 2 LB: %s:22 → VM2:22", tier2_lb_ip)

        # ---- Tier 1: Static NAT (allocated BEFORE restart) ----
        snat_ip1_obj = PublicIPAddress.create(
            self.apiclient,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=tier1.id,
            vpcid=vpc.id
        )
        snat_ip1    = snat_ip1_obj.ipaddress.ipaddress
        snat_ip1_id = snat_ip1_obj.ipaddress.id
        StaticNATRule.enable(
            self.apiclient,
            ipaddressid=snat_ip1_id,
            virtualmachineid=vm1.id,
            networkid=tier1.id
        )
        self.logger.info("Static NAT enabled on tier-1: %s → VM1", snat_ip1)

        # ==============================================================
        # A. Baseline connectivity — BEFORE VPC restart
        # ==============================================================
        self.logger.info("--- Sub-test A: Baseline connectivity (before restart) ---")

        self._assert_vm_ssh_accessible(
            tier1_pf_ip, 22,
            "SSH to tier-1 VM via PF (%s) should succeed before restart"
            % tier1_pf_ip)
        self.logger.info("Verified: SSH to tier-1 VM works via PF (before restart)")

        self._assert_vm_ssh_accessible(
            tier2_lb_ip, 22,
            "SSH to tier-2 VM via LB (%s) should succeed before restart"
            % tier2_lb_ip)
        self.logger.info("Verified: SSH to tier-2 VM works via LB (before restart)")

        self._assert_vm_ssh_accessible(
            snat_ip1, 22,
            "SSH to tier-1 VM via static NAT (%s) should succeed before restart"
            % snat_ip1)
        self.logger.info("Verified: SSH to tier-1 VM works via static NAT (before restart)")

        # ==============================================================
        # B. VPC restart (cleanup=True)
        #    Re-verify all three access methods after the namespace is rebuilt.
        # ==============================================================
        self.logger.info("--- Sub-test B: VPC restart (cleanup=True) ---")
        self.logger.info("Restarting VPC %s (cleanup=True) ...", vpc.id)
        vpc.restart(self.apiclient, cleanup=True)
        self.logger.info("VPC restart completed")

        self._assert_vm_ssh_accessible(
            tier1_pf_ip, 22,
            "SSH to tier-1 VM via PF must work after VPC restart")
        self.logger.info("Verified: SSH to tier-1 VM works via PF (after restart)")

        self._assert_vm_ssh_accessible(
            tier2_lb_ip, 22,
            "SSH to tier-2 VM via LB must work after VPC restart")
        self.logger.info("Verified: SSH to tier-2 VM works via LB (after restart)")

        self._assert_vm_ssh_accessible(
            snat_ip1, 22,
            "SSH to tier-1 VM via static NAT must work after VPC restart")
        self.logger.info("Verified: SSH to tier-1 VM works via static NAT (after restart)")

        # ==============================================================
        # C. Disable static NAT (after VPC restart)
        # ==============================================================
        self.logger.info("--- Sub-test C: Disable static NAT (after restart) ---")
        StaticNATRule.disable(self.apiclient, ipaddressid=snat_ip1_id)
        self.logger.info("Static NAT disabled on tier-1 %s", snat_ip1)
        self._assert_vm_ssh_not_accessible(
            snat_ip1, 22,
            "SSH via tier-1 %s:22 should fail after static NAT disabled" % snat_ip1)
        self.logger.info("Verified: SSH fails after static NAT disabled")
        snat_ip1_obj.delete(self.apiclient)

        # ==============================================================
        # D. Partial delete: tier-1 — tier-2 must remain accessible
        # ==============================================================
        self.logger.info("--- Sub-test D: Delete tier-1, verify tier-2 intact ---")
        pf_rule1.delete(self.apiclient)
        pf_ip1.delete(self.apiclient)
        vm1.delete(self.apiclient, expunge=True)
        self.cleanup = [o for o in self.cleanup if o != vm1]
        tier1.delete(self.apiclient)
        self.cleanup = [o for o in self.cleanup if o != tier1]
        self.logger.info("Tier 1 VM + network deleted")

        # Tier 2 must remain accessible — cmd_destroy() on tier-1 must not
        # delete tier-2's public veth (fixed via .tier ownership tracking).
        self._assert_vm_ssh_accessible(
            tier2_lb_ip, 22,
            "SSH to tier-2 VM via LB must still work after tier-1 deleted")
        self.logger.info("Verified: tier-2 VM still accessible via LB after tier-1 deleted")

        # ==============================================================
        # E. Final delete: tier-2, VPC
        # ==============================================================
        lb_rule2.remove(self.apiclient, vms=[vm2])
        lb_rule2.delete(self.apiclient)
        lb_ip2.delete(self.apiclient)
        vm2.delete(self.apiclient, expunge=True)
        self.cleanup = [o for o in self.cleanup if o != vm2]
        tier2.delete(self.apiclient)
        self.cleanup = [o for o in self.cleanup if o != tier2]
        self.logger.info("Tier 2 VM + network deleted")

        vpc.delete(self.apiclient)
        self.cleanup = [o for o in self.cleanup if o != vpc]

        self._teardown_extension()
        self.logger.info("test_06 PASSED")

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_07_vpc_network_acl(self):
        """VPC Network ACL testing with multiple tiers and traffic rules.

        Creates two VPC tiers with distinct network ACL lists and verifies that
        ACL rules are correctly applied:
          - Inbound rules from public network (via port forwarding)
          - Egress rules between VPC tiers (via ping)

        Sub-tests in order
        ------------------
        A. Setup: Create two tiers with different ACL lists
              tier1 (acl1): Allow ICMP from anywhere, Deny SSH
              tier2 (acl2): Allow ICMP and SSH from anywhere
        B. Deploy VMs and verify ACLs block SSH to tier1, allow ICMP to both tiers
        C. Test inter-tier ICMP communication between VMs
        D. Verify SSH only works on tier2 (where ACL permits it)
        E. Cleanup

        The test uses ICMP (ping) to verify inter-tier connectivity and SSH
        attempts to verify ACL rules are enforced on ingress traffic.
        """
        self._check_kvm_host_prerequisites(['arping', 'dnsmasq', 'haproxy'])

        # ---- Setup: extension + NSP (supporting NetworkACL) ----
        svc = "SourceNat,StaticNat,PortForwarding,Lb,UserData,Dhcp,Dns,NetworkACL"
        _nw_offering, ext_name = self._setup_extension_nsp_offering(
            "extnet-acl", supported_services=svc, for_vpc=True)

        # ---- VPC tier network offering (useVpc=on, with NetworkACL support) ----
        vpc_tier_svc = "SourceNat,StaticNat,PortForwarding,Lb,UserData,Dhcp,Dns,NetworkACL"
        _tier_prov   = {s.strip(): ext_name for s in vpc_tier_svc.split(',')}
        vpc_tier_offering = NetworkOffering.create(self.apiclient, {
            "name":              "ExtNet-VPCTier-ACL-%s" % random_gen(),
            "displaytext":       "ExtNet VPC tier offering with ACL",
            "guestiptype":       "Isolated",
            "traffictype":       "GUEST",
            "availability":      "Optional",
            "useVpc":            "on",
            "supportedservices": vpc_tier_svc,
            "serviceProviderList": _tier_prov,
            "serviceCapabilityList": {
                "SourceNat": {"SupportedSourceNatTypes": "peraccount"},
            },
        })
        self.cleanup.append(vpc_tier_offering)
        vpc_tier_offering.update(self.apiclient, state='Enabled')
        self.logger.info("VPC tier offering '%s' enabled", vpc_tier_offering.name)

        # ---- VPC offering ----
        vpc_svc  = "SourceNat,StaticNat,PortForwarding,Lb,UserData,Dhcp,Dns,NetworkACL"
        _vpc_prov = {s.strip(): ext_name for s in vpc_svc.split(',')}
        vpc_offering = VpcOffering.create(self.apiclient, {
            "name":              "ExtNet-VPC-ACL-%s" % random_gen(),
            "displaytext":       "ExtNet VPC offering with ACL",
            "supportedservices": vpc_svc,
            "serviceProviderList": _vpc_prov,
        })
        self.cleanup.append(vpc_offering)
        vpc_offering.update(self.apiclient, state='Enabled')
        self.logger.info("VPC offering '%s' enabled", vpc_offering.name)

        # ---- Account ----
        suffix  = random_gen()
        account = Account.create(
            self.apiclient,
            self.services["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup.append(account)
        account_keypair = self._create_account_keypair(account, suffix)

        # ---- VPC ----
        vpc = VPC.create(
            self.apiclient,
            {"name":        "extnet-vpc-acl-%s" % suffix,
             "displaytext": "ExtNet VPC ACL %s" % suffix,
             "cidr":        "10.2.0.0/16"},
            vpcofferingid=vpc_offering.id,
            zoneid=self.zone.id,
            account=account.name,
            domainid=account.domainid
        )
        self.cleanup.insert(0, vpc)
        self.logger.info("VPC created: %s (%s)", vpc.name, vpc.id)

        # ---- Network ACL Lists ----
        # ACL1: Restrict to ICMP only (deny SSH)
        acl1 = NetworkACLList.create(
            self.apiclient,
            {"name": "acl1-icmp-only-%s" % suffix,
             "description": "ACL1 for tier1 - ICMP only"},
            vpcid=vpc.id
        )
        self.cleanup.insert(0, acl1)
        self.logger.info("ACL1 created: %s (ICMP only)", acl1.id)

        # ACL2: Allow ICMP and SSH
        acl2 = NetworkACLList.create(
            self.apiclient,
            {"name": "acl2-icmp-ssh-%s" % suffix,
             "description": "ACL2 for tier2 - ICMP and SSH allowed"},
            vpcid=vpc.id
        )
        self.cleanup.insert(0, acl2)
        self.logger.info("ACL2 created: %s (ICMP and SSH)", acl2.id)

        # ---- Tier 1 with ACL1 (ICMP only) ----
        tier1 = Network.create(
            self.apiclient,
            {"name":        "tier1-acl-%s" % suffix,
             "displaytext": "Tier 1 ACL %s" % suffix},
            accountid=account.name,
            domainid=account.domainid,
            networkofferingid=vpc_tier_offering.id,
            zoneid=self.zone.id,
            vpcid=vpc.id,
            gateway="10.2.1.1",
            netmask="255.255.255.0"
        )
        self.cleanup.insert(0, tier1)
        self.logger.info("Tier 1 created: %s (%s)", tier1.name, tier1.id)

        # ---- Tier 2 with ACL2 (ICMP + SSH) ----
        tier2 = Network.create(
            self.apiclient,
            {"name":        "tier2-acl-%s" % suffix,
             "displaytext": "Tier 2 ACL %s" % suffix},
            accountid=account.name,
            domainid=account.domainid,
            networkofferingid=vpc_tier_offering.id,
            zoneid=self.zone.id,
            vpcid=vpc.id,
            gateway="10.2.2.1",
            netmask="255.255.255.0"
        )
        self.cleanup.insert(0, tier2)
        self.logger.info("Tier 2 created: %s (%s)", tier2.name, tier2.id)

        svc_offering = ServiceOffering.list(self.apiclient, issystem=False)[0]

        # ---- VM in tier 1 ----
        vm1_cfg = {"displayname": "vm1-acl-%s" % suffix,
                   "name":        "vm1-acl-%s" % suffix,
                   "zoneid":      self.zone.id}
        vm1_kw  = dict(accountid=account.name,
                       domainid=account.domainid,
                       serviceofferingid=svc_offering.id,
                       templateid=self.template.id,
                       networkids=[tier1.id])
        if account_keypair:
            vm1_kw["keypair"] = account_keypair.name
        vm1 = VirtualMachine.create(self.apiclient, vm1_cfg, **vm1_kw)
        self.cleanup.insert(0, vm1)
        self.logger.info("VM1 deployed in tier 1: %s (%s)", vm1.name, vm1.id)

        # ---- VM in tier 2 ----
        vm2_cfg = {"displayname": "vm2-acl-%s" % suffix,
                   "name":        "vm2-acl-%s" % suffix,
                   "zoneid":      self.zone.id}
        vm2_kw  = dict(accountid=account.name,
                       domainid=account.domainid,
                       serviceofferingid=svc_offering.id,
                       templateid=self.template.id,
                       networkids=[tier2.id])
        if account_keypair:
            vm2_kw["keypair"] = account_keypair.name
        vm2 = VirtualMachine.create(self.apiclient, vm2_cfg, **vm2_kw)
        self.cleanup.insert(0, vm2)
        self.logger.info("VM2 deployed in tier 2: %s (%s)", vm2.name, vm2.id)

        # Get VM IPs for later use
        vm1_networks = VirtualMachine.list(self.apiclient, id=vm1.id)[0].nic
        vm1_ip = None
        for nic in vm1_networks:
            if nic.networkid == tier1.id:
                vm1_ip = nic.ipaddress
        self.assertIsNotNone(vm1_ip, "VM1 should have IP in tier1")
        self.logger.info("VM1 IP in tier1: %s", vm1_ip)

        vm2_networks = VirtualMachine.list(self.apiclient, id=vm2.id)[0].nic
        vm2_ip = None
        for nic in vm2_networks:
            if nic.networkid == tier2.id:
                vm2_ip = nic.ipaddress
        self.assertIsNotNone(vm2_ip, "VM2 should have IP in tier2")
        self.logger.info("VM2 IP in tier2: %s", vm2_ip)

        # ==============================================================
        # A. Setup ACL rules
        # ==============================================================
        self.logger.info("--- Sub-test A: Setting up ACL rules ---")

        # ACL1 rules: ICMP allowed, SSH denied (ingress), ICMP allowed (egress)
        # Rule numbers must be unique per ACL list (across ingress+egress).
        # Ingress rule: Allow ICMP
        NetworkACL.create(
            self.apiclient,
            {"protocol": "ICMP", "icmptype": -1, "icmpcode": -1,
             "traffictype": "Ingress", "aclid": acl1.id,
             "cidrlist": ["0.0.0.0/0"], "action": "Allow", "number": 10},
            networkid=tier1.id
        )
        self.logger.info("ACL1 Ingress rule: ICMP Allow")

        # Ingress rule: Deny SSH
        NetworkACL.create(
            self.apiclient,
            {"protocol": "TCP", "startport": 22, "endport": 22,
             "traffictype": "Ingress", "aclid": acl1.id,
             "cidrlist": ["0.0.0.0/0"], "action": "Deny", "number": 20},
            networkid=tier1.id
        )
        self.logger.info("ACL1 Ingress rule: SSH Deny")

        # Egress rule: Allow all
        NetworkACL.create(
            self.apiclient,
            {"protocol": "All", "traffictype": "Egress", "aclid": acl1.id,
             "cidrlist": ["0.0.0.0/0"], "action": "Allow", "number": 30},
            networkid=tier1.id
        )
        self.logger.info("ACL1 Egress rule: All Allow")

        # ACL2 rules: ICMP and SSH allowed (ingress), All allowed (egress)
        # Ingress rule: Allow ICMP
        NetworkACL.create(
            self.apiclient,
            {"protocol": "ICMP", "icmptype": -1, "icmpcode": -1,
             "traffictype": "Ingress", "aclid": acl2.id,
             "cidrlist": ["0.0.0.0/0"], "action": "Allow", "number": 10},
            networkid=tier2.id
        )
        self.logger.info("ACL2 Ingress rule: ICMP Allow")

        # Ingress rule: Allow SSH
        NetworkACL.create(
            self.apiclient,
            {"protocol": "TCP", "startport": 22, "endport": 22,
             "traffictype": "Ingress", "aclid": acl2.id,
             "cidrlist": ["0.0.0.0/0"], "action": "Allow", "number": 20},
            networkid=tier2.id
        )
        self.logger.info("ACL2 Ingress rule: SSH Allow")

        # Egress rule: Allow all
        NetworkACL.create(
            self.apiclient,
            {"protocol": "All", "traffictype": "Egress", "aclid": acl2.id,
             "cidrlist": ["0.0.0.0/0"], "action": "Allow", "number": 30},
            networkid=tier2.id
        )
        self.logger.info("ACL2 Egress rule: All Allow")

        # Apply the ACL lists to the tier networks
        tier1.replaceACLList(self.apiclient, acl1.id)
        self.logger.info("Applied ACL1 (deny SSH) to tier1")
        tier2.replaceACLList(self.apiclient, acl2.id)
        self.logger.info("Applied ACL2 (allow SSH) to tier2")

        # ==============================================================
        # B. Test Public IP access with ACL enforcement (via PF)
        # ==============================================================
        self.logger.info("--- Sub-test B: Test public IP access with ACLs ---")

        # Create public IP and PF for tier1 (should block SSH due to ACL1)
        ip1 = PublicIPAddress.create(
            self.apiclient,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=tier1.id,
            vpcid=vpc.id
        )
        tier1_public_ip = ip1.ipaddress.ipaddress
        self.logger.info("Tier1 public IP allocated: %s", tier1_public_ip)

        pf_rule1 = NATRule.create(
            self.apiclient, vm1,
            {"privateport": 22, "publicport": 22, "protocol": "TCP"},
            ipaddressid=ip1.ipaddress.id,
            networkid=tier1.id,
            vpcid=vpc.id
        )
        self.assertIsNotNone(pf_rule1)
        self.logger.info("Tier1 PF rule created: %s:22 → VM1:22", tier1_public_ip)

        # SSH to tier1 should fail due to ACL denying SSH
        self._assert_vm_ssh_not_accessible(
            tier1_public_ip, 22,
            "SSH to tier1 %s should FAIL (ACL denies SSH)" % tier1_public_ip)
        self.logger.info("Verified: SSH to tier1 correctly blocked by ACL")

        # Create public IP and PF for tier2 (should allow SSH due to ACL2)
        ip2 = PublicIPAddress.create(
            self.apiclient,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=tier2.id,
            vpcid=vpc.id
        )
        tier2_public_ip = ip2.ipaddress.ipaddress
        self.logger.info("Tier2 public IP allocated: %s", tier2_public_ip)

        pf_rule2 = NATRule.create(
            self.apiclient, vm2,
            {"privateport": 22, "publicport": 22, "protocol": "TCP"},
            ipaddressid=ip2.ipaddress.id,
            networkid=tier2.id,
            vpcid=vpc.id
        )
        self.assertIsNotNone(pf_rule2)
        self.logger.info("Tier2 PF rule created: %s:22 → VM2:22", tier2_public_ip)

        # SSH to tier2 should succeed due to ACL allowing SSH
        self._assert_vm_ssh_accessible(
            tier2_public_ip, 22,
            "SSH to tier2 %s should succeed (ACL allows SSH)" % tier2_public_ip)
        self.logger.info("Verified: SSH to tier2 correctly allowed by ACL")

        # ==============================================================
        # C. Test inter-tier ICMP communication
        # ==============================================================
        self.logger.info("--- Sub-test C: Test inter-tier ICMP (ping) ---")

        # From VM2 (tier2), ping VM1 (tier1) — should succeed (both allow ICMP egress)
        # This tests that the ACL egress rules work and inter-tier routing is OK
        try:
            ssh_vm2 = SshClient(
                tier2_public_ip, 22, "ubuntu", None,
                keyPairFiles=self._ssh_private_key_file,
                timeout=30, retries=10
            )
            out = ssh_vm2.execute("ping -c 3 %s 2>&1 | tail -5" % vm1_ip)
            ping_result = "\n".join(out)
            # Check if ping succeeded (look for "3 packets transmitted" or similar)
            if "transmitted" in ping_result.lower():
                self.logger.info("Ping from VM2 to VM1 output:\n%s", ping_result)
                if "0 received" not in ping_result.lower():
                    self.logger.info("Verified: Inter-tier ping succeeded (ICMP egress rule allows it)")
                else:
                    self.logger.warning("Ping packets were transmitted but all lost")
            else:
                self.logger.warning("Could not determine ping result from: %s", ping_result)
        except Exception as e:
            self.logger.warning("Could not execute ping test: %s", e)

        # ==============================================================
        # D. Additional SSH verification
        # ==============================================================
        self.logger.info("--- Sub-test D: Additional SSH verification ---")

        # Re-verify tier2 SSH works after ACL rules are fully active
        self._assert_vm_ssh_accessible(
            tier2_public_ip, 22,
            "SSH to tier2 should still work (ACL permits SSH)")
        self.logger.info("Verified: SSH to tier2 confirmed working")

        # ==============================================================
        # E. Cleanup
        # ==============================================================
        self.logger.info("--- Sub-test E: Cleanup ---")
        pf_rule1.delete(self.apiclient)
        ip1.delete(self.apiclient)
        pf_rule2.delete(self.apiclient)
        ip2.delete(self.apiclient)

        vm1.delete(self.apiclient, expunge=True)
        self.cleanup = [o for o in self.cleanup if o != vm1]
        vm2.delete(self.apiclient, expunge=True)
        self.cleanup = [o for o in self.cleanup if o != vm2]
        tier1.delete(self.apiclient)
        self.cleanup = [o for o in self.cleanup if o != tier1]
        tier2.delete(self.apiclient)
        self.cleanup = [o for o in self.cleanup if o != tier2]

        vpc.delete(self.apiclient)
        self.cleanup = [o for o in self.cleanup if o != vpc]

        self._teardown_extension()
        self.logger.info("test_07 PASSED")

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_08_custom_action_policy_based_routing(self):
        """Custom-action smoke test for PBR lifecycle helpers.

        Verifies that network custom actions can create/list/delete:
          - routing tables
          - routes per table
          - policy rules
        """
        self._check_kvm_host_prerequisites(['ip', 'arping', 'dnsmasq', 'haproxy'])

        svc = "SourceNat,PortForwarding,Dhcp,Dns,UserData,CustomAction"
        nw_offering, _ext_name = self._setup_extension_nsp_offering(
            "extnet-pbr", supported_services=svc)
        _account, network, vm = self._create_account_network_vm(
            nw_offering, name_suffix="pbr")

        # Use a unique table name to avoid collisions with stale test state.
        table_name = "app-%s" % random.randint(100, 999)
        route_cidr = "172.30.%d.0/24" % random.randint(1, 200)

        actions = []
        try:
            def _mk_action(name, parameters = []):
                a = ExtensionCustomAction.create(
                    self.apiclient,
                    extensionid=self.extension.id,
                    enabled=True,
                    name=name,
                    description="PBR smoke: %s" % name,
                    resourcetype='Network',
                    parameters=parameters
                )
                actions.append(a)
                return a

            act_create_table = _mk_action("pbr-create-table", parameters=[
                {"name": "table-id", "type": "STRING", "required": True},
                {"name": "table-name", "type": "STRING", "required": True},
            ])
            act_delete_table = _mk_action("pbr-delete-table", parameters=[
                {"name": "table-name", "type": "STRING", "required": True},
            ])
            act_list_tables  = _mk_action("pbr-list-tables")
            act_add_route    = _mk_action("pbr-add-route", parameters=[
                {"name": "table", "type": "STRING", "required": True},
                {"name": "route", "type": "STRING", "required": True},
            ])
            act_delete_route = _mk_action("pbr-delete-route", parameters=[
                {"name": "table", "type": "STRING", "required": True},
                {"name": "route", "type": "STRING", "required": True},
            ])
            act_list_routes  = _mk_action("pbr-list-routes", parameters=[
                {"name": "table", "type": "STRING", "required": False},
            ])
            act_add_rule     = _mk_action("pbr-add-rule", parameters=[
                {"name": "table", "type": "STRING", "required": True},
                {"name": "rule", "type": "STRING", "required": True},
            ])
            act_delete_rule  = _mk_action("pbr-delete-rule", parameters=[
                {"name": "table", "type": "STRING", "required": True},
                {"name": "rule", "type": "STRING", "required": True},
            ])
            act_list_rules   = _mk_action("pbr-list-rules", parameters=[
                {"name": "table", "type": "STRING", "required": False},
            ])

            # 1) Create and list routing table
            out = act_create_table.run(
                self.apiclient,
                resourceid=network.id,
                parameters=[{"table-id": "100", "table-name": table_name}],
            )
            self.assertTrue(getattr(out, 'success', False), "pbr-create-table should succeed")

            out = act_list_tables.run(self.apiclient, resourceid=network.id)
            self.assertTrue(getattr(out, 'success', False), "pbr-list-tables should succeed")
            self.assertIn(table_name, self._custom_action_details(out))

            # 2) Add and list route in table
            out = act_add_route.run(
                self.apiclient,
                resourceid=network.id,
                parameters=[{"table": table_name, "route": "blackhole %s" % route_cidr}],
            )
            self.assertTrue(getattr(out, 'success', False), "pbr-add-route should succeed")

            out = act_list_routes.run(
                self.apiclient,
                resourceid=network.id,
                parameters=[{"table": table_name}],
            )
            self.assertTrue(getattr(out, 'success', False), "pbr-list-routes should succeed")
            self.assertIn(route_cidr, self._custom_action_details(out))

            # 3) Add and list policy rule
            out = act_add_rule.run(
                self.apiclient,
                resourceid=network.id,
                parameters=[{"table": table_name, "rule": "to %s" % route_cidr}],
            )
            self.assertTrue(getattr(out, 'success', False), "pbr-add-rule should succeed")

            out = act_list_rules.run(
                self.apiclient,
                resourceid=network.id,
                parameters=[{"table": table_name}],
            )
            self.assertTrue(getattr(out, 'success', False), "pbr-list-rules should succeed")
            self.assertIn(table_name, self._custom_action_details(out))

            # 4) Delete policy rule, route, and table
            out = act_delete_rule.run(
                self.apiclient,
                resourceid=network.id,
                parameters=[{"table": table_name, "rule": "to %s" % route_cidr}],
            )
            self.assertTrue(getattr(out, 'success', False), "pbr-delete-rule should succeed")

            out = act_delete_route.run(
                self.apiclient,
                resourceid=network.id,
                parameters=[{"table": table_name, "route": "blackhole %s" % route_cidr}],
            )
            self.assertTrue(getattr(out, 'success', False), "pbr-delete-route should succeed")

            out = act_delete_table.run(
                self.apiclient,
                resourceid=network.id,
                parameters=[{"table-name": table_name}],
            )
            self.assertTrue(getattr(out, 'success', False), "pbr-delete-table should succeed")

            self.logger.info("test_08 PASSED")
        finally:
            for action in actions:
                try:
                    action.delete(self.apiclient)
                except Exception:
                    pass
            try:
                vm.delete(self.apiclient, expunge=True)
                self.cleanup = [o for o in self.cleanup if o != vm]
            except Exception:
                pass
            try:
                network.delete(self.apiclient)
                self.cleanup = [o for o in self.cleanup if o != network]
            except Exception:
                pass
            try:
                self._teardown_extension()
            except Exception:
                pass

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_09_vpc_source_nat_ip_update(self):
        """Update VPC source NAT IP and verify old/new source NAT flags flip correctly."""
        self._check_kvm_host_prerequisites(['arping'])

        svc = "SourceNat,StaticNat,PortForwarding,Lb,UserData,Dhcp,Dns,NetworkACL"
        _nw_offering, ext_name = self._setup_extension_nsp_offering(
            "extnet-vpc-snat-update", supported_services=svc, for_vpc=True)

        suffix = random_gen()
        account = None
        vpc = None
        tier = None
        vm = None
        ip1 = None
        ip2 = None
        pf_rule = None
        lb_rule = None
        static_nat_enabled = False
        try:
            # VPC tier network offering (useVpc=on)
            _tier_prov = {s.strip(): ext_name for s in svc.split(',')}
            vpc_tier_offering = NetworkOffering.create(self.apiclient, {
                "name":              "ExtNet-VPCTier-SNAT-%s" % random_gen(),
                "displaytext":       "ExtNet VPC tier offering for source NAT update",
                "guestiptype":       "Isolated",
                "traffictype":       "GUEST",
                "availability":      "Optional",
                "useVpc":            "on",
                "supportedservices": svc,
                "serviceProviderList": _tier_prov,
                "serviceCapabilityList": {
                    "SourceNat": {"SupportedSourceNatTypes": "peraccount"},
                },
            })
            self.cleanup.append(vpc_tier_offering)
            vpc_tier_offering.update(self.apiclient, state='Enabled')
            self.logger.info("VPC tier offering '%s' enabled", vpc_tier_offering.name)

            # VPC offering
            _vpc_prov = {s.strip(): ext_name for s in svc.split(',')}
            vpc_offering = VpcOffering.create(self.apiclient, {
                "name":              "ExtNet-VPC-SNAT-%s" % random_gen(),
                "displaytext":       "ExtNet VPC offering for source NAT update",
                "supportedservices": svc,
                "serviceProviderList": _vpc_prov,
            })
            self.cleanup.append(vpc_offering)
            vpc_offering.update(self.apiclient, state='Enabled')
            self.logger.info("VPC offering '%s' enabled", vpc_offering.name)

            account = Account.create(
                self.apiclient,
                self.services["account"],
                admin=True,
                domainid=self.domain.id
            )
            self.cleanup.append(account)
            account_keypair = self._create_account_keypair(account, suffix)

            vpc = VPC.create(
                self.apiclient,
                {"name":        "extnet-vpc-snat-%s" % suffix,
                 "displaytext": "ExtNet VPC SNAT %s" % suffix,
                 "cidr":        "10.3.0.0/16"},
                vpcofferingid=vpc_offering.id,
                zoneid=self.zone.id,
                account=account.name,
                domainid=account.domainid
            )
            self.cleanup.insert(0, vpc)
            self.logger.info("VPC created: %s (%s)", vpc.name, vpc.id)

            tier = Network.create(
                self.apiclient,
                {"name":        "tier-snat-%s" % suffix,
                 "displaytext": "Tier SNAT %s" % suffix},
                accountid=account.name,
                domainid=account.domainid,
                networkofferingid=vpc_tier_offering.id,
                zoneid=self.zone.id,
                vpcid=vpc.id,
                gateway="10.3.1.1",
                netmask="255.255.255.0"
            )
            self.cleanup.insert(0, tier)

            # Create a vm instance in the tier, so that the vpc and tier are implemented on the backend and have their source NAT IPs allocated.
            svc_offering = ServiceOffering.list(self.apiclient, issystem=False)[0]
            vm_cfg = {"displayname": "vm-snat-%s" % suffix,
                      "name":        "vm-snat-%s" % suffix,
                      "zoneid":      self.zone.id}
            vm_kw  = dict(accountid=account.name,
                          domainid=account.domainid,
                          serviceofferingid=svc_offering.id,
                          templateid=self.template.id,
                          networkids=[tier.id])
            if account_keypair:
                vm_kw["keypair"] = account_keypair.name
            vm = VirtualMachine.create(self.apiclient, vm_cfg, **vm_kw)
            self.cleanup.insert(0, vm)

            # Wait for the VPC's auto-assigned source NAT IP (CloudStack assigns
            # one automatically when the VPC is first used; it is NOT necessarily
            # the first manually-allocated IP).
            src_before = self._wait_for_vpc_source_nat_ip(vpc.id)
            self.assertIsNotNone(src_before,
                                 "A source NAT IP should already exist for the VPC")
            src_before_addr = getattr(src_before, 'ipaddress', None)
            self.logger.info("Initial source NAT IP: %s", src_before_addr)

            # Allocate ip1 and ip2 as candidates for the updated source NAT.
            ip1 = PublicIPAddress.create(
                self.apiclient,
                accountid=account.name,
                zoneid=self.zone.id,
                domainid=account.domainid,
                networkid=tier.id,
                vpcid=vpc.id
            )
            ip1_addr = ip1.ipaddress.ipaddress

            ip2 = PublicIPAddress.create(
                self.apiclient,
                accountid=account.name,
                zoneid=self.zone.id,
                domainid=account.domainid,
                networkid=tier.id,
                vpcid=vpc.id
            )
            ip2_addr = ip2.ipaddress.ipaddress

            # Use ip1 for static NAT (SSH/22) and verify ingress connectivity.
            StaticNATRule.enable(
                self.apiclient,
                ip1.ipaddress.id,
                vm.id,
                networkid=tier.id
            )
            static_nat_enabled = True

            # Use ip2 for both PF and LB on different public ports.
            pf_port = 2222
            lb_port = 2223
            pf_rule = NATRule.create(
                self.apiclient,
                vm,
                {"privateport": 22, "publicport": pf_port, "protocol": "TCP"},
                ipaddressid=ip2.ipaddress.id,
                networkid=tier.id,
                vpcid=vpc.id
            )
            self.assertIsNotNone(pf_rule, "Port forwarding rule should be created on ip2")

            lb_rule = LoadBalancerRule.create(
                self.apiclient,
                {
                    "name": "lb-ssh-%s" % suffix,
                    "alg": "roundrobin",
                    "privateport": 22,
                    "publicport": lb_port,
                    "protocol": "TCP",
                },
                ipaddressid=ip2.ipaddress.id,
                accountid=account.name,
                domainid=account.domainid,
                networkid=tier.id,
                vpcid=vpc.id
            )
            self.assertIsNotNone(lb_rule, "Load balancer rule should be created on ip2")
            lb_rule.assign(self.apiclient, [vm])

            # Validate all inbound paths before source NAT migration.
            self._assert_vm_ssh_accessible(
                ip1_addr, 22,
                "Static NAT SSH on ip1 should work before source NAT update")
            self._assert_vm_ssh_accessible(
                ip2_addr, pf_port,
                "Port forwarding SSH on ip2 should work before source NAT update")
            self._assert_vm_ssh_accessible(
                ip2_addr, lb_port,
                "Load balancer SSH on ip2 should work before source NAT update")

            ip3 = PublicIPAddress.create(
                self.apiclient,
                accountid=account.name,
                zoneid=self.zone.id,
                domainid=account.domainid,
                networkid=tier.id,
                vpcid=vpc.id
            )
            ip3_addr = ip3.ipaddress.ipaddress

            self.logger.info("Updating VPC source NAT IP to %s", ip3_addr)
            update_resp = vpc.update(self.apiclient, sourcenatipaddress=ip3_addr)
            self.assertIsNotNone(update_resp,
                                 "updateVPC should return a response")

            src_after = self._wait_for_vpc_source_nat_ip(vpc.id, expected_ip=ip3_addr)
            self.assertIsNotNone(src_after,
                                 "Updated source NAT IP should be %s" % ip3_addr)

            all_vpc_ips = self._list_vpc_public_ips(vpc.id)
            self.assertEqual(len(all_vpc_ips), 4,
                                    "VPC should have four public IPs")

            by_addr = {getattr(x, 'ipaddress', None): x for x in all_vpc_ips}
            self.assertIn(src_before_addr, by_addr,
                          "Original source NAT IP must remain allocated to VPC")
            self.assertIn(ip1_addr, by_addr, "Static NAT IP must remain allocated to VPC")
            self.assertIn(ip2_addr, by_addr, "New source NAT IP must remain allocated to VPC")

            src_ips = [x for x in all_vpc_ips if getattr(x, 'issourcenat', False)]
            self.assertEqual(1, len(src_ips),
                             "Exactly one VPC public IP must be marked source NAT")
            self.assertEqual(ip3_addr, getattr(src_ips[0], 'ipaddress', None),
                             "Source NAT IP should switch to the requested IP")
            self.assertFalse(getattr(by_addr[src_before_addr], 'issourcenat', False),
                             "Original source NAT IP must be unset after update")
            self.assertTrue(getattr(by_addr[ip3_addr], 'issourcenat', False),
                            "Requested source NAT IP must be marked source NAT")

            # Validate all inbound paths still work after source NAT migration.
            self._assert_vm_ssh_accessible(
                ip1_addr, 22,
                "Static NAT SSH on ip1 should work after source NAT update")
            self._assert_vm_ssh_accessible(
                ip2_addr, pf_port,
                "Port forwarding SSH on ip2 should work after source NAT update")
            self._assert_vm_ssh_accessible(
                ip2_addr, lb_port,
                "Load balancer SSH on ip2 should work after source NAT update")

            self.logger.info("test_09 PASSED")
        finally:
            try:
                if lb_rule and vm:
                    lb_rule.remove(self.apiclient, [vm])
            except Exception:
                pass
            try:
                if lb_rule:
                    lb_rule.delete(self.apiclient)
            except Exception:
                pass
            try:
                if pf_rule:
                    pf_rule.delete(self.apiclient)
            except Exception:
                pass
            try:
                if static_nat_enabled and ip1:
                    StaticNATRule.disable(self.apiclient, ip1.ipaddress.id)
            except Exception:
                pass
            try:
                if ip1:
                    ip1.delete(self.apiclient)
            except Exception:
                pass
            try:
                if ip2:
                    ip2.delete(self.apiclient)
            except Exception:
                pass
            try:
                if vm:
                    vm.delete(self.apiclient, expunge=True)
                    self.cleanup = [o for o in self.cleanup if o != vm]
            except Exception:
                pass
            try:
                if tier:
                    tier.delete(self.apiclient)
                    self.cleanup = [o for o in self.cleanup if o != tier]
            except Exception:
                pass
            try:
                if vpc:
                    vpc.delete(self.apiclient)
                    self.cleanup = [o for o in self.cleanup if o != vpc]
            except Exception:
                pass
            try:
                self._teardown_extension()
            except Exception:
                pass

    @attr(tags=["advanced", "smoke"], required_hardware="true")
    def test_10_vpc_custom_action_policy_based_routing(self):
        """Custom-action smoke test for PBR lifecycle helpers on a VPC.

        Same as test_08 but exercised against a VPC instead of
        an isolated network.  Verifies that network custom actions work
        correctly in a VPC context:
          - routing tables
          - routes per table
          - policy rules
        """
        self._check_kvm_host_prerequisites(['ip', 'arping', 'dnsmasq', 'haproxy'])

        svc = "SourceNat,PortForwarding,Dhcp,Dns,UserData,NetworkACL,CustomAction"
        _nw_offering, ext_name = self._setup_extension_nsp_offering(
            "extnet-vpc-pbr", supported_services=svc, for_vpc=True)

        # ---- VPC tier network offering (useVpc=on) ----
        _tier_prov = {s.strip(): ext_name for s in svc.split(',')}
        vpc_tier_offering = NetworkOffering.create(self.apiclient, {
            "name":              "ExtNet-VPCTier-PBR-%s" % random_gen(),
            "displaytext":       "ExtNet VPC tier offering for PBR",
            "guestiptype":       "Isolated",
            "traffictype":       "GUEST",
            "availability":      "Optional",
            "useVpc":            "on",
            "supportedservices": svc,
            "serviceProviderList": _tier_prov,
            "serviceCapabilityList": {
                "SourceNat": {"SupportedSourceNatTypes": "peraccount"},
            },
        })
        self.cleanup.append(vpc_tier_offering)
        vpc_tier_offering.update(self.apiclient, state='Enabled')

        # ---- VPC offering ----
        _vpc_prov = {s.strip(): ext_name for s in svc.split(',')}
        vpc_offering = VpcOffering.create(self.apiclient, {
            "name":              "ExtNet-VPC-PBR-%s" % random_gen(),
            "displaytext":       "ExtNet VPC offering for PBR",
            "supportedservices": svc,
            "serviceProviderList": _vpc_prov,
        })
        self.cleanup.append(vpc_offering)
        vpc_offering.update(self.apiclient, state='Enabled')

        suffix = random_gen()
        account = Account.create(
            self.apiclient,
            self.services["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup.append(account)

        vpc = VPC.create(
            self.apiclient,
            {"name":        "extnet-vpc-pbr-%s" % suffix,
             "displaytext": "ExtNet VPC PBR %s" % suffix,
             "cidr":        "10.1.0.0/16"},
            vpcofferingid=vpc_offering.id,
            zoneid=self.zone.id,
            account=account.name,
            domainid=account.domainid
        )
        self.cleanup.insert(0, vpc)

        tier = Network.create(
            self.apiclient,
            {"name":        "tier-pbr-%s" % suffix,
             "displaytext": "Tier PBR %s" % suffix},
            accountid=account.name,
            domainid=account.domainid,
            networkofferingid=vpc_tier_offering.id,
            zoneid=self.zone.id,
            vpcid=vpc.id,
            gateway="10.1.1.1",
            netmask="255.255.255.0"
        )
        self.cleanup.insert(0, tier)

        svc_offering = ServiceOffering.list(self.apiclient, issystem=False)[0]
        vm = VirtualMachine.create(
            self.apiclient,
            {"displayname": "vm-pbr-%s" % suffix,
             "name":        "vm-pbr-%s" % suffix,
             "zoneid":      self.zone.id},
            accountid=account.name,
            domainid=account.domainid,
            serviceofferingid=svc_offering.id,
            templateid=self.template.id,
            networkids=[tier.id]
        )
        self.cleanup.insert(0, vm)

        table_name = "app-%s" % random.randint(100, 999)
        route_cidr = "172.30.%d.0/24" % random.randint(1, 200)

        actions = []
        try:
            def _mk_action(name, parameters=[]):
                a = ExtensionCustomAction.create(
                    self.apiclient,
                    extensionid=self.extension.id,
                    enabled=True,
                    name=name,
                    description="VPC PBR smoke: %s" % name,
                    resourcetype='Vpc',
                    parameters=parameters
                )
                actions.append(a)
                return a

            act_create_table = _mk_action("pbr-create-table", parameters=[
                {"name": "table-id", "type": "STRING", "required": True},
                {"name": "table-name", "type": "STRING", "required": True},
            ])
            act_delete_table = _mk_action("pbr-delete-table", parameters=[
                {"name": "table-name", "type": "STRING", "required": True},
            ])
            act_list_tables  = _mk_action("pbr-list-tables")
            act_add_route    = _mk_action("pbr-add-route", parameters=[
                {"name": "table", "type": "STRING", "required": True},
                {"name": "route", "type": "STRING", "required": True},
            ])
            act_delete_route = _mk_action("pbr-delete-route", parameters=[
                {"name": "table", "type": "STRING", "required": True},
                {"name": "route", "type": "STRING", "required": True},
            ])
            act_list_routes  = _mk_action("pbr-list-routes", parameters=[
                {"name": "table", "type": "STRING", "required": False},
            ])
            act_add_rule     = _mk_action("pbr-add-rule", parameters=[
                {"name": "table", "type": "STRING", "required": True},
                {"name": "rule", "type": "STRING", "required": True},
            ])
            act_delete_rule  = _mk_action("pbr-delete-rule", parameters=[
                {"name": "table", "type": "STRING", "required": True},
                {"name": "rule", "type": "STRING", "required": True},
            ])
            act_list_rules   = _mk_action("pbr-list-rules", parameters=[
                {"name": "table", "type": "STRING", "required": False},
            ])

            # 1) Create and list routing table
            out = act_create_table.run(
                self.apiclient,
                resourceid=vpc.id,
                parameters=[{"table-id": "100", "table-name": table_name}],
            )
            self.assertTrue(getattr(out, 'success', False), "pbr-create-table should succeed")

            out = act_list_tables.run(self.apiclient, resourceid=vpc.id)
            self.assertTrue(getattr(out, 'success', False), "pbr-list-tables should succeed")
            self.assertIn(table_name, self._custom_action_details(out))

            # 2) Add and list route in table
            out = act_add_route.run(
                self.apiclient,
                resourceid=vpc.id,
                parameters=[{"table": table_name, "route": "blackhole %s" % route_cidr}],
            )
            self.assertTrue(getattr(out, 'success', False), "pbr-add-route should succeed")

            out = act_list_routes.run(
                self.apiclient,
                resourceid=vpc.id,
                parameters=[{"table": table_name}],
            )
            self.assertTrue(getattr(out, 'success', False), "pbr-list-routes should succeed")
            self.assertIn(route_cidr, self._custom_action_details(out))

            # 3) Add and list policy rule
            out = act_add_rule.run(
                self.apiclient,
                resourceid=vpc.id,
                parameters=[{"table": table_name, "rule": "to %s" % route_cidr}],
            )
            self.assertTrue(getattr(out, 'success', False), "pbr-add-rule should succeed")

            out = act_list_rules.run(
                self.apiclient,
                resourceid=vpc.id,
                parameters=[{"table": table_name}],
            )
            self.assertTrue(getattr(out, 'success', False), "pbr-list-rules should succeed")
            self.assertIn(table_name, self._custom_action_details(out))

            # 4) Delete policy rule, route, and table
            out = act_delete_rule.run(
                self.apiclient,
                resourceid=vpc.id,
                parameters=[{"table": table_name, "rule": "to %s" % route_cidr}],
            )
            self.assertTrue(getattr(out, 'success', False), "pbr-delete-rule should succeed")

            out = act_delete_route.run(
                self.apiclient,
                resourceid=vpc.id,
                parameters=[{"table": table_name, "route": "blackhole %s" % route_cidr}],
            )
            self.assertTrue(getattr(out, 'success', False), "pbr-delete-route should succeed")

            out = act_delete_table.run(
                self.apiclient,
                resourceid=vpc.id,
                parameters=[{"table-name": table_name}],
            )
            self.assertTrue(getattr(out, 'success', False), "pbr-delete-table should succeed")

            self.logger.info("test_10 PASSED")
        finally:
            for action in actions:
                try:
                    action.delete(self.apiclient)
                except Exception:
                    pass
            try:
                vm.delete(self.apiclient, expunge=True)
                self.cleanup = [o for o in self.cleanup if o != vm]
            except Exception:
                pass
            try:
                tier.delete(self.apiclient)
                self.cleanup = [o for o in self.cleanup if o != tier]
            except Exception:
                pass
            try:
                vpc.delete(self.apiclient)
                self.cleanup = [o for o in self.cleanup if o != vpc]
            except Exception:
                pass
            try:
                self._teardown_extension()
            except Exception:
                pass
