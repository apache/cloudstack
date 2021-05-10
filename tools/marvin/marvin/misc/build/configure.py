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

from configparser import ConfigParser
from bashUtils import bash
from marvin import configGenerator
from marvin import sshClient
from marvin import dbConnection
from argparse import ArgumentParser
from time import sleep as delay
from netaddr import IPNetwork
from netaddr import IPAddress
import contextlib
import telnetlib
import logging
import threading
import queue
import sys
import random
import string
import urllib.request, urllib.error, urllib.parse
import urllib.parse
import socket

WORKSPACE="."
IPMI_PASS="calvin"
DOMAIN = 'fmt.vmops.com'

macinfo = {}
ipmiinfo = {}
cobblerinfo = {}

def generate_system_tables(config):
    dhcp = config.items("dhcp")
    for entry in dhcp:
        macinfo[entry[0]] = {}
        mac, passwd, ip =  entry[1].split(",")
        macinfo[entry[0]]["ethernet"] = mac
        macinfo[entry[0]]["password"] = passwd
        macinfo[entry[0]]["address"] = ip

    ipmi = config.items("ipmi")
    for entry in ipmi:
        ipmiinfo[entry[0]] = entry[1]

    cobbler = config.items("cobbler")
    for entry in cobbler:
        cobblerinfo[entry[0]] = {}
        net, gw, cblrgw = entry[1].split(",")
        cobblerinfo[entry[0]]["network"] = net
        cobblerinfo[entry[0]]["gateway"] = gw
        cobblerinfo[entry[0]]["cblrgw"] = cblrgw

def initLogging(logFile=None, lvl=logging.INFO):
    try:
        if logFile is None:
            logging.basicConfig(level=lvl, \
                                format="'%(asctime)-6s: %(name)s \
                                (%(threadName)s) - %(levelname)s - %(message)s'") 
        else: 
            logging.basicConfig(filename=logFile, level=lvl, \
                                format="'%(asctime)-6s: %(name)s \
                                (%(threadName)s) - %(levelname)s - %(message)s'") 
    except:
        logging.basicConfig(level=lvl) 

def mkdirs(path):
    dir = bash("mkdir -p %s" % path)

def fetch(filename, url, path):
    try:
        zipstream = urllib.request.urlopen(url)
        tarball = open('/tmp/%s' % filename, 'wb')
        tarball.write(zipstream.read())
        tarball.close()
    except urllib.error.URLError as u:
        raise u
    except IOError:
        raise
    bash("mv /tmp/%s %s" % (filename, path))

def cobblerHomeResolve(ip_address, param="gateway"):
    ipAddr = IPAddress(ip_address)
    for nic, network in list(cobblerinfo.items()):
        subnet = IPNetwork(cobblerinfo[nic]["network"])
        if ipAddr in subnet:
            return cobblerinfo[nic][param]

def configureManagementServer(mgmt_host):
    """
    We currently configure all mgmt servers on a single xenserver HV. In the future
    replace this by launching instances via the API on a IaaS cloud using
    desired template
    """
    mgmt_vm = macinfo[mgmt_host]
    mgmt_ip = macinfo[mgmt_host]["address"]

    #Remove and re-add cobbler system
    bash("cobbler system remove --name=%s"%mgmt_host)
    bash("cobbler system add --name=%s --hostname=%s --mac-address=%s \
         --netboot-enabled=yes --enable-gpxe=no \
         --profile=%s --server=%s --gateway=%s"%(mgmt_host, mgmt_host,
                                                 mgmt_vm["ethernet"], mgmt_host,
                                                 cobblerHomeResolve(mgmt_ip, param='cblrgw'),
                                                 cobblerHomeResolve(mgmt_ip)));
    bash("cobbler sync")

    #Revoke all certs from puppetmaster
    bash("puppet cert clean %s.%s"%(mgmt_host, DOMAIN))

    #Start VM on xenserver
    xenserverssh = \
    sshClient.SshClient(macinfo["infraxenserver"]["address"],
                                    22, "root",
                                    macinfo["infraxenserver"]["password"])

    logging.debug("bash vm-uninstall.sh -n %s"%(mgmt_host))
    xenserverssh.execute("xe vm-uninstall force=true vm=%s"%mgmt_host)
    logging.debug("bash vm-start.sh -n %s -m %s"%(mgmt_host, mgmt_vm["ethernet"]))
    out = xenserverssh.execute("bash vm-start.sh -n %s -m %s"%(mgmt_host,
                                                  mgmt_vm["ethernet"]))

    logging.info("started mgmt server with uuid: %s. Waiting for services .."%out);
    return mgmt_host

