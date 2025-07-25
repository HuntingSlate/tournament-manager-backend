#!/bin/bash
/opt/mssql-tools/bin/sqlcmd -S ${DB_HOST} -U sa -P "${SA_PASSWORD}" -i /app/sql/run_integrity_check.sql