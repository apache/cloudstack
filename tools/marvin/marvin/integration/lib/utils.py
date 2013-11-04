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
"""Utilities functions
"""

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
from marvin.codes import *


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


def cleanup_resources(api_client, resources):
    """Delete resources"""
    for obj in resources:
        obj.delete(api_client)


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
    testClientLogger = logging.getLogger("testClient")
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

    if len(qresultset) == 0:
        #Snapshot does not exist
        return False

    snapshotPath = qresultset[0][0]

    nfsurl = secondaryStore.url
    from urllib2 import urlparse
    parse_url = urlparse.urlsplit(nfsurl, scheme='nfs')
    host, path = parse_url.netloc, parse_url.path

    if not config.mgtSvr:
        raise Exception("Your marvin configuration does not contain mgmt server credentials")
    mgtSvr, user, passwd = config.mgtSvr[0].mgtSvrIp, config.mgtSvr[0].user, config.mgtSvr[0].passwd

    try:
        ssh_client = remoteSSHClient(
            mgtSvr,
            22,
            user,
            passwd
        )
        cmds = [
                "mkdir -p %s /mnt/tmp",
                "mount -t %s %s%s /mnt/tmp" % (
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
                      (config.mgtSvr[0].mgtSvrIp, e))
    return 'snapshot exists' in result


def validateList(inp):
        '''
        @name: validateList
        @Description: 1. A utility function to validate
                 whether the input passed is a list
              2. The list is empty or not
              3. If it is list and not empty, return PASS and first element
              4. If not reason for FAIL
        @Input: Input to be validated
        @output: List, containing [ Result,FirstElement,Reason ]
                 Ist Argument('Result') : FAIL : If it is not a list
                                          If it is list but empty
                                         PASS : If it is list and not empty
                 IInd Argument('FirstElement'): If it is list and not empty,
                                           then first element
                                            in it, default to None
                 IIIrd Argument( 'Reason' ):  Reason for failure ( FAIL ),
                                              default to None.
                                              INVALID_INPUT
                                              EMPTY_LIST
        '''
        ret = [FAIL, None, None]
        if inp is None:
            ret[2] = INVALID_INPUT
            return ret
        if not isinstance(inp, list):
            ret[2] = INVALID_INPUT
            return ret
        if len(inp) == 0:
            ret[2] = EMPTY_LIST
            return ret
        return [PASS, inp[0], None]

def verifyElementInList(inp, toverify, pos = 0):
       '''
       @name: verifyElementInList
       @Description: 
              1. A utility function to validate
                 whether the input passed is a list.
                 The list is empty or not.
                 If it is list and not empty, verify
                 whether a given element is there in that list or not
                 at a given pos  
       @Input: 
              I  : Input to be verified whether its a list or not
             II  : Element to verify whether it exists in the list 
             III : Position in the list at which the input element to verify
                    default to 0
       @output: List, containing [ Result,Reason ]
                Ist Argument('Result') : FAIL : If it is not a list
                                          If it is list but empty
                                          PASS : If it is list and not empty
                                              and matching element was found
                IIrd Argument( 'Reason' ): Reason for failure ( FAIL ),
                                            default to None.
                                            INVALID_INPUT
                                            EMPTY_LIST
                                            MATCH_NOT_FOUND
       '''
       if toverify is None or toverify == '' \
           or pos is None or pos < -1 or pos == '':
           return [FAIL, INVALID_INPUT]
       out = validateList(inp)
       if out[0] == FAIL:
           return [FAIL, out[2]]
       if out[0] == PASS:
           if len(inp) > pos and inp[pos] == toverify:
               return [PASS, None]
           else:
               return [FAIL, MATCH_NOT_FOUND]


