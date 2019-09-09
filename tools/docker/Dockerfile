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
#
# CloudStack-simulator build

FROM ubuntu:16.04

MAINTAINER "Apache CloudStack" <dev@cloudstack.apache.org>
LABEL Vendor="Apache.org" License="ApacheV2" Version="4.14.0.0-SNAPSHOT"

RUN apt-get -y update && apt-get install -y \
    genisoimage \
    libffi-dev \
    libssl-dev \
    git \
    sudo \
    ipmitool \
    maven \
    openjdk-8-jdk \
    python-dev \
    python-setuptools \
    python-pip \
    python-mysql.connector \
    supervisor

RUN echo 'mysql-server mysql-server/root_password password root' |  debconf-set-selections; \
    echo 'mysql-server mysql-server/root_password_again password root' |  debconf-set-selections;

RUN apt-get install -qqy mysql-server && \
    apt-get clean all && \
    mkdir /var/run/mysqld; \
    chown mysql /var/run/mysqld

RUN echo '''sql_mode = "STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION"''' >> /etc/mysql/mysql.conf.d/mysqld.cnf
RUN (/usr/bin/mysqld_safe &); sleep 5; mysqladmin -u root -proot password ''

#RUN pip install --allow-external mysql-connector-python mysql-connector-python

COPY tools/docker/supervisord.conf /etc/supervisor/conf.d/supervisord.conf
COPY . ./root
WORKDIR /root

RUN mvn -Pdeveloper -Dsimulator -DskipTests clean install

RUN (/usr/bin/mysqld_safe &); \
    sleep 5; \
    mvn -Pdeveloper -pl developer -Ddeploydb; \
    mvn -Pdeveloper -pl developer -Ddeploydb-simulator; \
    MARVIN_FILE=`find /root/tools/marvin/dist/ -name "Marvin*.tar.gz"`; \
    pip install $MARVIN_FILE

EXPOSE 8080 8096

CMD ["/usr/bin/supervisord"]
