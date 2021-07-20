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

import os
import requests
import hashlib
import backports.configparser as configparser
from multiprocessing.pool import ThreadPool

CS_MAJOR_VERSION=4.16
CS_MINOR_VERSION=0
templates = {
    "kvm": "https://download.cloudstack.org/systemvm/{0}/systemvmtemplate-{0}.{1}-kvm.qcow2.bz2"
        .format(CS_MAJOR_VERSION, CS_MINOR_VERSION),
    "vmware": "https://download.cloudstack.org/systemvm/{0}/systemvmtemplate-{0}.{1}-vmware.ova"
        .format(CS_MAJOR_VERSION, CS_MINOR_VERSION),
    "xenserver": "https://download.cloudstack.org/systemvm/{0}/systemvmtemplate-{0}.{1}-xen.vhd.bz2"
        .format(CS_MAJOR_VERSION, CS_MINOR_VERSION),
    "hyperv": "https://download.cloudstack.org/systemvm/{0}/systemvmtemplate-{0}.{1}-hyperv.vhd.zip"
        .format(CS_MAJOR_VERSION, CS_MINOR_VERSION),
    "lxc": "https://download.cloudstack.org/systemvm/{0}/systemvmtemplate-{0}.{1}-kvm.qcow2.bz2"
           .format(CS_MAJOR_VERSION, CS_MINOR_VERSION),
    "ovm3": "https://download.cloudstack.org/systemvm/{0}/systemvmtemplate-{0}.{1}-ovm.raw.bz2"
            .format(CS_MAJOR_VERSION, CS_MINOR_VERSION),
}

checksums = {
    "kvm": "07268f267dc4316dc5f86150346bb8d7",
    "vmware": "b356cbbdef67c4eefa8c336328e2b202",
    "xenserver": "71d8adb40baa609997acdc3eae15fbde",
    "hyperv": "0982aa1461800ce1538e0cae07e00770",
    "lxc": "07268f267dc4316dc5f86150346bb8d7",
    "ovm3": "8c643d146c82f92843b8a48c7661f800"
}
destination = os.path.dirname(os.path.abspath(__file__)) + '/templates/'
if not os.path.exists(destination):
    os.makedirs(destination)

metadataFile = destination + 'metadata.ini'


def downloadSystemvmTemplate(url):
    fileName = url.rsplit('/', 1)[1]
    fileName = destination + fileName
    if (os.path.exists(fileName)):
        checksum = hashlib.md5(open(fileName, 'rb').read()).hexdigest()
        fileChecksum = checksums[list(templates.keys())[list(templates.values()).index(url)]]
        if checksum == fileChecksum:
            print('Template ' + url + ' already downloaded')
            return
    try:
        r = requests.get(url, stream=True)
        if r.status_code == 200:
            with open(fileName, 'wb') as f:
                for chunk in r:
                    f.write(chunk)
    except Exception as e:
        print(e)

    return fileName

def downloadTemplates():
    results = ThreadPool(4).imap_unordered(downloadSystemvmTemplate, list(templates.values()))
    for path in results:
        print(path)

def createMetadataFile():
    templateFiles = [f for f in os.listdir(destination) if os.path.isfile(os.path.join(destination, f))]
    # print(templates)
    write_config = configparser.ConfigParser()
    for template in templateFiles:
        value = filter(lambda temp : template in temp, templates.values())
        if len(value) == 0:
            continue
        hypervisor = list(templates.keys())[list(templates.values()).index(value[0])]
        write_config.add_section(hypervisor)
        write_config.set(hypervisor, "templateName", "systemvm-{0}-{1}.{2}".format(hypervisor, CS_MAJOR_VERSION, CS_MINOR_VERSION))
        write_config.set(hypervisor, "checksum", checksums[hypervisor])

    cfgfile = open(metadataFile, 'w')
    write_config.write(cfgfile)
    cfgfile.close()

downloadTemplates()
createMetadataFile()