#!/bin/bash
# Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
#     
# This software is licensed under the GNU General Public License v3 or later.
# 
# It is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or any later version.
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
# 



 



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
  sed -i  's/ssl-cert-snakeoil.key/realhostip.key/' /etc/apache2/sites-available/default-ssl
  sed -i  's/ssl-cert-snakeoil.pem/realhostip.crt/' /etc/apache2/sites-available/default-ssl
}

copy_certs() {
  local certdir=$(dirname $0)/certs
  local mydir=$(dirname $0)
  if [ -d $certdir ] && [ -f $certdir/realhostip.key ] &&  [ -f $certdir/realhostip.crt ] ; then
       mkdir -p /etc/httpd/ssl/keys  &&  mkdir -p /etc/httpd/ssl/certs  &&  cp $certdir/realhostip.key /etc/httpd/ssl/keys   &&  cp $certdir/realhostip.crt /etc/httpd/ssl/certs
      return $?
  fi
  return 1
}

copy_certs_apache2() {
  local certdir=$(dirname $0)/certs
  local mydir=$(dirname $0)
  if [ -d $certdir ] && [ -f $certdir/realhostip.key ] &&  [ -f $certdir/realhostip.crt ] ; then
      cp $certdir/realhostip.key /etc/ssl/private/   &&  cp $certdir/realhostip.crt /etc/ssl/certs/
      return $?
  fi
  return 1
}

if [ $# -ne 2 ] ; then
	echo $"Usage: `basename $0` ipaddr servername "
	exit 0
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
  config_apache2_conf $1 $2
else
  config_httpd_conf $1 $2
fi
