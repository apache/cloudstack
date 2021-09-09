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

FROM ubuntu:18.04

MAINTAINER "Apache CloudStack" <dev@cloudstack.apache.org>
LABEL Vendor="Apache.org" License="ApacheV2" Version="4.16.0.0-SNAPSHOT"

ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get -y update && apt-get install -y \
    genisoimage \
    libffi-dev \
    libssl-dev \
    git \
    sudo \
    ipmitool \
    iproute2 \
    maven \
    openjdk-11-jdk \
    python-dev \
    python-setuptools \
    python-pip \
    python-mysql.connector \
    supervisor

RUN apt-get install -qqy mysql-server && \
    apt-get clean all && \
    mkdir /var/run/mysqld; \
    chown mysql /var/run/mysqld

RUN echo '''sql_mode = "STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION"''' >> /etc/mysql/mysql.conf.d/mysqld.cnf

COPY tools/docker/supervisord.conf /etc/supervisor/conf.d/supervisord.conf
COPY . ./root
WORKDIR /root

RUN mvn -Pdeveloper -Dsimulator -DskipTests clean install

RUN find /var/lib/mysql -type f -exec touch {} \; && \
    (/usr/bin/mysqld_safe &) && \
    sleep 5; \
    mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password by ''" --connect-expired-password; \
    mvn -Pdeveloper -pl developer -Ddeploydb; \
    mvn -Pdeveloper -pl developer -Ddeploydb-simulator; \
    MARVIN_FILE=`find /root/tools/marvin/dist/ -name "Marvin*.tar.gz"`; \
    pip install $MARVIN_FILE

VOLUME /var/lib/mysql

EXPOSE 8080 8096

CMD ["/usr/bin/supervisord"]
