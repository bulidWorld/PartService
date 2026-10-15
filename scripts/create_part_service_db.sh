#!/bin/bash

set -e

DB_NAME="part_service_db"
DB_USER="part_service"
DB_PASSWORD="Changeme_123"

echo "==> 创建 PostgreSQL 用户和数据库"

sudo -u postgres psql <<EOF
-- 创建用户（如果不存在）
DO \$\$
BEGIN
    IF NOT EXISTS (
        SELECT FROM pg_catalog.pg_roles
        WHERE rolname = '${DB_USER}'
    ) THEN
        CREATE ROLE ${DB_USER} LOGIN PASSWORD '${DB_PASSWORD}';
    ELSE
        ALTER ROLE ${DB_USER} WITH LOGIN PASSWORD '${DB_PASSWORD}';
    END IF;
END
\$\$;

-- 创建数据库（如果不存在）
SELECT 'CREATE DATABASE ${DB_NAME} OWNER ${DB_USER}'
WHERE NOT EXISTS (
    SELECT FROM pg_database
    WHERE datname = '${DB_NAME}'
)\gexec

-- 确保数据库 owner 正确
ALTER DATABASE ${DB_NAME} OWNER TO ${DB_USER};
EOF

echo "==> 创建完成"
echo "数据库: ${DB_NAME}"
echo "用户:   ${DB_USER}"
echo "密码:   ${DB_PASSWORD}"