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

FROM ubuntu:14.04

MAINTAINER "Apache CloudStack" <dev@cloudstack.apache.org>
LABEL Vendor="Apache.org" License="ApacheV2" Version="4.10.0"

ARG mysql_connector_deb=mysql-connector-python_2.1.4-1ubuntu14.04_all.deb

RUN echo 'mysql-server mysql-server/root_password password root' |  debconf-set-selections; \
    echo 'mysql-server mysql-server/root_password_again password root' |  debconf-set-selections;

RUN apt-get -y update && apt-get install -y \
    genisoimage \
    git \
    maven \
    openjdk-7-jdk \
    python-dev \
    python-setuptools \
    python-pip \
    supervisor \
    wget \
    ipmitool \
    libffi-dev \
    libssl-dev \
    mysql-server \
&& apt-get clean all && rm -rf /var/lib/apt/lists/*

RUN (/usr/bin/mysqld_safe &); sleep 5; mysqladmin -u root -proot password ''

RUN wget -O /tmp/$mysql_connector_deb https://dev.mysql.com/get/Downloads/Connector-Python/$mysql_connector_deb && dpkg -i /tmp/$mysql_connector_deb

COPY tools/docker/supervisord.conf /etc/supervisor/conf.d/supervisord.conf
COPY . ./root
WORKDIR /root

RUN mvn -Pdeveloper -Dsimulator -DskipTests clean install

RUN (/usr/bin/mysqld_safe &); \
    sleep 3; \
    mvn -Pdeveloper -pl developer -Ddeploydb; \
    mvn -Pdeveloper -pl developer -Ddeploydb-simulator; \
    find /root/tools/marvin/dist/ -name "Marvin*.tar.gz" -exec pip install {} \;; \
    pip install cryptography --force-reinstall

EXPOSE 8080 8096

CMD ["/usr/bin/supervisord"]
