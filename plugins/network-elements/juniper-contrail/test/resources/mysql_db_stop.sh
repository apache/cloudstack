if [ "$#" -ne 1 ] ; then
   echo "Usage: $0 <port>" >&2
   exit 1
fi

echo "Stopping mysql server on port "$1

mysqladmin -S /tmp/mysql$1/mysqld.sock shutdown -u root

rm -rf /tmp/mysql$1

echo "Deleting db directories"


