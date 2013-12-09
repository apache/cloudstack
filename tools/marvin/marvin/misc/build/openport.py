from marvin import dbConnection

def _openIntegrationPort():
    dbhost = '10.223.132.200'#csconfig.dbSvr.dbSvr
    dbuser = 'cloud'#csconfig.dbSvr.user
    dbpasswd = 'cloud'#csconfig.dbSvr.passwd
    conn = dbConnection.dbConnection(dbhost, 3306, dbuser, dbpasswd, "cloud")
    query = "update configuration set value='8096' where name='integration.api.port'"
    print conn.execute(query)
    query = "select name,value from configuration where name='integration.api.port'"
    print conn.execute(query)

if __name__ == '__main__':
    _openIntegrationPort()
