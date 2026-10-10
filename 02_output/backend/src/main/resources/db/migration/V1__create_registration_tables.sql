-- US-005, AR-06: schema of docs/02_contracts/database-schema.sql

CREATE TABLE registration (
    id                BIGSERIAL     PRIMARY KEY,
    reference         UUID          NOT NULL,
    type              VARCHAR(16)   NOT NULL,
    first_name        VARCHAR(200)  NOT NULL,
    last_name         VARCHAR(200)  NOT NULL,
    email             VARCHAR(254)  NOT NULL,
    email_normalized  VARCHAR(254)  NOT NULL,
    organization      VARCHAR(200),
    study_institution VARCHAR(200),
    study_programme   VARCHAR(200),
    student_id        VARCHAR(200),
    submitted_at      TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uq_registration_reference UNIQUE (reference),
    CONSTRAINT uq_registration_email_normalized UNIQUE (email_normalized),
    CONSTRAINT ck_registration_type CHECK (type IN ('EXTERNAL', 'STUDENT')),
    CONSTRAINT ck_registration_fields CHECK (
        (type = 'EXTERNAL' AND organization IS NOT NULL
            AND study_institution IS NULL AND study_programme IS NULL AND student_id IS NULL)
        OR
        (type = 'STUDENT' AND organization IS NULL
            AND study_institution IS NOT NULL AND study_programme IS NOT NULL AND student_id IS NOT NULL)
    )
);

CREATE TABLE registration_option (
    registration_id BIGINT       NOT NULL REFERENCES registration (id) ON DELETE CASCADE,
    position        INTEGER      NOT NULL,
    option_id       VARCHAR(64)  NOT NULL,
    option_name     VARCHAR(200) NOT NULL,
    category        VARCHAR(16)  NOT NULL,
    PRIMARY KEY (registration_id, position),
    CONSTRAINT uq_registration_option UNIQUE (registration_id, option_id),
    CONSTRAINT ck_registration_option_category CHECK (category IN ('WORKSHOP', 'EVENT', 'MEAL', 'OTHER'))
);

CREATE TABLE registration_consent (
    registration_id BIGINT        NOT NULL REFERENCES registration (id) ON DELETE CASCADE,
    consent_id      VARCHAR(64)   NOT NULL,
    consent_text    VARCHAR(2000) NOT NULL,
    given_at        TIMESTAMPTZ   NOT NULL,
    PRIMARY KEY (registration_id, consent_id)
);

CREATE INDEX ix_registration_submitted_at ON registration (submitted_at);