def mountAndClean(host, path):
    """
    Will mount and clear the files on NFS host in the path given. Obviously the
    NFS server should be mountable where this script runs
    """
    mnt_path = "/tmp/" + ''.join([random.choice(string.ascii_uppercase) for x in range(0, 10)])
    mkdirs(mnt_path)
    logging.info("cleaning up %s:%s" % (host, path))
    mnt = bash("mount -t nfs %s:%s %s" % (host, path, mnt_path))
    erase = bash("rm -rf %s/*" % mnt_path)
    umnt = bash("umount %s" % mnt_path)
   
def cleanPrimaryStorage(cscfg):
    """
    Clean all the NFS primary stores and prepare them for the next run
    """
    for zone in cscfg.zones:
        for pod in zone.pods:
            for cluster in pod.clusters:
                for primaryStorage in cluster.primaryStorages:
                    if urllib.parse.urlsplit(primaryStorage.url).scheme == "nfs":
                        mountAndClean(urllib.parse.urlsplit(primaryStorage.url).hostname, urllib.parse.urlsplit(primaryStorage.url).path)
    logging.info("Cleaned up primary stores")

def seedSecondaryStorage(cscfg, hypervisor):
    """
    erase secondary store and seed system VM template via puppet. The
    secseeder.sh script is executed on mgmt server bootup which will mount and
    place the system VM templates on the NFS
    """
    mgmt_server = cscfg.mgtSvr[0].mgtSvrIp
    logging.info("Secondary storage seeded via puppet with systemvm templates")
    bash("rm -f /etc/puppet/modules/cloudstack/files/secseeder.sh")
    for zone in cscfg.zones:
        for sstor in zone.secondaryStorages:
            shost = urllib.parse.urlsplit(sstor.url).hostname
            spath = urllib.parse.urlsplit(sstor.url).path
            spath = ''.join([shost, ':', spath])
            logging.info("seeding %s systemvm template on %s"%(hypervisor, spath))
            bash("echo '/bin/bash /root/redeploy.sh -s %s -h %s' >> /etc/puppet/modules/cloudstack/files/secseeder.sh"%(spath, hypervisor))
    bash("chmod +x /etc/puppet/modules/cloudstack/files/secseeder.sh")

def refreshHosts(cscfg, hypervisor="xenserver", profile="xenserver602"):
    """
    Removes cobbler system from previous run. 
    Creates a new system for current run.
    Ipmi boots from PXE - default to Xenserver profile
    """
    for zone in cscfg.zones:
        for pod in zone.pods:
            for cluster in pod.clusters:
                for host in cluster.hosts:
                    hostname = urllib.parse.urlsplit(host.url).hostname
                    logging.debug("attempting to refresh host %s"%hostname)
                    #revoke certs
                    bash("puppet cert clean %s.%s"%(hostname, DOMAIN))
                    #setup cobbler profiles and systems
                    try:
                        hostmac = macinfo[hostname]['ethernet']
                        hostip = macinfo[hostname]['address']
                        bash("cobbler system remove \
                             --name=%s"%(hostname))
                        bash("cobbler system add --name=%s --hostname=%s \
                             --mac-address=%s --netboot-enabled=yes \
                             --enable-gpxe=no --profile=%s --server=%s \
                             --gateway=%s"%(hostname, hostname, hostmac,
                                            profile, cobblerHomeResolve(hostip, param='cblrgw'),
                                            cobblerHomeResolve(hostip)))

                        bash("cobbler sync")
                    except KeyError:
                        logging.error("No mac found against host %s. Exiting"%hostname)
                        sys.exit(2)
                    #set ipmi to boot from PXE
                    try:
                        ipmi_hostname = ipmiinfo[hostname]
                        logging.debug("found IPMI nic on %s for host %s"%(ipmi_hostname, hostname))
                        bash("ipmitool -Uroot -P%s -H%s chassis bootdev \
                             pxe"%(IPMI_PASS, ipmi_hostname))
                        bash("ipmitool -Uroot -P%s -H%s chassis power cycle"
                             %(IPMI_PASS, ipmi_hostname))
                        logging.debug("Sent PXE boot for %s"%ipmi_hostname)
                    except KeyError:
                        logging.error("No ipmi host found against %s. Exiting"%hostname)
                        sys.exit(2)
                    yield hostname
    delay(5) #to begin pxe boot process or wait returns immediately

