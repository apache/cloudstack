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

import MySQLdb

class Database:
    """Database connection"""
    def __init__(self, host='localhost', username='cloud', password='cloud', db='cloud'):
        self._conn = MySQLdb.connect (host, username, password, db)

    def update(self, statement):
        cursor = self._conn.cursor ()
        #print statement
        cursor.execute (statement)
        #print "Number of rows updated: %d" % cursor.rowcount
        cursor.close ()
        self._conn.commit ()

    def __del__(self):
        self._conn.close ()

