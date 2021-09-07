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
import os
from .utilities import bash
from .cloudException import CloudRuntimeException
import mysql.connector

class Database(object):
    """Database connection"""
    def __init__(self, username, password=None, host='localhost', port='3306', db="cloud"):
        self.host = host
        self.username = username
        self.password = password
        self.port = port
        self.db = db

    def connect(self):
        return mysql.connector.connect(host=self.host,
                                       user=self.username,
                                       password=self.password,
                                       database=self.db)

    def execute(self, statement):
        txn = None
        try:
            txn = self.connect()
            cursor = txn.cursor()
            cursor.execute(statement)
            cursor.close()
            txn.commit()
            if txn is not None:
                try:
                    txn.close()
                except:
                    pass
        except:
            raise CloudRuntimeException("Failed to execute: %s " % statement)
        finally:
            if txn is not None:
                try:
                    txn.close()
                except:
                    pass

    def testConnection(self):
        try:
            conn = self.connect()
            conn.ping()
            conn.close()
            return True
        except:
            raise CloudRuntimeException("Failed to Connect to DB")

    def executeFromFile(self, file):
        if not os.path.exists(file):
            return False

        cmdLine = "mysql --host=" + self.host + " --port=" + str(self.port) + " --user=" + self.username
        if self.password is not None:
            cmdLine += " --password=" + self.password

        cmdLine += " < " + file

        try:
            bash(cmdLine)
        except:
            raise CloudRuntimeException("Failed to execute " + cmdLine)
