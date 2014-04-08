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

#!/usr/bin/python
# coding: latin-1

smokecfg = {
            'browser':         'Firefox',
#           'window position': '10, 10',        # upper left coordinates
#           'window size':     '2000, 1500',
            'cssite':          'http://127.0.0.1:8080/client/',
#           'cssite':          'http://192.168.1.31:8080/client/',
            'username':        'admin',
            'password':        'password',
            'badusername':     'badname',
            'badpassword':     'badpassword',
            'sqlinjection_1':  '\' or 1=1 --\'',
            'sqlinjection_2':  '\' union select 1, badusername, badpassword, 1--\'',
            'sqlinjection_3':  '\' union select @@version,1,1,1--\'',
            'sqlinjection_4':  '\'; drop table user--\'',
            'sqlinjection_5':  '\'OR\' \'=\'',
            'language':        'English',

            # add a new user account
            'new user account':{'username':   'JohnD',
                                'password':   'password',
                                'email':      'johndoe@aol.com',
                                'firstname':  'John',
                                'lastname':   'Doe',
                                'domain':     'ROOT',
                                'type':       'User',                   # either 'User' or 'Admin'
                                'timezone':   'US/Eastern [Eastern Standard Time]',
                               },
            # add a new user under JohnD
            'account':         {'username':   'JohnD',
                                'domain':     'ROOT',
                                'type':       'User',
                               },
            # add a new user
            'new user':        {'username':   'JaneD',
                                'password':   'password',
                                'email':      'janedoe@aol.com',
                                'firstname':  'Jane',
                                'lastname':   'Doe',
                                'timezone':   'US/Eastern [Eastern Standard Time]',
                               },

           }

