# -*- coding: utf-8 -*-
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

# Use following rules for versioning:
# <cloudstack version>-<cli increment, starts from 0>
__version__ = "4.1.0-0"
__description__ = "Command Line Interface for Apache CloudStack"
__maintainer__ = "Rohit Yadav"
__maintaineremail__ = "bhaisaab@apache.org"
__project__ = "The Apache CloudStack Team"
__projectemail__ = "dev@cloudstack.apache.org"
__projecturl__ = "http://cloudstack.apache.org"

try:
    import os
    import sys

    from ConfigParser import ConfigParser, SafeConfigParser
    from os.path import expanduser
except ImportError, e:
    print "ImportError", e

param_type = ['boolean', 'date', 'float', 'integer', 'short', 'list',
              'long', 'object', 'map', 'string', 'tzdate', 'uuid']

iterable_type = ['set', 'list', 'object']

config_dir = expanduser('~/.cloudmonkey')
config_file = expanduser(config_dir + '/config')

# cloudmonkey config fields
config_fields = {'core': {}, 'server': {}, 'user': {}, 'ui': {}}

# core
config_fields['core']['asyncblock'] = 'true'
config_fields['core']['paramcompletion'] = 'false'
config_fields['core']['cache_file'] = expanduser(config_dir + '/cache')
config_fields['core']['history_file'] = expanduser(config_dir + '/history')
config_fields['core']['log_file'] = expanduser(config_dir + '/log')

# ui
config_fields['ui']['color'] = 'true'
config_fields['ui']['prompt'] = '> '
config_fields['ui']['tabularize'] = 'false' # deprecate - REMOVE
config_fields['ui']['display'] = 'default' # default display mechanism

# server
config_fields['server']['host'] = 'localhost'
config_fields['server']['path'] = '/client/api'
config_fields['server']['port'] = '8080'
config_fields['server']['protocol'] = 'http'
config_fields['server']['timeout'] = '3600'

# user
config_fields['user']['apikey'] = ''
config_fields['user']['secretkey'] = ''


def write_config(get_attr, config_file, first_time=False):
    global config_fields
    config = ConfigParser()
    for section in config_fields.keys():
        config.add_section(section)
        for key in config_fields[section].keys():
            if first_time:
                config.set(section, key, config_fields[section][key])
            else:
                config.set(section, key, get_attr(key))
    with open(config_file, 'w') as cfg:
        config.write(cfg)
    return config


def read_config(get_attr, set_attr, config_file):
    global config_fields, config_dir
    if not os.path.exists(config_dir):
        os.makedirs(config_dir)

    config_options = reduce(lambda x, y: x + y, map(lambda x:
                            config_fields[x].keys(), config_fields.keys()))

    if os.path.exists(config_file):
        config = ConfigParser()
        try:
            with open(config_file, 'r') as cfg:
                config.readfp(cfg)
        except IOError, e:
            print "Error: config_file not found", e
    else:
        config = write_config(get_attr, config_file, True)
        print "Welcome! Using `set` configure the necessary settings:"
        print " ".join(sorted(config_options))
        print "Config file:", config_file
        print "After setting up, run the `sync` command to sync apis\n"

    missing_keys = []
    for section in config_fields.keys():
        for key in config_fields[section].keys():
            try:
                if( key == "tabularize" ): # this key is deprecated
                    print "\ntabularize config parameter is deprecated:",
                    print "please switch to display =",
                    print "[default,json,tabularize]\n"
                set_attr(key, config.get(section, key))
            except Exception:
                if( key == "tabularize" ): # this key is deprecated
                    set_attr( key, "false" ) # set default
                elif( key == "display" ): # this key is deprecated
                    config = write_config(get_attr, config_file, True)
                    set_attr( key, "default" ) # set default
                else:
                    missing_keys.append(key)

    if len(missing_keys) > 0:
        print "Please fix `%s` in %s" % (', '.join(missing_keys), config_file)
        sys.exit()

    return config_options
