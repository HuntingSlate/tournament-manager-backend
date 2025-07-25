#!/bin/bash

echo "Maintenance container started. Waiting 10 seconds for SQL Server..."
sleep 10

echo "Running DB initialization script (CREATE DATABASE)..."
/opt/mssql-tools/bin/sqlcmd -S ${DB_HOST} -U sa -P "${SA_PASSWORD}" -d master -i /app/sql/init_database.sql

echo "Installing Ola Hallengren's Maintenance Solution..."
/opt/mssql-tools/bin/sqlcmd -S ${DB_HOST} -U sa -P "${SA_PASSWORD}" -d master -i /app/sql/MaintenanceSolution.sql

echo "Initialization complete. Starting cron daemon..."
cron -f