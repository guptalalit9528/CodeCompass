CREATE TABLE app_users (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_app_users_email UNIQUE (email)
);

CREATE TABLE auth_tokens (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    purpose VARCHAR(24) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_auth_tokens_user FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT uk_auth_tokens_hash UNIQUE (token_hash)
);
CREATE INDEX ix_auth_tokens_user_purpose ON auth_tokens (user_id, purpose);

CREATE TABLE projects (
    id VARCHAR(36) PRIMARY KEY,
    owner_id VARCHAR(36) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    instructions VARCHAR(4000) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES app_users (id)
);
CREATE INDEX ix_projects_owner_updated ON projects (owner_id, updated_at);

CREATE TABLE conversations (
    id VARCHAR(36) PRIMARY KEY,
    owner_id VARCHAR(36) NOT NULL,
    project_id VARCHAR(36) NULL,
    title VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_conversations_owner FOREIGN KEY (owner_id) REFERENCES app_users (id),
    CONSTRAINT fk_conversations_project FOREIGN KEY (project_id) REFERENCES projects (id)
);
CREATE INDEX ix_conversations_owner_updated ON conversations (owner_id, updated_at);
CREATE INDEX ix_conversations_project_updated ON conversations (project_id, updated_at);

CREATE TABLE chat_messages (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    role VARCHAR(16) NOT NULL,
    content VARCHAR(12000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_chat_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE
);
CREATE INDEX ix_chat_messages_conversation_created ON chat_messages (conversation_id, created_at);
