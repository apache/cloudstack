#!/bin/bash
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

set -e
set -x

function configure_locale() {
  grep LANG=en_US.UTF-8 /etc/default/locale && \
      grep LC_ALL=en_US.UTF-8 /etc/default/locale && \
      grep "^en_US.UTF-8 UTF-8" /etc/locale.gen &&
      return

  cat >> /etc/default/locale  << EOF
LANG=en_US.UTF-8
LC_ALL=en_US.UTF-8
EOF

  grep "^en_US.UTF-8 UTF-8" /etc/locale.gen || \
  cat >> /etc/locale.gen  << EOF
en_US.UTF-8 UTF-8
EOF

  locale-gen en_US.UTF-8
}

return 2>/dev/null || configure_locale
