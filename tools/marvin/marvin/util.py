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

import marvin
import os
import time
import logging
import string
import random
import imaplib
import email
import socket
import urlparse
import datetime
from marvin.cloudstackAPI import *
from marvin.remoteSSHClient import remoteSSHClient
from marvin.entity.template import Template
from marvin.entity.zone import Zone
from marvin.entity.serviceoffering import ServiceOffering
from marvin.entity.domain import Domain
from marvin.entity.configuration import Configuration


def get_domain(apiclient):
    "Returns a default `ROOT` domain"

    domains = Domain.list(
        apiclient=apiclient,
    )
    if isinstance(domains, list) and len(domains) > 0:
        return domains[0]
    else:
        raise Exception("Failed to find any domains")


def get_zone(apiclient):
    "Returns the default enabled zone"

    zones = Zone.list(
        apiclient=apiclient,
    )
    if isinstance(zones, list) and len(zones) > 0:
        for zone in zones:
            if zone.allocationstate == 'Enabled':
                return zone
        else:
            raise Exception("No active zones found for deployment")
    else:
        raise Exception("Failed to find specified zone.")


def get_service_offering(apiclient, storagetype='shared', scope=None):
    """Returns the service offering that is available in the zone

    @param: `storagetype` is assumed to be `shared storage`
    @param: `scope` zone-wide or cluster-wide. defaults to cluster
    """
    serviceofferings = ServiceOffering.list(
        apiclient=apiclient,
        name='Small Instance'
    )
    if isinstance(serviceofferings, list) and len(serviceofferings) > 0:
        for service in serviceofferings:
            if service.storagetype == storagetype:
                return service
    raise Exception("No service offering for storagetype %s available")


def get_template(apiclient, description=None):
    "Returns a featured template with a specific description"
    templates = Template.list(
        apiclient=apiclient,
        templatefilter='featured'
    )

    if isinstance(templates, list) and len(templates) > 0:
        for template in templates:
            if template.isready:
                return template
        else:
            raise Exception(
                "None of the templates are ready in your deployment")
    else:
        raise Exception(
            "Failed to find ready and featured template of : %s" % description)

def restart_mgmt_server(server):
    """Restarts the management server"""

    try:
        # Get the SSH client
        ssh = is_server_ssh_ready(
            server["ipaddress"],
            server["port"],
            server["username"],
            server["password"],
        )
        result = ssh.execute("/etc/init.d/cloud-management restart")
        res = str(result)
        # Server Stop - OK
        # Server Start - OK
        if res.count("OK") != 2:
            raise ("ErrorInReboot!")
    except Exception as e:
        raise e
    return


def fetch_latest_mail(services, from_mail):
    """Fetch mail"""

    # Login to mail server to verify email
    mail = imaplib.IMAP4_SSL(services["server"])
    mail.login(
        services["email"],
        services["password"]
    )
    mail.list()
    mail.select(services["folder"])
    date = (datetime.date.today() - datetime.timedelta(1)).strftime("%d-%b-%Y")

    result, data = mail.uid(
        'search',
        None,
        '(SENTSINCE {date} HEADER FROM "{mail}")'.format(
            date=date,
            mail=from_mail
        )
    )
    # Return False if email is not present
    if data == []:
        return False

    latest_email_uid = data[0].split()[-1]
    result, data = mail.uid('fetch', latest_email_uid, '(RFC822)')
    raw_email = data[0][1]
    email_message = email.message_from_string(raw_email)
    result = get_first_text_block(email_message)
    return result


def get_first_text_block(email_message_instance):
    """fetches first text block from the mail"""
    maintype = email_message_instance.get_content_maintype()
    if maintype == 'multipart':
        for part in email_message_instance.get_payload():
            if part.get_content_maintype() == 'text':
                return part.get_payload()
    elif maintype == 'text':
        return email_message_instance.get_payload()


