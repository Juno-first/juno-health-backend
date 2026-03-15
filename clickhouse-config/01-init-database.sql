-- Create database
CREATE DATABASE IF NOT EXISTS healthcare_queue;

-- Use the database
USE healthcare_queue;

-- Create table for patient queue events with detailed information
CREATE TABLE IF NOT EXISTS patient_queue (
    patient_id String,
    patient_name String,
    department_id String,
    visit_id String,
    queue_entry_id String,
    position UInt32,
    queue_depth UInt32,
    priority_tier String,
    ai_priority_score UInt32,
    estimated_wait_minutes UInt32,
    symptom_severity String,
    pain_level Nullable(UInt8),
    symptom_categories Array(String),
    symptom_duration String,
    presenting_complaint String,
    additional_notes Nullable(String),
    status String,
    checked_in_at DateTime,
    room_name Nullable(String),
    assigned_staff_name Nullable(String),
    assigned_staff_role Nullable(String),
    event_type String,
    created_at DateTime DEFAULT now(),
    updated_at DateTime DEFAULT now(),
    event_timestamp DateTime64(3)
) ENGINE = ReplacingMergeTree(updated_at)
ORDER BY (patient_id, event_timestamp)
PARTITION BY toYYYYMM(event_timestamp);

-- Create table for queue analytics
CREATE TABLE IF NOT EXISTS queue_analytics (
    event_date Date,
    total_patients UInt64,
    high_priority_patients UInt64,
    average_wait_time_seconds Float64,
    patients_processed UInt64
) ENGINE = SummingMergeTree()
ORDER BY event_date;
