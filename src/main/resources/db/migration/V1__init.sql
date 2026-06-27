CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE users (
    id         BIGSERIAL    PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    name       VARCHAR(255) NOT NULL,
    role       VARCHAR(50)  NOT NULL DEFAULT 'MEMBER',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE threads (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE chats (
    id         BIGSERIAL   PRIMARY KEY,
    thread_id  BIGINT      NOT NULL REFERENCES threads(id),
    question   TEXT        NOT NULL,
    answer     TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE feedbacks (
    id          BIGSERIAL   PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id),
    chat_id     BIGINT      NOT NULL REFERENCES chats(id),
    is_positive BOOLEAN     NOT NULL,
    status      VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, chat_id)
);

CREATE TABLE document_chunks (
    id          BIGSERIAL    PRIMARY KEY,
    content     TEXT         NOT NULL,
    embedding   vector(3072),
    source      VARCHAR(255),
    chunk_index INT
);

-- HNSW index supports max 2000 dims; gemini-embedding-2 is 3072 dims.
-- Sequential scan is sufficient for demo scale. Add IVFFlat after pgvector upgrade if needed.
