#!/bin/bash
set -e  # Exit immediately on error

echo "----Running MySQL initialization script..."

# Wait until MySQL is ready, pings the MySQL server awaiting a response
until mysqladmin ping &>/dev/null; do
  echo "----Waiting for MySQL to be ready..."
  sleep 2
done

# Validate the database exists before creating it
DB_EXISTS=$(mysql -u root -p${MYSQL_ROOT_PASSWORD} -se "SHOW DATABASES LIKE 'cloud';")

if [ -z "$DB_EXISTS" ]; then
  echo "----Creating MySQL database..."
  mysql -u root -p${MYSQL_ROOT_PASSWORD} -e "CREATE DATABASE cloud;"
else
  echo "----Database 'cloud' already exists—skipping creation."
fi

# Validate the cloud user exists before creating it
USER_EXISTS=$(mysql -u root -p${MYSQL_ROOT_PASSWORD} -se "SELECT COUNT(*) FROM mysql.user WHERE user='cloud';")

if [ "$USER_EXISTS" -eq 0 ]; then
  echo "----Creating MySQL user..."
  mysql -u root -p${MYSQL_ROOT_PASSWORD} -e "
    CREATE USER 'cloud'@'%' IDENTIFIED BY 'cloud';
    GRANT ALL PRIVILEGES ON cloud.* TO 'cloud'@'%';
    FLUSH PRIVILEGES;"
else
  echo "----MySQL user 'cloud' already exists—skipping."
fi

echo "----MySQL setup complete"
