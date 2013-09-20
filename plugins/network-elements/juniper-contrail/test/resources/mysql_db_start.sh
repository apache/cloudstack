if [ "$#" -ne 1 ] ; then
   echo "Usage: $0 <port>" >&2
   exit 1
fi

echo "starting mysql on port: "$1

echo "creating temporary mysql db directories /tmp/mysql"$1 
mkdir /tmp/mysql$1
mkdir /tmp/mysql$1/data

echo "install db";

mysql_install_db --user=$USER --datadir=/tmp/mysql$1/data
mysqld_safe --datadir=/tmp/mysql$1/data --socket=/tmp/mysql$1/mysqld.sock --port=$1 --log-error=/tmp/mysql$1/mysql.log --pid-file=/tmp/mysql$1/mysql.pid --user=$USER &

echo "new mysql server is started on port "$1

sleep 3

echo "commands ...."
echo "to connect(from local host): mysql -h 127.0.0.1 -P "$1 
echo "to stop: mysqladmin -S /tmp/mysql"$1"/mysqld.sock shutdown -u root"
