CREATE TABLE entitlements (
    install_id TEXT PRIMARY KEY,
    package_name TEXT NOT NULL,
    product_id TEXT NOT NULL,
    purchase_token_hash TEXT NOT NULL UNIQUE,
    state TEXT NOT NULL,
    active_until_millis BIGINT,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    last_verified_at_millis BIGINT NOT NULL,
    app_version TEXT,
    created_at_millis BIGINT NOT NULL,
    updated_at_millis BIGINT NOT NULL
);

CREATE INDEX entitlements_purchase_token_hash_idx
    ON entitlements (purchase_token_hash);

CREATE INDEX entitlements_state_idx
    ON entitlements (state);
