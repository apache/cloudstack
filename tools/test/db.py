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

