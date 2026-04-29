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


from marvin import dbConnection

def _openIntegrationPort():
    dbhost = '10.223.132.200'#csconfig.dbSvr.dbSvr
    dbuser = 'cloud'#csconfig.dbSvr.user
    dbpasswd = 'cloud'#csconfig.dbSvr.passwd
    conn = dbConnection.dbConnection(dbhost, 3306, dbuser, dbpasswd, "cloud")
    query = "update configuration set value='8096' where name='integration.api.port'"
    print(conn.execute(query))
    query = "select name,value from configuration where name='integration.api.port'"
    print(conn.execute(query))

if __name__ == '__main__':
    _openIntegrationPort()
