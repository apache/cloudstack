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
LABEL Vendor="Apache.org" License="ApacheV2" Version="4.6.0"

RUN apt-get -y update && apt-get install -y \
    genisoimage \
    git \
    maven \
    openjdk-7-jdk \
    python-dev \
    python-setuptools \
    python-pip \
    supervisor

RUN echo 'mysql-server mysql-server/root_password password root' |  debconf-set-selections; \
    echo 'mysql-server mysql-server/root_password_again password root' |  debconf-set-selections;

RUN apt-get install -qqy mysql-server && \
    apt-get clean all

RUN (/usr/bin/mysqld_safe &); sleep 5; mysqladmin -u root -proot password ''

RUN pip install --allow-external mysql-connector-python mysql-connector-python

COPY tools/docker/supervisord.conf /etc/supervisor/conf.d/supervisord.conf
COPY . ./root
WORKDIR /root

RUN mvn -Pdeveloper -Dsimulator -DskipTests clean install

RUN (/usr/bin/mysqld_safe &); \
    sleep 3; \
    mvn -Pdeveloper -pl developer -Ddeploydb; \
    mvn -Pdeveloper -pl developer -Ddeploydb-simulator; \
    pip install tools/marvin/dist/Marvin-4.6.0-SNAPSHOT.tar.gz

EXPOSE 8080 8096

CMD ["/usr/bin/supervisord"]
