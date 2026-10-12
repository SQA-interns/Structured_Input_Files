-- Contract: registration storage (backend <-> PostgreSQL). Spec section 6.
-- The Flyway migration V1__registration_schema.sql is this file verbatim.

CREATE TABLE registration (
    id                UUID         PRIMARY KEY,
    registration_type VARCHAR(16)  NOT NULL CHECK (registration_type IN ('EXTERNAL', 'STUDENT')),
    first_name        VARCHAR(100) NOT NULL,
    last_name         VARCHAR(100) NOT NULL,
    email             VARCHAR(254) NOT NULL,
    organization      VARCHAR(200),
    study_institution VARCHAR(200),
    study_programme   VARCHAR(200),
    student_id        VARCHAR(50),
    registered_at     TIMESTAMPTZ  NOT NULL
);

CREATE INDEX registration_registered_at_idx ON registration (registered_at);

CREATE TABLE registration_option (
    registration_id UUID         NOT NULL REFERENCES registration (id) ON DELETE CASCADE,
    option_id       VARCHAR(64)  NOT NULL,
    option_name     VARCHAR(200) NOT NULL,
    option_category VARCHAR(16)  NOT NULL CHECK (option_category IN ('WORKSHOP', 'EVENT', 'MEAL', 'OTHER')),
    PRIMARY KEY (registration_id, option_id)
);

CREATE TABLE registration_consent (
    registration_id UUID          NOT NULL REFERENCES registration (id) ON DELETE CASCADE,
    consent_id      VARCHAR(64)   NOT NULL,
    consent_text    VARCHAR(1000) NOT NULL,
    given_at        TIMESTAMPTZ   NOT NULL,
    PRIMARY KEY (registration_id, consent_id)
);
