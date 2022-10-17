#!/usr/bin/env bash

echo "CREATE DATABASE IF NOT EXISTS $SERVICE_DB_NAME" | mysql -h $AWS_RDS_HOSTNAME -P $AWS_RDS_PORT -u $AWS_RDS_ADMIN_USERNAME -p$AWS_RDS_ADMIN_PASSWORD
echo "CREATE USER IF NOT EXISTS '$SERVICE_DB_USERNAME'@'%' IDENTIFIED BY '$SERVICE_DB_PASSWORD'" | mysql -h $AWS_RDS_HOSTNAME -P $AWS_RDS_PORT -u $AWS_RDS_ADMIN_USERNAME -p$AWS_RDS_ADMIN_PASSWORD
echo "GRANT REFERENCES, SELECT, INSERT, UPDATE, DELETE, LOCK TABLES, CREATE, DROP, INDEX, ALTER, CREATE TEMPORARY TABLES, CREATE VIEW, EVENT, TRIGGER, SHOW VIEW, CREATE ROUTINE, ALTER ROUTINE, EXECUTE ON $SERVICE_DB_NAME.* TO '$SERVICE_DB_USERNAME'@'%'" | mysql -h $AWS_RDS_HOSTNAME -P $AWS_RDS_PORT -u $AWS_RDS_ADMIN_USERNAME -p$AWS_RDS_ADMIN_PASSWORD
echo "SHOW GRANTS for '$SERVICE_DB_USERNAME'@'%'" | mysql -h $AWS_RDS_HOSTNAME -P $AWS_RDS_PORT -u $AWS_RDS_ADMIN_USERNAME -p$AWS_RDS_ADMIN_PASSWORD
