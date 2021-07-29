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
from lxml import etree
import backports.configparser as configparser

ns = {"ns" : "http://maven.apache.org/POM/4.0.0"}
doc=etree.parse("./pom.xml")

def getCloudstackVersion():
    table = doc.xpath('.//ns:parent', namespaces=ns)
    version=""
    try:
        for df in table:
            versionTag = df.find('.//ns:version', ns)
            if versionTag is not None:
                version = versionTag.text
                break
        splitVersion=version.split("-SNAPSHOT")[0].split('.')
        major='.'.join(splitVersion[0:2])
        minor=splitVersion[2]
        return major,minor

    except Exception as e:
        raise Exception("Failed to fetch cloudstack version")

def getGenericName(hypervisor):
    if hypervisor.lower() == "ovm3":
        return "ovm"
    if hypervisor.lower() == "lxc":
        return "kvm"
    if hypervisor.lower() == "xenserver":
        return "xen"
    else:
        return hypervisor

def fetchChecksum(checksumData, hypervisor):
    for line in checksumData:
        hypervisor = getGenericName(hypervisor)
        if hypervisor in line:
            print(type(line.split(" ")[0]))
            return str(line.split(" ")[0])

def createMetadataFile():
    write_config = configparser.ConfigParser()
    with open(sourceFile, "r") as md5sumFile:
        checksumData = md5sumFile.readlines()

    for hypervisor in templates.keys():
        write_config.add_section(hypervisor)
        write_config.set(hypervisor, "templatename", "systemvm-{0}-{1}.{2}".format(hypervisor, CS_MAJOR_VERSION, CS_MINOR_VERSION))
        checksum=fetchChecksum(checksumData, hypervisor)
        write_config.set(hypervisor, "checksum", str(checksum))
        downloadUrl=templates.get(hypervisor).format(CS_MAJOR_VERSION, CS_MINOR_VERSION)
        write_config.set(hypervisor, "downloadurl", str(downloadUrl))
        write_config.set(hypervisor, "filename", str(downloadUrl.split('/')[-1]))

    cfgfile = open(metadataFile, 'w')
    write_config.write(cfgfile)
    cfgfile.close()


CS_MAJOR_VERSION,CS_MINOR_VERSION=getCloudstackVersion()
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

parentPath = os.path.dirname(os.path.abspath(__file__)) + '/dist/systemvm-templates/'
if not os.path.exists(parentPath):
    os.makedirs(parentPath)
metadataFile =  parentPath + 'metadata.ini'
sourceFile = parentPath + 'md5sum.txt'

createMetadataFile()


