CREATE TABLE conversation_branches (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    forked_from_message_id VARCHAR(36) NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_branches_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE
);
CREATE INDEX ix_branches_conversation ON conversation_branches (conversation_id);

CREATE TABLE branch_checkpoints (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    branch_id VARCHAR(36) NULL,
    message_id VARCHAR(36) NULL,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_checkpoints_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE
);
CREATE INDEX ix_checkpoints_conversation ON branch_checkpoints (conversation_id);

CREATE TABLE context_items (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    conversation_id VARCHAR(36) NULL,
    project_id VARCHAR(36) NULL,
    name VARCHAR(150) NOT NULL,
    type VARCHAR(32) NOT NULL,
    content VARCHAR(12000) NOT NULL,
    pinned BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_context_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE
);
CREATE INDEX ix_context_user_conversation ON context_items (user_id, conversation_id);

CREATE TABLE shared_collaborators (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    collaborator_email VARCHAR(254) NOT NULL,
    role VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_collaborators_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE,
    CONSTRAINT fk_collaborators_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE
);
CREATE INDEX ix_collaborators_conversation ON shared_collaborators (conversation_id);
