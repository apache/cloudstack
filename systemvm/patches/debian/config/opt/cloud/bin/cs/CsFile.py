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
import logging
import re
import copy


class CsFile:
    """ File editors """

    def __init__(self, filename):
        self.filename = filename
        self.load()

    def load(self):
        self.new_config = []
        self.config = []
        try:
            for line in open(self.filename):
                self.new_config.append(line)
        except IOError:
            logging.debug("File %s does not exist" % self.filename)
            return
        else:
            logging.debug("Reading file %s" % self.filename)
            self.config = copy.deepcopy(self.new_config)

    def is_changed(self):
        if set(self.config) != set(self.new_config):
            return True
        else:
            return False

    def empty(self):
        self.config = []
        self.new_config = []

    def commit(self):
        if not self.is_changed():
            return
        handle = open(self.filename, "w+")
        for line in self.new_config:
            handle.write(line)
        handle.close()
        logging.info("Wrote edited file %s" % self.filename)

    def dump(self):
        for line in self.new_config:
            print line

    def addeq(self, string):
        """ Update a line in a file of the form token=something
        match on token= and replace something if needed
        Add line if token is not present
        """
        token = string.split('=')[0] + '='
        self.search(token, string)

    def add(self, string, where=-1):
        for index, line in enumerate(self.new_config):
            if line.strip() == string:
                return
        if where == -1:
            self.new_config.append("%s\n" % string)
        else:
            self.new_config.insert(where, "%s\n" % string)

    def section(self, start, end, content):
        sind = -1
        eind = -1
        found = False
        for index, line in enumerate(self.new_config):
            if found and line.strip() == end:
                eind = index
                found = False
            if line.strip() == start:
                sind = index + 1
                found = True
        self.new_config[sind:eind] = content

    def greplace(self, search, replace):
        self.new_config = [w.replace(search, replace) for w in self.new_config]

    def search(self, search, replace):
        found = False
        logging.debug("Searching for %s and replacing with %s" % (search, replace))
        for index, line in enumerate(self.new_config):
            if line.lstrip().startswith("#"):
                continue
            if re.search(search, line):
                found = True
                if replace not in line:
                    self.new_config[index] = replace + "\n"
        if not found:
            self.new_config.append(replace + "\n")

    def compare(self, o):
        return (isinstance(o, self.__class__) and set(self.config) == set(o.new_config))
