CREATE TABLE IF NOT EXISTS app_users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(40) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_app_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS user_data (
    user_id BIGINT NOT NULL,
    data_key VARCHAR(40) NOT NULL,
    data_value MEDIUMTEXT NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, data_key),
    CONSTRAINT fk_user_data_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE
);
