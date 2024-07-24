#!/usr/bin/python
# -*- coding: utf-8 -*-
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

import argparse
import sys
import urllib
import uuid
import subprocess
import os
import shutil
import gzip
import zipfile
import bz2

class InstallSysTemplate(object):
  parser = None
  mountpoint = None
  args = None
  hypervisor = None
  systemvmtemplatepath = None
  systemvmtemplateurl = None
  managementsecretkey = None
  forcecleanup = None
  databasehostname = None
  databaseusername = None
  databaseuserpassword = None
  templatesuffix = None
  template = None
  fileextension = None
  templateName = None
  destDir = None
  fileSize = None
  dictionary = None

  def __init__(self):
    self.dictionary = dict(xenserver=('XenServer', 'vhd'), kvm=('KVM', 'qcow2'), vmware=('VMware', 'ova'), lxc=('LXC', 'qcow2'), hyperv=('Hyperv', 'vhd'))

  def parseOptions(self):
    self.parser = argparse.ArgumentParser(prog="System Template Installer")
    self.parser.add_argument("-m", "--mount-point", action="store", dest="mountpoint", help="Secondary Storage Mount Point where to install the template.", required="true")
    self.parser.add_argument("-H", "--hypervisor", action="store", dest="hypervisor", help="The Hypervisor name for which template need to be installed", required="true", choices=['kvm','xenserver','vmware','lxc','hyperv'])
    group = self.parser.add_mutually_exclusive_group(required=True)
    group.add_argument("-f", "--system-vm-template", action="store", dest="systemvmtemplatepath", help="The local system vm template file path")
    group.add_argument("-u", "--system-vm-template-url", action="store", dest="systemvmtemplateurl", help="Url to download system vm template")
    self.parser.add_argument("-s", "--management-secret-key", action="store", dest="managementsecretkey", help="mgmt server secret key, if you specified any when running cloudstack-setup-database, default is password", default="password")
    self.parser.add_argument("-F", "--force-clean-up", action="store_true", dest="forcecleanup", help="clean up system templates of specified hypervisor", default="false")
    self.parser.add_argument("-d", "--database-host-name", action="store", dest="databasehostname", help="Database server hostname or ip, e.g localhost", default="localhost", required="true")
    self.parser.add_argument("-r", "--database-user-name", action="store", dest="databaseusername", help="Database user name, e.g root", default="root", required="true")
    self.parser.add_argument("-p", "--database-user-password", nargs='?', action="store", dest="databaseuserpassword", help="Database password. Followed by nothing if the password is empty", default="", required="true")
    self.parser.add_argument("-e", "--template-suffix", action="store", dest="templatesuffix", help="Template suffix, e.g vhd, ova, qcow2",default="vhd")
    self.parser.add_argument("-t", "--file-extension", action="store", dest="fileextension", help="The template file extension", default="", required="true", choices=['bz2','gz','zip'])

    self.args = self.parser.parse_args()

  def populateOptions(self):
    self.mountpoint = self.args.mountpoint
    self.hypervisor = self.args.hypervisor
    self.fileextension = self.args.fileextension
    if self.args.systemvmtemplatepath:
      self.systemvmtemplatepath = self.args.systemvmtemplatepath
    if self.args.systemvmtemplateurl:
      self.systemvmtemplateurl = self.args.systemvmtemplateurl
    if self.args.managementsecretkey:
      self.managementsecretkey = self.args.managementsecretkey
    if self.args.forcecleanup:
      self.forcecleanup = self.args.forcecleanup
    if self.args.databasehostname:
      self.databasehostname = self.args.databasehostname
    if self.args.databaseusername:
      self.databaseusername = self.args.databaseusername
    if self.args.databaseuserpassword:
      self.databaseuserpassword = self.args.databaseuserpassword
    else:
      self.databaseuserpassword = ""
    if self.args.templatesuffix:
      self.templatesuffix = self.args.templatesuffix
    print 'Password for DB: %s'%self.databaseuserpassword

  def errorAndExit(self, msg):
    err = '''\n\nWe apologize for below error:
***************************************************************
%s
***************************************************************
Please run:

    cloud-install-sys-tmplt -h

for full help
''' % msg
    sys.stderr.write(err)
    sys.stderr.flush()
    sys.exit(1)

  def runCmd(self, cmds):
    process = subprocess.Popen(' '.join(cmds), shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    stdout, stderr = process.communicate()
    print(stdout)
    if process.returncode != 0:
        raise Exception(stderr)
    return stdout

  def runMysql(self, query):
    try:
      print 'Running Query: %s' % query
      mysqlCmds = ['mysql', '--user=%s'%self.databaseusername, '--host=%s'%self.databasehostname, '--password=%s'%self.databaseuserpassword, '--skip-column-names', '-U', 'cloud', '-e "%s"'%query]
      templateId = self.runCmd(mysqlCmds)
      print 'TemplateId is : %s' % templateId
    except Exception, e:
      err = '''Encountering an error when executing mysql script\n%s''' % str(e)
      self.errorAndExit(err)
    return templateId

  def fetchTemplateDetails(self):
    mysqlQuery = "select max(id) from cloud.vm_template where type = 'SYSTEM' and hypervisor_type = '%s' and removed is null"
    ht = None
    hypervisorInfo = self.dictionary[self.hypervisor]
    ht = hypervisorInfo[0]
    self.templatesuffix = hypervisorInfo[1]

    self.template = int(self.runMysql(mysqlQuery%ht))

  def downloadTemplate(self):
    self.systemvmtemplatepath = self.templateName + "." + self.fileextension
    print 'Downloading template from %s To %s' % (self.systemvmtemplateurl, self.systemvmtemplatepath)
    try:
      templateFileDownloadUrl = urllib.urlretrieve(self.systemvmtemplateurl, self.systemvmtemplatepath, reporthook=self.report)
    except Exception:
      self.errorAndExit("Failed to download template file from %s" % self.systemvmtemplateurl)

  def report(tmp, blocknr, blocksize, size):
    current = blocknr*blocksize
    sys.stdout.write("\rDownloading completed: {0:.2f}%".format(100.0*current/size))

  def installTemplate(self):
    destDir = self.mountpoint + os.sep + "template" + os.sep + "tmpl" + os.sep + "1" + os.sep + str(self.template)
    self.destDir = destDir
    print 'The desination Directory is : %s' % destDir
    try:
      if self.forcecleanup:
        if os.path.exists(destDir):
          shutil.rmtree(destDir)
      if not os.path.exists(destDir):
        os.makedirs(destDir)
    except Exception, e:
      self.errorAndExit('Failed to create directories on the mounted path.. %s' % str (e))
    print 'Installing Template to : %s' % destDir
    tmpFile = self.templateName + "." + "tmp"
    self.uncompressFile(tmpFile)
    print 'Moving the decompressed file to destination directory %s... which could take a long time, please wait' % destDir
    shutil.move(tmpFile, destDir + os.sep + self.templateName)

  def uncompressFile(self, fileName):
    print 'Uncompressing the file %s... which could take a long time, please wait' % self.systemvmtemplatepath
    if self.fileextension == 'gz':
      compressedFile = gzip.GzipFile(self.systemvmtemplatepath, 'rb')
      decompressedData = compressedFile.read()
      compressedFile.close()
      decompressedFile = file(fileName, 'wb')
      decompressedFile.write(decompressedData)
      decompressedFile.close()
    elif self.fileextension == 'bz2':
      compressedFile = bz2.BZ2File(self.systemvmtemplatepath)
      decompressedData = compressedFile.read()
      compressedFile.close()
      decompressedFile = file(fileName, 'wb')
      decompressedFile.write(decompressedData)
      decompressedFile.close()
      print ''
    elif self.fileextension == 'zip':
      zippedFile = zipfile.ZipFile(self.systemvmtemplatepath, 'r')
      zippedFiles = zippedFile.namelist()
      compressedFile = zippedFiles[0]
      decompressedData = zippedFile.read(compressedFile)
      decompressedFile = file(fileName, 'wb')
      decompressedFile.write(decompressedData)
      decompressedFile.close()
      zippedFile.close()
      print ''
    else:
      self.errorAndExit('Not supported file type %s to decompress' % self.fileextension)
    self.fileSize = os.path.getsize(fileName)

  def writeProperties(self):
    propertiesFile = file(self.destDir + os.sep + 'template.properties', 'wb')
    propertiesFile.write('filename=%s\n'%self.templateName)
    propertiesFile.write('description=SystemVM Template\n')
    propertiesFile.write('checksum=\n')
    propertiesFile.write('hvm=false\n')
    propertiesFile.write('size=%s\n'%str(self.fileSize))
    propertiesFile.write('%s=true\n'%self.templatesuffix)
    propertiesFile.write('id=%s\n'%str(self.template))
    propertiesFile.write('public=true\n')
    propertiesFile.write('%s.filename=%s\n'%(self.templatesuffix, self.templateName))
    propertiesFile.write('uniquename=routing-%s\n'%str(self.template))
    propertiesFile.write('%s.virtualsize=%s\n'%(self.templatesuffix, str(self.fileSize)))
    propertiesFile.write('virtualsize=%s\n'%str(self.fileSize))
    propertiesFile.write('%s.size=%s'%(self.templatesuffix, str(self.fileSize)))

    propertiesFile.close()

  def run(self):
    try:
      self.parseOptions()
      self.populateOptions()
      self.fetchTemplateDetails()
      randomUUID = uuid.uuid1()
      self.templateName = str(randomUUID) + "." + self.templatesuffix
      if self.args.systemvmtemplateurl:
        self.downloadTemplate()
      self.installTemplate()
      self.writeProperties()
    finally:
      print ''
    print ''
    print "CloudStack has successfully installed system template"
    print ''

if __name__ == "__main__":
   o = InstallSysTemplate()
   o.run()
