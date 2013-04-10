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
}


config_httpd_conf() {
  local ip=$1
  local srvr=$2
  cp -f /etc/httpd/conf/httpd.conf.orig /etc/httpd/conf/httpd.conf
  sed -i -e "s/Listen.*:80$/Listen $ip:80/" /etc/httpd/conf/httpd.conf
  echo "<VirtualHost $ip:443> " >> /etc/httpd/conf/httpd.conf
  echo "  DocumentRoot /var/www/html/" >> /etc/httpd/conf/httpd.conf
  echo "  ServerName $srvr" >> /etc/httpd/conf/httpd.conf
  echo "  SSLEngine on" >>  /etc/httpd/conf/httpd.conf
  echo "  SSLCertificateFile /etc/httpd/ssl/certs/realhostip.crt" >>  /etc/httpd/conf/httpd.conf
  echo "  SSLCertificateKeyFile /etc/httpd/ssl/keys/realhostip.key" >> /etc/httpd/conf/httpd.conf
  echo "</VirtualHost>" >> /etc/httpd/conf/httpd.conf
}

config_apache2_conf() {
  local ip=$1
  local srvr=$2
  cp -f /etc/apache2/sites-available/default.orig /etc/apache2/sites-available/default
  cp -f /etc/apache2/sites-available/default-ssl.orig /etc/apache2/sites-available/default-ssl
  sed -i -e "s/<VirtualHost.*>/<VirtualHost $ip:80>/" /etc/apache2/sites-available/default
  sed -i -e "s/<VirtualHost.*>/<VirtualHost $ip:443>/" /etc/apache2/sites-available/default-ssl
  sed -i -e "s/Listen .*:80/Listen $ip:80/g" /etc/apache2/ports.conf
  sed -i -e "s/Listen .*:443/Listen $ip:443/g" /etc/apache2/ports.conf
  sed -i -e "s/NameVirtualHost .*:80/NameVirtualHost $ip:80/g" /etc/apache2/ports.conf
  sed -i  's/ssl-cert-snakeoil.key/cert_apache.key/' /etc/apache2/sites-available/default-ssl
  sed -i  's/ssl-cert-snakeoil.pem/cert_apache.crt/' /etc/apache2/sites-available/default-ssl
}

copy_certs() {
  local certdir=$(dirname $0)/certs
  local mydir=$(dirname $0)
  if [ -d $certdir ] && [ -f $customPrivKey ] &&  [ -f $customPrivCert ] ; then
       mkdir -p /etc/httpd/ssl/keys  &&  mkdir -p /etc/httpd/ssl/certs  &&  cp $customprivKey /etc/httpd/ssl/keys   &&  cp $customPrivCert /etc/httpd/ssl/certs
      return $?
  fi
  if [ ! -z customCertChain ] && [ -f $customCertChain ] ; then
     cp $customCertChain /etc/httpd/ssl/certs  
  fi
  return 1
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
publicIp=
hostName=
while getopts 'i:h:k:p:t:c' OPTION
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
     printf "priviate key file is not exist\n"
     exit 2
  fi

  if [ ! -f "$customPrivCert" ]
  then
     printf "public certificate is not exist\n"
     exit 3
  fi

  if [ "$cccflag" == "1" ] 
  then
     if [ ! -f "$customCertChain" ]
     then
        printf "certificate chain is not exist\n"
        exit 4
     fi
  fi
fi

if [ -d /etc/apache2 ]
then
  copy_certs_apache2
else
  copy_certs
fi

if [ $? -ne 0 ]
then
  echo "Failed to copy certificates"
  exit 2
fi

if [ -d /etc/apache2 ]
then
  config_apache2_conf $publicIp $hostName
  /etc/init.d/apache2 stop
  /etc/init.d/apache2 start
else
  config_httpd_conf $publicIp $hostName
fi


