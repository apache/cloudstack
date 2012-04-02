# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#

"""Utilities functions
"""

import time
import remoteSSHClient
from cloudstackAPI import *
import cloudstackConnection
#from cloudstackConnection import cloudConnection
import configGenerator
import logging
import string
import random
import imaplib
import email
import datetime

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

def random_gen(size=6, chars=string.ascii_uppercase + string.digits):
    """Generate Random Strings of variable length"""
    return ''.join(random.choice(chars) for x in range(size))

def cleanup_resources(api_client, resources):
    """Delete resources"""
    for obj in resources:
        obj.delete(api_client)

def is_server_ssh_ready(ipaddress, port, username, password, retries=50):
    """Return ssh handle else wait till sshd is running"""
    loop_cnt = retries
    while True:
        try:
            ssh = remoteSSHClient.remoteSSHClient(
                                            ipaddress,
                                            port,
                                            username,
                                            password
                                            )
        except Exception as e:
            if loop_cnt == 0:
                raise e
            loop_cnt = loop_cnt - 1
            time.sleep(30)
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
    config = configGenerator.get_setup_config(config_file)
    mgt = config.mgtSvr[0]
    testClientLogger = logging.getLogger("testClient")
    asyncTimeout = 3600
    return cloudstackAPIClient.CloudStackAPIClient(
            cloudstackConnection.cloudConnection(
                                                mgt.mgtSvrIp,
                                                mgt.port,
                                                mgt.apiKey,
                                                mgt.securityKey,
                                                asyncTimeout,
                                                testClientLogger
                                                )
                                            )

def get_process_status(hostip, port, username, password, linklocalip, process):
    """Double hop and returns a process status"""

    #SSH to the machine
    ssh = remoteSSHClient.remoteSSHClient(
                                          hostip,
                                          port,
                                          username,
                                          password
                            )
    ssh_command = "ssh -i ~/.ssh/id_rsa.cloud -ostricthostkeychecking=no "
    ssh_command = ssh_command + "-oUserKnownHostsFile=/dev/null -p 3922 %s %s" \
                        % (linklocalip, process)

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