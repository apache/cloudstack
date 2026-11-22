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
# Management server image for tools/docker/local/docker-compose.yml. Build context: repository root.

FROM ubuntu:24.04@sha256:9c7f200c07e3d4a03e7249d2c16bf66a51120fb876aa2242cf31451412543289

ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get -y update && apt-get install -y \
    genisoimage \
    libffi-dev \
    libssl-dev \
    curl \
    gcc-10 \
    git \
    sudo \
    ipmitool \
    iproute2 \
    maven \
    mysql-client \
    openjdk-17-jre-headless \
    openjdk-17-jdk \
    python3-dev \
    python-is-python3 \
    python3-setuptools \
    python3-pip \
    python3-mysql.connector && \
    apt-get clean all

COPY . /root
WORKDIR /root

# Point the management server (replace.properties) and deploydb
# (db.properties.override) at the mysql service instead of localhost
RUN sed -i 's/^DBHOST=.*/DBHOST=mysql/' build/replace.properties && \
    sed -e 's/^\(db\.[a-z]*\.host\)=.*/\1=mysql/' \
        -e 's/^db\.root\.password=.*/db.root.password=cloud/' \
        utils/conf/db.properties > utils/conf/db.properties.override

# The Maven repository is kept in a cache mount between builds, so dependencies
# are not downloaded again, and copied into the image, where jetty:run and
# deploydb need it at run time
RUN --mount=type=cache,target=/m2-cache \
    mvn -Dmaven.repo.local=/m2-cache -Pdeveloper -Dsimulator -DskipTests clean install && \
    mkdir -p /root/.m2 && \
    cp -a /m2-cache /root/.m2/repository

RUN pip3 install tools/marvin/dist/[mM]arvin*.tar.gz --break-system-packages

EXPOSE 8080 8096

CMD ["mvn", "-Dsimulator", "-Dorg.eclipse.jetty.annotations.maxWait=120", "-pl", ":cloud-client-ui", "jetty:run"]
