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
import os
from utilities import bash
from cloudException import CloudRuntimeException
import sys
class Database:
    """Database connection"""
    def __init__(self, username, password=None, host='localhost', port='3306', db="cloud"):
        self.host = host
        self.username = username
        self.password = password
        self.port = port
        self.db = db

    def execute(self, statement):
        txn = None
        try:
            if self.password is not None:
                txn = MySQLdb.Connect(host=self.host, user=self.username,
                                 passwd=self.password, db=self.db)
            else:
                txn = MySQLdb.Connect(host=self.host, user=self.username,
                                 db=self.db)
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
            if txn is not None:
                try:
                    txn.close()
                except:
                    pass
            raise CloudRuntimeException("Failed to execute:%s"%statement)
        
    def testConnection(self):
        try:
            if self.password is not None:
                db = MySQLdb.Connect(host=self.host, user=self.username,
                                 passwd=self.password, db=self.db)
            else:
                db = MySQLdb.Connect(host=self.host, user=self.username,
                                  db=self.db)
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
