-- Registration storage contract (PostgreSQL 16). The Flyway migration
-- V1__create_registration_tables.sql in the backend applies exactly this schema (AR-06, ES-08).
-- Text columns are UTF-8 (database encoding UTF8, NFR-01); times are timestamptz in UTC.

CREATE TABLE registration (
    id                 uuid         PRIMARY KEY,
    type               varchar(16)  NOT NULL CHECK (type IN ('EXTERNAL', 'STUDENT')),
    first_name         varchar(100) NOT NULL,
    last_name          varchar(100) NOT NULL,
    email              varchar(254) NOT NULL,
    organization       varchar(200),
    study_institution  varchar(200),
    study_programme    varchar(200),
    student_id         varchar(50),
    registered_at      timestamptz  NOT NULL,
    CONSTRAINT registration_type_fields CHECK (
        (type = 'EXTERNAL' AND organization IS NOT NULL
            AND study_institution IS NULL AND study_programme IS NULL AND student_id IS NULL)
        OR
        (type = 'STUDENT' AND organization IS NULL
            AND study_institution IS NOT NULL AND study_programme IS NOT NULL AND student_id IS NOT NULL)
    )
);

CREATE INDEX registration_registered_at_idx ON registration (registered_at);

-- Selected options with the name and category at the time of registration.
CREATE TABLE registration_option (
    registration_id  uuid         NOT NULL REFERENCES registration (id) ON DELETE CASCADE,
    position         integer      NOT NULL,
    option_id        varchar(64)  NOT NULL,
    option_name      varchar(200) NOT NULL,
    category         varchar(16)  NOT NULL CHECK (category IN ('WORKSHOP', 'EVENT', 'MEAL', 'OTHER')),
    PRIMARY KEY (registration_id, option_id),
    UNIQUE (registration_id, position)
);

-- Consents given, with the wording shown and the time given (SB-14).
CREATE TABLE registration_consent (
    registration_id  uuid          NOT NULL REFERENCES registration (id) ON DELETE CASCADE,
    consent_id       varchar(64)   NOT NULL,
    consent_text     varchar(1000) NOT NULL,
    given_at         timestamptz   NOT NULL,
    PRIMARY KEY (registration_id, consent_id)
);
