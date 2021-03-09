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
import re
import time
import logging
import string
import random
import imaplib
import email
import socket
import urllib.parse
import datetime
from marvin.cloudstackAPI import cloudstackAPIClient, listHosts, listRouters
from platform import system
from marvin.cloudstackException import GetDetailExceptionInfo
from marvin.sshClient import SshClient
from marvin.codes import (
                          SUCCESS,
                          FAIL,
                          PASS,
                          MATCH_NOT_FOUND,
                          INVALID_INPUT,
                          EMPTY_LIST,
                          FAILED)

def _configure_ssh_credentials(hypervisor):
    ssh_command = "ssh -i ~/.ssh/id_rsa.cloud -ostricthostkeychecking=no "
    
    if (str(hypervisor).lower() == 'vmware'
        or str(hypervisor).lower() == 'hyperv'):
        ssh_command = "ssh -i /var/cloudstack/management/.ssh/id_rsa -ostricthostkeychecking=no "

    return ssh_command


def _configure_timeout(hypervisor):
    timeout = 5

    # Increase hop into router
    if str(hypervisor).lower() == 'hyperv':
        timeout = 12

    return timeout


def _execute_ssh_command(hostip, port, username, password, ssh_command, timeout=5):
    #SSH to the machine
    ssh = SshClient(hostip, port, username, password)
    # Ensure the SSH login is successful
    while True:
        res = ssh.execute(ssh_command)
        if len(res) == 0:
            return res
        elif "Connection refused".lower() in res[0].lower():
            pass
        elif res[0] != "Host key verification failed.":
            break
        elif timeout == 0:
            break

        time.sleep(5)
        timeout = timeout - 1
    return res

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


def is_server_ssh_ready(ipaddress, port, username, password, retries=20, retryinterv=30, timeout=10.0, keyPairFileLocation=None):
    '''
    @Name: is_server_ssh_ready
    @Input: timeout: tcp connection timeout flag,
            others information need to be added
    @Output:object for SshClient
    Name of the function is little misnomer and is not
              verifying anything as such mentioned
    '''

    try:
        ssh = SshClient(
            host=ipaddress,
            port=port,
            user=username,
            passwd=password,
            keyPairFiles=keyPairFileLocation,
            retries=retries,
            delay=retryinterv,
            timeout=timeout)
    except Exception as e:
        raise Exception("SSH connection has Failed. Waited %ss. Error is %s" % (retries * retryinterv, str(e)))
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
            mgt,
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
                        hostname = urllib.parse.urlsplit(str(host.url)).netloc
                    else:
                        hostname = str(host.url)
                    try:
                        if socket.getfqdn(hostip) == socket.getfqdn(hostname):
                            return host.username, host.password
                    except socket.error as e:
                        raise Exception("Unresolvable host %s error is %s" % (hostip, e))
    raise KeyError("Please provide the marvin configuration file with credentials to your hosts")

def execute_command_in_host(hostip, port, username, password, command, hypervisor=None):
    timeout = _configure_timeout(hypervisor)
    result = _execute_ssh_command(hostip, port, username, password, command)
    return result

def get_process_status(hostip, port, username, password, linklocalip, command, hypervisor=None):
    """Double hop and returns a command execution result"""

    ssh_command = _configure_ssh_credentials(hypervisor)

    ssh_command = ssh_command +\
                  "-oUserKnownHostsFile=/dev/null -p 3922 %s %s" % (
                      linklocalip,
                      command)
    timeout = _configure_timeout(hypervisor)

    result = _execute_ssh_command(hostip, port, username, password, ssh_command)
    return result


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