def random_gen(id=None, size=6, chars=string.ascii_uppercase + string.digits):
    """Generate Random Strings of variable length"""
    randomstr = ''.join(random.choice(chars) for x in range(size))
    if id:
        return ''.join([id, '-', randomstr])
    return randomstr

def is_server_ssh_ready(ipaddress, port, username, password, retries=10, timeout=30, keyPairFileLocation=None):
    """Return ssh handle else wait till sshd is running"""
    try:
        ssh = remoteSSHClient(
            host=ipaddress,
            port=port,
            user=username,
            passwd=password,
            keyPairFileLocation=keyPairFileLocation,
            retries=retries,
            delay=timeout)
    except Exception, e:
        raise Exception("Failed to bring up ssh service in time. Waited %ss. Error is %s" % (retries * timeout, e))
    else:
        return ssh


def format_volume_to_ext3(ssh_client, device="/dev/sda"):
    """Format attached storage to ext3 fs"""
    cmds = [
        "echo -e 'n\np\n1\n\n\nw' | fdisk %s" % device,
        "mkfs.ext3 %s1" % device,
    ]
    for c in cmds:
        ssh_client.execute(c)


def fetch_api_client(config_file='datacenterCfg'):
    """Fetch the Cloudstack API Client"""
    config = marvin.configGenerator.get_setup_config(config_file)
    mgt = config.mgtSvr[0]
    testClientLogger = logging.getLogger("marvin.testClient")
    asyncTimeout = 3600
    return cloudstackAPIClient.CloudStackAPIClient(
        marvin.cloudstackConnection.cloudConnection(
            mgt.mgtSvrIp,
            mgt.port,
            mgt.apiKey,
            mgt.securityKey,
            asyncTimeout,
            testClientLogger
        )
    )

def get_host_credentials(config, hostip):
    """Get login information for a host `hostip` (ipv4) from marvin's `config`

    @return the tuple username, password for the host else raise keyerror"""
    for zone in config.zones:
        for pod in zone.pods:
            for cluster in pod.clusters:
                for host in cluster.hosts:
                    if str(host.url).startswith('http'):
                        hostname = urlparse.urlsplit(str(host.url)).netloc
                    else:
                        hostname = str(host.url)
                    try:
                        if socket.getfqdn(hostip) == socket.getfqdn(hostname):
                            return host.username, host.password
                    except socket.error, e:
                        raise Exception("Unresolvable host %s error is %s" % (hostip, e))
    raise KeyError("Please provide the marvin configuration file with credentials to your hosts")

def get_process_status(hostip, port, username, password, linklocalip, process, hypervisor=None):
    """Double hop and returns a process status"""

    #SSH to the machine
    ssh = remoteSSHClient(hostip, port, username, password)
    if str(hypervisor).lower() == 'vmware':
        ssh_command = "ssh -i /var/cloudstack/management/.ssh/id_rsa -ostricthostkeychecking=no "
    else:
        ssh_command = "ssh -i ~/.ssh/id_rsa.cloud -ostricthostkeychecking=no "

    ssh_command = ssh_command +\
                  "-oUserKnownHostsFile=/dev/null -p 3922 %s %s" % (
                      linklocalip,
                      process)

    # Double hop into router
    timeout = 5
    # Ensure the SSH login is successful
    while True:
        res = ssh.execute(ssh_command)

        if res[0] != "Host key verification failed.":
            break
        elif timeout == 0:
            break

        time.sleep(5)
        timeout = timeout - 1
    return res


def isAlmostEqual(first_digit, second_digit, range=0):
    digits_equal_within_range = False

    try:
        if ((first_digit - range) < second_digit < (first_digit + range)):
            digits_equal_within_range = True
    except Exception as e:
        raise e
    return digits_equal_within_range


def xsplit(txt, seps):
    """
    Split a string in `txt` by list of delimiters in `seps`
    @param txt: string to split
    @param seps: list of separators
    @return: list of split units
    """
    default_sep = seps[0]
    for sep in seps[1:]: # we skip seps[0] because that's the default separator
        txt = txt.replace(sep, default_sep)
    return [i.strip() for i in txt.split(default_sep)]

