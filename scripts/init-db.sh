#!/bin/bash
# Initialize PostgreSQL database with required tables and partitions

set -e

POSTGRES_HOST=${POSTGRES_HOST:-/var/run/postgresql}
POSTGRES_PORT=${POSTGRES_PORT:-5432}
POSTGRES_USER=${POSTGRES_USER:-neochat}
POSTGRES_PASSWORD=${POSTGRES_PASSWORD:-neochat}
POSTGRES_DB=${POSTGRES_DB:-neochat_app}
KEYCLOAK_DB=${KEYCLOAK_DB:-keycloak}
KEYCLOAK_DB_USER=${KEYCLOAK_DB_USER:-keycloak_dev}
KEYCLOAK_DB_PASSWORD=${KEYCLOAK_DB_PASSWORD:-keycloak_dev_password}

echo "Initializing PostgreSQL database: $POSTGRES_DB"

# Export password for psql
export PGPASSWORD=$POSTGRES_PASSWORD

psql -v ON_ERROR_STOP=1 -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d postgres <<-EOSQL
    DO
    \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${KEYCLOAK_DB_USER}') THEN
            EXECUTE format('CREATE ROLE %I LOGIN PASSWORD %L', '${KEYCLOAK_DB_USER}', '${KEYCLOAK_DB_PASSWORD}');
        END IF;
    END
    \$\$;

    SELECT format('CREATE DATABASE %I', '${KEYCLOAK_DB}')
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${KEYCLOAK_DB}')
    \gexec

    GRANT ALL PRIVILEGES ON DATABASE "${KEYCLOAK_DB}" TO "${KEYCLOAK_DB_USER}";
    ALTER DATABASE "${KEYCLOAK_DB}" OWNER TO "${KEYCLOAK_DB_USER}";
EOSQL

psql -v ON_ERROR_STOP=1 -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$KEYCLOAK_DB" <<-EOSQL
    GRANT USAGE, CREATE ON SCHEMA public TO "${KEYCLOAK_DB_USER}";
    ALTER SCHEMA public OWNER TO "${KEYCLOAK_DB_USER}";
EOSQL

# Create NeoChat app schemas and tables
psql -v ON_ERROR_STOP=1 -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" <<-EOSQL
    -- Create app schemas
    CREATE SCHEMA IF NOT EXISTS chat;
    CREATE SCHEMA IF NOT EXISTS admin;

    -- Create chat_events table with range partitioning on timestamp
    CREATE TABLE IF NOT EXISTS chat.chat_events (
        id UUID NOT NULL,
        conversation_id VARCHAR(255) NOT NULL,
        event_type VARCHAR(50) NOT NULL,
        payload TEXT,
        user_id VARCHAR(255) NOT NULL,
        timestamp TIMESTAMP NOT NULL
    ) PARTITION BY RANGE (timestamp);

    -- Create default partition
    CREATE TABLE IF NOT EXISTS chat.chat_events_default PARTITION OF chat.chat_events DEFAULT;

    -- Create indexes
    CREATE INDEX IF NOT EXISTS idx_chat_events_conversation ON chat.chat_events(conversation_id);
    CREATE INDEX IF NOT EXISTS idx_chat_events_timestamp ON chat.chat_events(timestamp);
    CREATE INDEX IF NOT EXISTS idx_chat_events_user ON chat.chat_events(user_id);
    CREATE INDEX IF NOT EXISTS idx_chat_events_id ON chat.chat_events(id);

    -- Create audit_logs table
    CREATE TABLE IF NOT EXISTS admin.audit_logs (
        id UUID PRIMARY KEY,
        timestamp TIMESTAMP NOT NULL,
        user_id VARCHAR(255) NOT NULL,
        action VARCHAR(100) NOT NULL,
        resource_type VARCHAR(100) NOT NULL,
        resource_id VARCHAR(255),
        ip_address VARCHAR(45),
        user_agent TEXT,
        status VARCHAR(50) NOT NULL,
        details TEXT
    );

    -- Create indexes for audit_logs
    CREATE INDEX IF NOT EXISTS idx_audit_logs_user ON admin.audit_logs(user_id);
    CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON admin.audit_logs(timestamp);
    CREATE INDEX IF NOT EXISTS idx_audit_logs_resource ON admin.audit_logs(resource_type, resource_id);
    CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON admin.audit_logs(action);
    CREATE INDEX IF NOT EXISTS idx_audit_logs_status ON admin.audit_logs(status);

    -- Create function to automatically create monthly partitions
    CREATE OR REPLACE FUNCTION chat.create_monthly_partition()
    RETURNS void AS \$\$
    DECLARE
        start_date DATE;
        end_date DATE;
        partition_name TEXT;
    BEGIN
        start_date := DATE_TRUNC('month', CURRENT_DATE);
        end_date := start_date + INTERVAL '1 month';
        partition_name := 'chat_events_' || TO_CHAR(start_date, 'YYYY_MM');
        
        -- Check if partition already exists
        IF to_regclass('chat.' || partition_name) IS NULL THEN
            EXECUTE format(
                'CREATE TABLE chat.%I PARTITION OF chat.chat_events FOR VALUES FROM (%L) TO (%L)',
                partition_name, start_date::timestamp, end_date::timestamp
            );
        END IF;
    END;
    \$\$ LANGUAGE plpgsql;

    -- Create partitions for current and next month
    SELECT chat.create_monthly_partition();
EOSQL

echo "Database initialized successfully"