def _isPortListening(host, port, timeout=120):
    """
    Scans 'host' for a listening service on 'port'
    """
    tn = None
    while timeout != 0:
        try:
            tn = telnetlib.Telnet(host, port, timeout=timeout)
            timeout = 0
        except Exception as e:
            logging.debug("Failed to telnet connect to %s:%s with %s"%(host, port, e))
            delay(5)
            timeout = timeout - 5
    if tn is None:
        logging.error("No service listening on port %s:%d"%(host, port))
        return False 
    else:
        logging.info("Unrecognizable service up on %s:%d"%(host, port))
        return True

def _isPortOpen(hostQueue, port=22):
    """
    Checks if there is an open socket on specified port. Default is SSH
    """
    ready = []
    host = hostQueue.get()
    while True:
        channel = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        channel.settimeout(20)
        try:
            logging.debug("Attempting port=%s connect to host %s"%(port, host))
            err = channel.connect_ex((host, port))
        except socket.error as e:
            logging.debug("encountered %s retrying in 5s"%e)
            err = e.errno
            delay(5)
        finally:
            if err == 0:
                ready.append(host)
                logging.info("host: %s is ready"%host)
                break
            else:
                logging.debug("[%s] host %s is not ready. Retrying"%(err, host))
                delay(5)
                channel.close()
    hostQueue.task_done()

def waitForHostReady(hostlist):
    logging.info("Waiting for hosts %s to refresh"%hostlist)
    hostQueue = queue.Queue()

    for host in hostlist:
        t = threading.Thread(name='HostWait-%s'%hostlist.index(host), target=_isPortOpen,
                             args=(hostQueue, ))
        t.setDaemon(True)
        t.start()

    [hostQueue.put(host) for host in hostlist]
    hostQueue.join()
    logging.info("All hosts %s are up"%hostlist)

def isManagementServiceStable(ssh=None, timeout=300, interval=5):
    logging.info("Waiting for cloudstack-management service to become stable")
    if ssh is None:
        return False
    while timeout != 0:
        cs_status = ''.join(ssh.execute("service cloudstack-management status"))
        logging.debug("[-%ds] Cloud Management status: %s"%(timeout, cs_status))
        if cs_status.find('running') > 0:
            pass
        else:
            ssh.execute("service cloudstack-management restart")
        timeout = timeout - interval
        delay(interval)

def testManagementServer(mgmt_host):
    """
    Test that the cloudstack service is up
    """
    #TODO: Add user registration step
    mgmt_ip = macinfo[mgmt_host]["address"]
    mgmt_pass = macinfo[mgmt_host]["password"]
    with contextlib.closing(sshClient.SshClient(mgmt_ip, 22, "root", mgmt_pass)) as ssh:
        isManagementServiceStable(ssh, timeout=60)

