CREATE TABLE installation_authorizations (
    install_id TEXT PRIMARY KEY,
    bearer_hash TEXT NOT NULL CHECK (bearer_hash ~ '^[a-f0-9]{64}$')
);
