CREATE TABLE standup_attachments
(
    id              UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    standup_id      UUID          NOT NULL REFERENCES standups (id) ON DELETE CASCADE,
    attachment_type VARCHAR(10)   NOT NULL CHECK (attachment_type IN ('LINK', 'IMAGE')),
    url             VARCHAR(2048) NOT NULL,
    label           VARCHAR(200),
    thumbnail_url   VARCHAR(2048),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    version         BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX idx_attachment_standup ON standup_attachments (standup_id);

CREATE TRIGGER set_standup_attachments_updated_at
    BEFORE UPDATE
    ON standup_attachments
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE comment_reactions
(
    comment_id UUID         NOT NULL REFERENCES comments (id) ON DELETE CASCADE,
    user_id    VARCHAR(255) NOT NULL, -- userId.toString() (UUID as string)
    emoji      VARCHAR(20)  NOT NULL,
    PRIMARY KEY (comment_id, user_id)
);

CREATE INDEX idx_reaction_comment ON comment_reactions (comment_id);

ALTER TABLE decision_sessions
    ADD COLUMN IF NOT EXISTS linked_standup_id UUID REFERENCES standups (id) ON DELETE SET NULL;

CREATE INDEX idx_session_linked_standup ON decision_sessions (linked_standup_id)
    WHERE linked_standup_id IS NOT NULL;
