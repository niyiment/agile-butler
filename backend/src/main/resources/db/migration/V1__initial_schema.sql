CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

CREATE TABLE teams
(
    id                    UUID PRIMARY KEY      DEFAULT uuid_generate_v4(),
    name                  VARCHAR(100) NOT NULL,
    description           VARCHAR(500),
    standup_deadline_time TIME         NOT NULL DEFAULT '11:00:00',
    timezone              VARCHAR(50)  NOT NULL DEFAULT 'UTC',
    invite_code           VARCHAR(12) UNIQUE,
    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by            VARCHAR(150),
    updated_by            VARCHAR(150),
    version               BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE users
(
    id                UUID PRIMARY KEY      DEFAULT uuid_generate_v4(),
    name              VARCHAR(100) NOT NULL,
    email             VARCHAR(150) NOT NULL,
    password_hash     TEXT         NOT NULL,
    avatar_url        TEXT,
    fcm_token         TEXT,
    notification_time TIME,
    timezone          VARCHAR(50)  NOT NULL DEFAULT 'UTC',
    role              VARCHAR(20)  NOT NULL DEFAULT 'MEMBER',
    team_id           UUID         REFERENCES teams (id) ON DELETE SET NULL,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(150),
    updated_by        VARCHAR(150),
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'SCRUM_MASTER', 'MEMBER'))
);

CREATE INDEX idx_users_team ON users (team_id);
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_notification_time ON users (notification_time) WHERE is_active = TRUE;

CREATE TABLE standups
(
    id                 UUID PRIMARY KEY     DEFAULT uuid_generate_v4(),
    user_id            UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    team_id            UUID        NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    standup_date       DATE        NOT NULL,
    yesterday_text     TEXT,
    today_text         TEXT,
    blockers_text      TEXT,
    is_blocker_flagged BOOLEAN     NOT NULL DEFAULT FALSE,
    status             VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(150),
    updated_by         VARCHAR(150),
    version            BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uk_standup_user_date UNIQUE (user_id, standup_date),
    CONSTRAINT chk_standup_status CHECK (status IN ('DRAFT', 'SUBMITTED'))
);

CREATE INDEX idx_standup_team_date ON standups (team_id, standup_date);
CREATE INDEX idx_standup_user_date ON standups (user_id, standup_date);
CREATE INDEX idx_standup_blocker ON standups (team_id, standup_date) WHERE is_blocker_flagged = TRUE;

CREATE TABLE decision_sessions
(
    id                   UUID PRIMARY KEY      DEFAULT uuid_generate_v4(),
    title                VARCHAR(200) NOT NULL,
    description          TEXT,
    team_id              UUID         NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    created_by_user_id   UUID         NOT NULL REFERENCES users (id),
    session_type         VARCHAR(20)  NOT NULL,
    decision_type        VARCHAR(20)  NOT NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    is_anonymous         BOOLEAN      NOT NULL DEFAULT FALSE,
    max_duration_minutes INT,
    closes_at            TIMESTAMPTZ,
    closed_at            TIMESTAMPTZ,
    category             VARCHAR(50),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(150),
    updated_by           VARCHAR(150),
    version              BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT chk_session_type CHECK (session_type IN ('LIVE', 'TIMED_POLL')),
    CONSTRAINT chk_decision_type CHECK (decision_type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'YES_NO', 'RANKED')),
    CONSTRAINT chk_session_status CHECK (status IN ('ACTIVE', 'CLOSED'))
);

CREATE INDEX idx_session_team_status ON decision_sessions (team_id, status);
CREATE INDEX idx_session_closes_at ON decision_sessions (closes_at) WHERE status = 'ACTIVE';
CREATE INDEX idx_session_title_trgm ON decision_sessions USING GIN (title gin_trgm_ops);


CREATE TABLE decision_options
(
    id            UUID PRIMARY KEY      DEFAULT uuid_generate_v4(),
    session_id    UUID         NOT NULL REFERENCES decision_sessions (id) ON DELETE CASCADE,
    option_text   VARCHAR(300) NOT NULL,
    display_order INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by    VARCHAR(150),
    updated_by    VARCHAR(150),
    version       BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_option_session ON decision_options (session_id, display_order);

CREATE TABLE votes
(
    id           UUID PRIMARY KEY     DEFAULT uuid_generate_v4(),
    session_id   UUID        NOT NULL REFERENCES decision_sessions (id) ON DELETE CASCADE,
    user_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    option_id    UUID        NOT NULL REFERENCES decision_options (id) ON DELETE CASCADE,
    comment_text TEXT,
    rank_order   INT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by   VARCHAR(150),
    updated_by   VARCHAR(150),
    version      BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uk_vote_session_user_option UNIQUE (session_id, user_id, option_id)
);

CREATE INDEX idx_vote_session ON votes (session_id);
CREATE INDEX idx_vote_user_session ON votes (user_id, session_id);
CREATE INDEX idx_vote_option ON votes (option_id);

CREATE TABLE comments
(
    id                UUID PRIMARY KEY     DEFAULT uuid_generate_v4(),
    session_id        UUID        NOT NULL REFERENCES decision_sessions (id) ON DELETE CASCADE,
    user_id           UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    comment_text      TEXT        NOT NULL,
    parent_comment_id UUID REFERENCES comments (id) ON DELETE CASCADE,
    reaction_count    INT         NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(150),
    updated_by        VARCHAR(150),
    version           BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_comment_session ON comments (session_id, created_at);

CREATE TABLE notifications
(
    id           UUID PRIMARY KEY      DEFAULT uuid_generate_v4(),
    user_id      UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type         VARCHAR(40)  NOT NULL,
    title        VARCHAR(200) NOT NULL,
    body         TEXT,
    reference_id UUID,
    is_read      BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at      TIMESTAMPTZ,
    push_sent    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by   VARCHAR(150),
    updated_by   VARCHAR(150),
    version      BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_notif_user_read ON notifications (user_id, is_read);
CREATE INDEX idx_notif_created ON notifications (created_at);

CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO
$$
    DECLARE
        tbl TEXT;
    BEGIN
        FOREACH tbl IN ARRAY ARRAY ['teams', 'users', 'standups', 'decision_sessions',
            'decision_options', 'votes', 'comments', 'notifications']
            LOOP
                EXECUTE format('
            CREATE TRIGGER trg_%s_updated_at
            BEFORE UPDATE ON %s
            FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
        ', tbl, tbl);
            END LOOP;
    END;
$$;