def get_hypervisor_type(apiclient):

    """Return the hypervisor type of the hosts in setup"""

    cmd = listHosts.listHostsCmd()
    cmd.type = 'Routing'
    cmd.listall = True
    hosts = apiclient.listHosts(cmd)
    hosts_list_validation_result = validateList(hosts)
    assert hosts_list_validation_result[0] == PASS, "host list validation failed"
    return hosts_list_validation_result[1].hypervisor

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
    # snapshot extension to be appended to the snapshot path obtained from db
    snapshot_extensions = {"vmware": ".ovf",
                            "kvm": "",
                            "xenserver": "",
                            "simulator":""}

    qresultset = dbconn.execute(
                        "select id from snapshots where uuid = '%s';" \
                        % str(snapshotid)
                        )
    if len(qresultset) == 0:
        raise Exception(
            "No snapshot found in cloudstack with id %s" % snapshotid)


    snapshotid = qresultset[0][0]
    qresultset = dbconn.execute(
        "select install_path,store_id from snapshot_store_ref where snapshot_id='%s' and store_role='Image';" % snapshotid
    )

    assert isinstance(qresultset, list), "Invalid db query response for snapshot %s" % snapshotid

    if len(qresultset) == 0:
        #Snapshot does not exist
        return False

    from .base import ImageStore
    #pass store_id to get the exact storage pool where snapshot is stored
    secondaryStores = ImageStore.list(apiclient, zoneid=zoneid, id=int(qresultset[0][1]))

    assert isinstance(secondaryStores, list), "Not a valid response for listImageStores"
    assert len(secondaryStores) != 0, "No image stores found in zone %s" % zoneid

    secondaryStore = secondaryStores[0]

    if str(secondaryStore.providername).lower() != "nfs":
        raise Exception(
            "is_snapshot_on_nfs works only against nfs secondary storage. found %s" % str(secondaryStore.providername))

    hypervisor = get_hypervisor_type(apiclient)
    # append snapshot extension based on hypervisor, to the snapshot path
    snapshotPath = str(qresultset[0][0]) + snapshot_extensions[str(hypervisor).lower()]

    nfsurl = secondaryStore.url
    parse_url = urllib.parse.urlsplit(nfsurl, scheme='nfs')
    host, path = str(parse_url.netloc), str(parse_url.path)

    if not config.mgtSvr:
        raise Exception("Your marvin configuration does not contain mgmt server credentials")
    mgtSvr, user, passwd = config.mgtSvr[0].mgtSvrIp, config.mgtSvr[0].user, config.mgtSvr[0].passwd

    try:
        ssh_client = SshClient(
            mgtSvr,
            22,
            user,
            passwd
        )

        pathSeparator = "" #used to form host:dir format
        if not host.endswith(':'):
            pathSeparator= ":"

        cmds = [

            "mkdir -p %s /mnt/tmp",
            "mount -t %s %s%s%s /mnt/tmp" % (
                'nfs',
                host,
                pathSeparator,
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
    """
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
    """
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

def verifyElementInList(inp, toverify, responsevar=None,  pos=0):
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
             I   : Input to be verified whether its a list or not
             II  : Element to verify whether it exists in the list 
             III : variable name in response object to verify 
                   default to None, if None, we will verify for the complete 
                   first element EX: state of response object object
             IV  : Position in the list at which the input element to verify
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
    if len(inp) > pos:
        if responsevar is None:
                if inp[pos] == toverify:
                    return [PASS, None]
        else:
                if responsevar in inp[pos].__dict__ and getattr(inp[pos], responsevar) == toverify:
                    return [PASS, None]
                else:
                    return [FAIL, MATCH_NOT_FOUND]
    else:
        return [FAIL, MATCH_NOT_FOUND]

def checkVolumeSize(ssh_handle=None,
                    volume_name="/dev/sda",
                    cmd_inp="/sbin/fdisk -l | grep Disk",
                    size_to_verify=0):
    '''
    @Name : getDiskUsage
    @Desc : provides facility to verify the volume size against the size to verify
    @Input: 1. ssh_handle : machine against which to execute the disk size cmd
            2. volume_name : The name of the volume against which to verify the size
            3. cmd_inp : Input command used to veify the size
            4. size_to_verify: size against which to compare.
    @Output: Returns FAILED in case of an issue, else SUCCESS
    '''
    try:
        if ssh_handle is None or cmd_inp is None or volume_name is None:
            return INVALID_INPUT

        cmd = cmd_inp
        '''
        Retrieve the cmd output
        '''
        if system().lower() != "windows":
            fdisk_output = ssh_handle.runCommand(cmd_inp)
            if fdisk_output["status"] != SUCCESS:
                return FAILED
            for line in fdisk_output["stdout"]:
                if volume_name in line:
                    # Get the bytes from the output
                    # Disk /dev/xvdb: 1 GiB, 1073741824 bytes, 2097152 sectors
                    m = re.match('.*?(\d+) bytes.*', line)
                    if m and str(m.group(1)) == str(size_to_verify):
                        return [SUCCESS,str(m.group(1))]
            return [FAILED,"Volume Not Found"]
    except Exception as e:
        print("\n Exception Occurred under getDiskUsage: " \
              "%s" %GetDetailExceptionInfo(e))
        return [FAILED,GetDetailExceptionInfo(e)]

        
def verifyRouterState(apiclient, routerid, allowedstates):
    """List the router and verify that its state is in allowed states
    @output: List, containing [Result, Reason]
             Ist Argument ('Result'): FAIL: If router state is not
                                                in allowed states
                                          PASS: If router state is in
                                                allowed states"""

    try:
        cmd = listRouters.listRoutersCmd()
        cmd.id = routerid
        cmd.listall = True
        routers = apiclient.listRouters(cmd)
    except Exception as e:
        return [FAIL, e]
    listvalidationresult = validateList(routers)
    if listvalidationresult[0] == FAIL:
        return [FAIL, listvalidationresult[2]]
    if routers[0].state.lower() not in allowedstates:
        return [FAIL, "state of the router should be in %s but is %s" %
            (allowedstates, routers[0].state)]
    return [PASS, None]


def wait_until(retry_interval=2, no_of_times=2, callback=None, *callback_args):
    """ Utility method to try out the callback method at most no_of_times with a interval of retry_interval,
        Will return immediately if callback returns True. The callback method should be written to return a list of values first being a boolean """

    if callback is None:
        raise ("Bad value for callback method !")

    wait_result = False 
    for i in range(0,no_of_times):
        time.sleep(retry_interval)
        wait_result, return_val = callback(*callback_args)
        if not(isinstance(wait_result, bool)):
            raise ("Bad parameter returned from callback !")
        if wait_result :
            break

    return wait_result, return_val

