#!/bin/bash


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

copy_certs() {
  local certdir=$(dirname $0)/certs
  local mydir=$(dirname $0)
  if [ -d $certdir ] && [ -f $certdir/realhostip.key ] &&  [ -f $certdir/realhostip.crt ] ; then
       mkdir -p /etc/httpd/ssl/keys  &&  mkdir -p /etc/httpd/ssl/certs  &&  cp $certdir/realhostip.key /etc/httpd/ssl/keys   &&  cp $certdir/realhostip.crt /etc/httpd/ssl/certs
      return $?
  fi
  return 1
}

if [ $# -ne 2 ] ; then
	echo $"Usage: `basename $0` ipaddr servername "
	exit 0
fi

copy_certs
if [ $? -ne 0 ]
then
  echo "Failed to copy certificates"
  exit 2
fi

config_httpd_conf $1 $2
