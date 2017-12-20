#!/usr/bin/env bash
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

help() {
   printf " -c use customized key/cert\n"
   printf " -k path of private key\n"
   printf " -p path of certificate of public key\n"
   printf " -t path of certificate chain\n"
   printf " -u path of root ca certificate \n"
}


config_apache2_conf() {
  local ip=$1
  local srvr=$2
  sed -i  's/ssl-cert-snakeoil.key/cert_apache.key/' /etc/apache2/sites-enabled/vhost*
  sed -i  's/ssl-cert-snakeoil.pem/cert_apache.crt/' /etc/apache2/sites-enabled/vhost*
  if [ -f /etc/ssl/certs/cert_apache_chain.crt ]
  then
    sed -i -e "s/#SSLCertificateChainFile.*/SSLCertificateChainFile \/etc\/ssl\/certs\/cert_apache_chain.crt/" /etc/apache2/sites-enabled/vhost*
  fi
}

copy_certs_apache2() {
  local certdir=$(dirname $0)/certs
  local mydir=$(dirname $0)
  if [ -f $customPrivKey ] &&  [ -f $customPrivCert ] ; then
      cp $customPrivKey /etc/ssl/private/cert_apache.key   &&  cp $customPrivCert /etc/ssl/certs/cert_apache.crt
  fi
  if [ ! -z "$customCertChain" ] && [ -f "$customCertChain" ] ; then
     cp $customCertChain /etc/ssl/certs/cert_apache_chain.crt
  fi
  return 0
}

cflag=
cpkflag=
cpcflag=
cccflag=
customPrivKey=$(dirname $0)/certs/realhostip.key
customPrivCert=$(dirname $0)/certs/realhostip.crt
customCertChain=
customCACert=
publicIp=
hostName=
keyStore=$(dirname $0)/certs/realhostip.keystore
defaultJavaKeyStoreFile=/etc/ssl/certs/java/cacerts
defaultJavaKeyStorePass="changeit"
aliasName="CPVMCertificate"
storepass="vmops.com"
while getopts 'i:h:k:p:t:u:c' OPTION
do
  case $OPTION in
     c) cflag=1
        ;;
     k) cpkflag=1
        customPrivKey="$OPTARG"
        ;;
     p) cpcflag=1
        customPrivCert="$OPTARG"
        ;;
     t) cccflag=1
        customCertChain="$OPTARG"
        ;;
     u) ccacflag=1
        customCACert="$OPTARG"
        ;;
     i) publicIp="$OPTARG"
        ;;
     h) hostName="$OPTARG"
        ;;
     ?) help
        ;;
   esac
done


if [ -z "$publicIp" ] || [ -z "$hostName" ]
then
   help
   exit 1
fi

if [ "$cflag" == "1" ]
then
  if [ "$cpkflag$cpcflag" != "11" ]
  then
     help
     exit 1
  fi
  if [ ! -f "$customPrivKey" ]
  then
     printf "private key file does not exist\n"
     exit 2
  fi

  if [ ! -f "$customPrivCert" ]
  then
     printf "public certificate does not exist\n"
     exit 3
  fi

  if [ "$cccflag" == "1" ]
  then
     if [ ! -f "$customCertChain" ]
     then
        printf "certificate chain does not exist\n"
        exit 4
     fi
  fi
fi

copy_certs_apache2
if [ $? -ne 0 ]
then
  echo "Failed to copy certificates"
  exit 2
fi

if [ -f "$customCACert" ]
then
  keytool -delete -alias $aliasName -keystore $keyStore -storepass $storepass -noprompt || true
  keytool -import -alias $aliasName -keystore $keyStore -storepass $storepass -noprompt -file $customCACert
  keytool -importkeystore -srckeystore $defaultJavaKeyStoreFile -destkeystore $keyStore -srcstorepass $defaultJavaKeyStorePass -deststorepass $storepass -noprompt
fi

config_apache2_conf $publicIp $hostName
systemctl restart apache2
