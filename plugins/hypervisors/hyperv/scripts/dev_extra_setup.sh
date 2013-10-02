#!/bin/bash
mvn -P developer -pl developer -Ddeploydb
cp client/target/cloud-client-ui-4.2.0-SNAPSHOT/WEB-INF/classes/log4j{-cloud,}.xml
mysql --user=root --password="" cloud -e "update configuration set value='false' where name='developer';"
mysql --user=root --password="" cloud -e "INSERT INTO configuration (instance, name,value) VALUE('DEFAULT','system.vm.use.local.storage', 'true');" 
update template_view set url='http://10.70.176.29/pub/systemvmtemplate-2013-07-04-master-hyperv.vhd' where  name='SystemVM Template (HyperV)';
export MAVEN_OPTS="-XX:MaxPermSize=256m -Xmx1g"
mvn -pl :cloud-client-ui jetty:run