def is_snapshot_on_nfs(apiclient, dbconn, config, zoneid, snapshotid):
    """
    Checks whether a snapshot with id (not UUID) `snapshotid` is present on the nfs storage

    @param apiclient: api client connection
    @param @dbconn:  connection to the cloudstack db
    @param config: marvin configuration file
    @param zoneid: uuid of the zone on which the secondary nfs storage pool is mounted
    @param snapshotid: uuid of the snapshot
    @return: True if snapshot is found, False otherwise
    """

    from base import ImageStore, Snapshot
    secondaryStores = ImageStore.list(apiclient, zoneid=zoneid)

    assert isinstance(secondaryStores, list), "Not a valid response for listImageStores"
    assert len(secondaryStores) != 0, "No image stores found in zone %s" % zoneid

    secondaryStore = secondaryStores[0]

    if str(secondaryStore.providername).lower() != "nfs":
        raise Exception(
            "is_snapshot_on_nfs works only against nfs secondary storage. found %s" % str(secondaryStore.providername))

    qresultset = dbconn.execute(
                        "select id from snapshots where uuid = '%s';" \
                        % str(snapshotid)
                        )
    if len(qresultset) == 0:
        raise Exception(
            "No snapshot found in cloudstack with id %s" % snapshotid)


    snapshotid = qresultset[0][0]
    qresultset = dbconn.execute(
        "select install_path from snapshot_store_ref where snapshot_id='%s' and store_role='Image';" % snapshotid
    )

    assert isinstance(qresultset, list), "Invalid db query response for snapshot %s" % snapshotid
    assert len(qresultset) != 0, "No such snapshot %s found in the cloudstack db" % snapshotid

    snapshotPath = qresultset[0][0]

    nfsurl = secondaryStore.url
    # parse_url = ['nfs:', '', '192.168.100.21', 'export', 'test']
    from urllib2 import urlparse
    parse_url = urlparse.urlsplit(nfsurl, scheme='nfs')
    host, path = parse_url.netloc, parse_url.path

    if not config.mgtSvr:
        raise Exception("Your marvin configuration does not contain mgmt server credentials")
    host, user, passwd = config.mgtSvr[0].mgtSvrIp, config.mgtSvr[0].user, config.mgtSvr[0].passwd

    try:
        ssh_client = remoteSSHClient(
            host,
            22,
            user,
            passwd,
        )
        cmds = [
                "mkdir -p %s /mnt/tmp",
                "mount -t %s %s:%s /mnt/tmp" % (
                    'nfs',
                    host,
                    path,
                    ),
                "test -f %s && echo 'snapshot exists'" % (
                    os.path.join("/mnt/tmp", snapshotPath)
                    ),
            ]

        for c in cmds:
            result = ssh_client.execute(c)

        # Unmount the Sec Storage
        cmds = [
                "cd",
                "umount /mnt/tmp",
            ]
        for c in cmds:
            ssh_client.execute(c)
    except Exception as e:
        raise Exception("SSH failed for management server: %s - %s" %
                      (config[0].mgtSvrIp, e))
    return 'snapshot exists' in result

def is_config_suitable(apiclient, name, value):
    """
    Ensure if the deployment has the expected `value` for the global setting `name'
    @return: true if value is set, else false
    """
    configs = Configuration.list(apiclient, name=name)
    assert(configs is not None and isinstance(configs, list) and len(configs) > 0)
    return configs[0].value == value

def wait_for_cleanup(apiclient, configs=None):
    """Sleeps till the cleanup configs passed"""

    # Configs list consists of the list of global configs
    if not isinstance(configs, list):
        return
    for config in configs:
        configs = Configuration.list(apiclient, name=config, listall=True)
        config_desc = configs[0]
        # Sleep for the config_desc.value time
        time.sleep(int(config_desc.value))
    return
