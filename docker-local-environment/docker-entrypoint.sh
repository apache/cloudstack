#!/bin/bash
set -e  # Exit immediately on error

echo "----Starting MySQL container..."

# Start MySQL in the background
service mysql start

# Wait for MySQL to fully initialize
until mysqladmin ping &>/dev/null; do
  echo "Waiting for MySQL to be ready..."
  sleep 2
done

echo "----MySQL is running!"

# Run the initialization script for database setup
bash /cloudstack/init-mysql.sh

echo "----Starting CloudStack Management Server..."
service cloudstack-management start

# Keep the container running
exec bash
tail -f /dev/null
