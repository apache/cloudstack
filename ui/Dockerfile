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

# Build example: docker build -t <name> .

FROM node:lts-stretch AS build

MAINTAINER "Apache CloudStack" <dev@cloudstack.apache.org>
LABEL Description="Apache CloudStack UI; Modern role-base progressive UI for Apache CloudStack"
LABEL Vendor="Apache.org"
LABEL License=ApacheV2
LABEL Version=0.5.0

WORKDIR /build

RUN apt-get -y update && apt-get -y upgrade

COPY . /build/
RUN npm install
RUN npm run build

FROM nginx:alpine AS runtime

LABEL org.opencontainers.image.title="Apache CloudStack UI" \
	org.opencontainers.image.description="A modern role-based progressive CloudStack UI" \
	org.opencontainers.image.authors="Apache CloudStack Contributors" \
	org.opencontainers.image.url="https://github.com/apache/cloudstack" \
	org.opencontainers.image.documentation="https://github.com/apache/cloudstack/blob/main/ui/README.md" \
	org.opencontainers.image.source="https://github.com/apache/cloudstack" \
	org.opencontainers.image.vendor="The Apache Software Foundation" \
	org.opencontainers.image.licenses="Apache-2.0" \
	org.opencontainers.image.ref.name="latest"

COPY --from=build /build/dist/. /usr/share/nginx/html/
