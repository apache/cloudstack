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
from db import Database
from configFileOps import configFileOps
from serviceConfig import serviceCfgBase
from cloudException import CloudRuntimeException, CloudInternalException
from utilities import bash
import os

class cloudManagementConfig(serviceCfgBase):
    def __init__(self, syscfg):
        super(cloudManagementConfig, self).__init__(syscfg)
        self.serviceName = "CloudStack Management Server"

    def config(self):
        def checkHostName():
           ret = bash("hostname --fqdn")
           if not ret.isSuccess():
               raise CloudInternalException("Cannot get hostname, 'hostname --fqdn failed'")

        if self.syscfg.env.svrMode == "mycloud":
            cfo = configFileOps("/usr/share/cloudstack-management/conf/environment.properties", self)
            cfo.addEntry("cloud-stack-components-specification", "components-cloudzones.xml")
            cfo.save()

            cfo = configFileOps("/usr/share/cloudstack-management/conf/db.properties", self)
            dbHost = cfo.getEntry("db.cloud.host")
            dbPort = cfo.getEntry("db.cloud.port")
            dbUser = cfo.getEntry("db.cloud.username")
            dbPass = cfo.getEntry("db.cloud.password")
            if dbPass.strip() == "":
                dbPass = None
            dbName = cfo.getEntry("db.cloud.name")
            db = Database(dbUser, dbPass, dbHost, dbPort, dbName)

            try:
                db.testConnection()
            except CloudRuntimeException, e:
                raise e
            except:
                raise CloudInternalException("Failed to connect to Mysql server")

            try:
                statement = """ UPDATE configuration SET value='%s' WHERE name='%s'"""

                db.execute(statement%('true','use.local.storage'))
                db.execute(statement%('20','max.template.iso.size'))

                statement = """ UPDATE vm_template SET url='%s',checksum='%s' WHERE id='%s' """
                db.execute(statement%('https://rightscale-cloudstack.s3.amazonaws.com/kvm/RightImage_CentOS_5.4_x64_v5.6.28.qcow2.bz2', '90fcd2fa4d3177e31ff296cecb9933b7', '4'))

                statement="""UPDATE disk_offering set use_local_storage=1"""
                db.execute(statement)
            except:
                raise e

            #add DNAT 443 to 8250
            if not bash("iptables-save |grep PREROUTING | grep 8250").isSuccess():
                bash("iptables -A PREROUTING -t nat -p tcp --dport 443 -j REDIRECT --to-port 8250 ")

            #generate keystore
            keyPath = "/var/cloudstack/management/web.keystore"
            if not os.path.exists(keyPath):
                cmd = bash("keytool -genkey -keystore %s -storepass \"cloud.com\" -keypass \"cloud.com\" -validity 3650 -dname cn=\"Cloudstack User\",ou=\"mycloud.cloud.com\",o=\"mycloud.cloud.com\",c=\"Unknown\""%keyPath)

                if not cmd.isSuccess():
                    raise CloudInternalException(cmd.getErrMsg())
            if not self.syscfg.env.svrConf == "Tomcat7":
                cfo = configFileOps("/etc/cloudstack/management/tomcat6.conf", self)
                cfo.add_lines("JAVA_OPTS+=\" -Djavax.net.ssl.trustStore=%s \""%keyPath)
        elif self.syscfg.env.svrMode == "HttpsServer":
            if self.syscfg.env.svrConf == "Tomcat7":
                if not os.path.exists("/etc/cloudstack/management/server7-ssl.xml"):
                    raise CloudRuntimeException("Cannot find /etc/cloudstack/management/server7-ssl.xml, https enable failed")
                if os.path.exists("/etc/cloudstack/management/server.xml"):
                    bash("rm -f /etc/cloudstack/management/server.xml")
                bash("ln -s /etc/cloudstack/management/server7-ssl.xml /etc/cloudstack/management/server.xml")
            else:
                if not os.path.exists("/etc/cloudstack/management/server-ssl.xml") or not os.path.exists("/etc/cloudstack/management/tomcat6-ssl.conf"):
                    raise CloudRuntimeException("Cannot find /etc/cloudstack/management/server-ssl.xml or /etc/cloudstack/management/tomcat6-ssl.conf, https enable failed")
                if os.path.exists("/etc/cloudstack/management/server.xml"):
                    bash("rm -f /etc/cloudstack/management/server.xml")
                if os.path.exists("/etc/cloudstack/management/tomcat6.conf"):
                    bash("rm -f /etc/cloudstack/management/tomcat6.conf")
                bash("ln -s /etc/cloudstack/management/server-ssl.xml /etc/cloudstack/management/server.xml")
                bash("ln -s /etc/cloudstack/management/tomcat6-ssl.conf /etc/cloudstack/management/tomcat6.conf")
            if not bash("iptables-save |grep PREROUTING | grep 6443").isSuccess():
                bash("iptables -A PREROUTING -t nat -p tcp --dport 443 -j REDIRECT --to-port 6443")
        else:
            if self.syscfg.env.svrConf == "Tomcat7":
                if not os.path.exists("/etc/cloudstack/management/server7-nonssl.xml"):
                    raise CloudRuntimeException("Cannot find /etc/cloudstack/management/server7-nonssl.xml, https enable failed")
                if os.path.exists("/etc/cloudstack/management/server.xml"):
                    bash("rm -f /etc/cloudstack/management/server.xml")
                bash("ln -s /etc/cloudstack/management/server7-nonssl.xml /etc/cloudstack/management/server.xml")
            else:
                if not os.path.exists("/etc/cloudstack/management/server-nonssl.xml") or not os.path.exists("/etc/cloudstack/management/tomcat6-nonssl.conf"):
                    raise CloudRuntimeException("Cannot find /etc/cloudstack/management/server-nonssl.xml or /etc/cloudstack/management/tomcat6-nonssl.conf, https enable failed")
                if os.path.exists("/etc/cloudstack/management/server.xml"):
                    bash("rm -f /etc/cloudstack/management/server.xml")
                if os.path.exists("/etc/cloudstack/management/tomcat6.conf"):
                    bash("rm -f /etc/cloudstack/management/tomcat6.conf")
                bash("ln -s /etc/cloudstack/management/server-nonssl.xml /etc/cloudstack/management/server.xml")
                bash("ln -s /etc/cloudstack/management/tomcat6-nonssl.conf /etc/cloudstack/management/tomcat6.conf")
        bash("touch /var/run/cloudstack-management.pid")
        bash("chown cloud.cloud /var/run/cloudstack-management.pid")
        #distro like sl 6.1 needs this folder, or tomcat6 failed to start
        checkHostName()
        bash("mkdir /var/log/cloudstack-management/")
        bash("chown cloud:cloud -R /var/lib/cloudstack/")
        bash("chmod +x -R /usr/share/cloudstack-management/webapps/client/WEB-INF/classes/scripts/")
        #set max process per account is unlimited
        if os.path.exists("/etc/security/limits.conf"):
            cfo = configFileOps("/etc/security/limits.conf")
            cfo.add_lines("cloud soft nproc -1\n")
            cfo.add_lines("cloud hard nproc -1\n")
            cfo.save()

        try:
            if self.syscfg.env.svrConf == "Tomcat7":
                self.syscfg.svo.disableService("tomcat")
            else:
                self.syscfg.svo.disableService("tomcat6")
        except:
            pass

        self.syscfg.svo.stopService("cloudstack-management")

        if self.syscfg.env.noStart == False:
            if self.syscfg.svo.enableService("cloudstack-management"):
                return True
            else:
                raise CloudRuntimeException("Failed to configure %s, please see the /var/log/cloudstack/management/setupManagement.log for detail"%self.serviceName)
        else:
            print "Configured successfully, but not starting management server."
            return True
