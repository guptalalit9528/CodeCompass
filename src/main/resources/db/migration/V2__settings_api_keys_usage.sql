CREATE TABLE api_keys (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    key_prefix VARCHAR(32) NOT NULL,
    key_hash VARCHAR(64) NOT NULL,
    scopes VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP NULL,
    revoked_at TIMESTAMP NULL,
    CONSTRAINT fk_api_keys_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE,
    CONSTRAINT uk_api_keys_hash UNIQUE (key_hash)
);
CREATE INDEX ix_api_keys_user ON api_keys (user_id);

CREATE TABLE user_preferences (
    user_id VARCHAR(36) PRIMARY KEY,
    theme VARCHAR(16) NOT NULL,
    code_style VARCHAR(32) NOT NULL,
    preferred_language VARCHAR(32) NOT NULL,
    font_size VARCHAR(16) NOT NULL,
    line_numbers BOOLEAN NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE
);

CREATE TABLE usage_events (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    source VARCHAR(16) NOT NULL,
    model_name VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    prompt_tokens INT NOT NULL,
    completion_tokens INT NOT NULL,
    total_tokens INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_usage_events_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE
);
CREATE INDEX ix_usage_events_user_created ON usage_events (user_id, created_at);