def prepareManagementServer(mgmt_host):
    """
    Prepare the mgmt server for a marvin test run
    """
    if _isPortListening(host=mgmt_host, port=22, timeout=10) \
            and _isPortListening(host=mgmt_host, port=3306, timeout=10) \
            and _isPortListening(host=mgmt_host, port=8080, timeout=300):
        delay(120) #introduce dumb delay
        mgmt_ip = macinfo[mgmt_host]["address"]
        mgmt_pass = macinfo[mgmt_host]["password"]
        with contextlib.closing(sshClient.SshClient(mgmt_ip, 22, "root", mgmt_pass)) as ssh:
            # Open up 8096 for Marvin initial signup and register
            ssh.execute("mysql -ucloud -pcloud -Dcloud -e\"update configuration set value=8096 where name like 'integr%'\"")
            ssh.execute("service cloudstack-management restart")
    else:
        raise Exception("Reqd services (ssh, mysql) on management server are not up. Aborting")

    if _isPortListening(host=mgmt_host, port=8096, timeout=300):
        logging.info("All reqd services are up on the management server %s"%mgmt_host)
        testManagementServer(mgmt_host)
        return
    else:
        with contextlib.closing(sshClient.SshClient(mgmt_ip, 22, "root", mgmt_pass)) as ssh:
            # Force kill java process
            ssh.execute("killall -9 java; service cloudstack-management start")

    if _isPortListening(host=mgmt_host, port=8096, timeout=300):
        logging.info("All reqd services are up on the management server %s"%mgmt_host)
        testManagementServer(mgmt_host)
        return
    else:
        raise Exception("Reqd service for integration port on management server %s is not open. Aborting"%mgmt_host)
    
def init(lvl=logging.INFO):
    initLogging(logFile=None, lvl=lvl)
        
if __name__ == '__main__':
    parser = ArgumentParser()
    parser.add_argument("-l", "--logging", action="store", default="INFO",
                      dest="loglvl", help="logging level (INFO|DEBUG|)")
    parser.add_argument("-d", "--distro", action="store",
                      dest="distro", help="management server distro")
    parser.add_argument("-v", "--hypervisor", action="store",
            dest="hypervisor", help="hypervisor type")
    parser.add_argument("-p", "--profile", action="store", default="xenserver602",
                      dest="profile", help="cobbler profile for hypervisor")
    parser.add_argument("-e","--environment", help="environment properties file",
                      dest="system", action="store")
    options = parser.parse_args()

    if options.loglvl == "DEBUG":
        init(logging.DEBUG)
    elif options.loglvl == "INFO":
        init(logging.INFO)
    else:
        init(logging.INFO)
        
    if options.system is None:
        logging.error("no environment properties given. exiting")
        sys.exit(-1)

    system = ConfigParser()
    try:
        with open(options.system, 'r') as cfg:
            system.readfp(cfg)
    except IOError as e:
        logging.error("Specify a valid path for the environment properties")
        raise e
    generate_system_tables(system)

    hosts = []
    prepare_mgmt = False
    if options.distro is not None:
        #Management Server configuration - only tests the packaging
        mgmt_host = "cloudstack-"+options.distro
        prepare_mgmt = True
        logging.info("Configuring management server %s"%mgmt_host)
        hosts.append(configureManagementServer(mgmt_host))

    if options.hypervisor is not None:
        #FIXME: query profiles from hypervisor args through cobbler api
        auto_config = options.hypervisor + ".cfg"
        cscfg = configGenerator.getSetupConfig(auto_config)
        logging.info("Reimaging hosts with %s profile for the %s \
                     hypervisor" % (options.profile, options.hypervisor))
        hosts.extend(refreshHosts(cscfg, options.hypervisor, options.profile))
        seedSecondaryStorage(cscfg, options.hypervisor)
        cleanPrimaryStorage(cscfg)

    waitForHostReady(hosts)
    delay(30)
    # Re-check because ssh connect works soon as post-installation occurs. But 
    # server is rebooted after post-installation. Assuming the server is up is
    # wrong in these cases. To avoid this we will check again before continuing
    # to add the hosts to cloudstack
    waitForHostReady(hosts)
    if prepare_mgmt:
        prepareManagementServer(mgmt_host)
    logging.info("All systems go!")
