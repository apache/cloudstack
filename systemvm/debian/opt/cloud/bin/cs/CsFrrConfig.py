#!/usr/bin/python
# -- coding: utf-8 --
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import re

# DOTALL masks a password that contains a line break, up to the end of the entry.
PASSWORD = re.compile(r'^(\s*neighbor\s+\S+\s+password\s+)\S.*$', re.DOTALL)


def mask_passwords(lines):
    """Returns a copy of the frr.conf lines with every neighbor password value replaced by <masked>."""
    return [PASSWORD.sub(r'\g<1><masked>', line) for line in lines]
