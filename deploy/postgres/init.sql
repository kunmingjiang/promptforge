-- ============================================================
-- 铸语 PromptForge · 数据库初始化脚本（PostgreSQL 15）
-- 挂载到 /docker-entrypoint-initdb.d/ 首次启动自动执行
-- 约定：主键 BIGSERIAL；时间统一 TIMESTAMP；逻辑外键级联删除；
--       audit_logs.user_id 不设外键——审计记录必须在用户删除后仍然留存
-- ============================================================

-- 1. 用户表
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(64)  NOT NULL,
    password    VARCHAR(100) NOT NULL,              -- BCrypt 哈希，60 字符
    email       VARCHAR(128),
    status      SMALLINT     NOT NULL DEFAULT 1,    -- 1=启用 0=禁用
    created_at  TIMESTAMP  NOT NULL DEFAULT now(),
    CONSTRAINT uk_users_username UNIQUE (username)
);
COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.password    IS 'BCrypt 加密的登录密码，禁止存明文';
COMMENT ON COLUMN users.status      IS '账号状态：1 启用，0 禁用';

-- 2. 角色表
CREATE TABLE roles (
    id   BIGSERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    CONSTRAINT uk_roles_code UNIQUE (code)
);
COMMENT ON TABLE roles IS '角色表：ADMIN 管理员 / USER 普通用户';

-- 3. 用户-角色关联表
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);
COMMENT ON TABLE user_roles IS '用户与角色的多对多关联';

-- 4. 模型配置表（管理后台维护，仅一行生效配置）
CREATE TABLE model_configs (
    id                BIGSERIAL PRIMARY KEY,
    provider          VARCHAR(32)  NOT NULL,          -- deepseek（预留扩展：openai/ollama 等）
    base_url          VARCHAR(256) NOT NULL,
    api_key_encrypted VARCHAR(512) NOT NULL,          -- AES-256-GCM 密文，禁止明文落库
    model_name        VARCHAR(64)  NOT NULL,
    enabled           BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP  NOT NULL DEFAULT now()
);
COMMENT ON TABLE model_configs IS '大模型配置：管理员在后台填写，API Key 加密存储';
COMMENT ON COLUMN model_configs.api_key_encrypted IS 'AES-256-GCM 加密后的 API Key，密钥来自环境变量 ENCRYPT_KEY';

-- 5. 会话表（两个模块共用：prompt=提示词生成，requirement=需求助手）
CREATE TABLE conversations (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title      VARCHAR(128) NOT NULL,
    module     VARCHAR(32)  NOT NULL,
    created_at TIMESTAMP  NOT NULL DEFAULT now()
);
COMMENT ON TABLE conversations IS '会话表：module 区分来源模块';
CREATE INDEX idx_conversations_user ON conversations (user_id, created_at DESC);
CREATE INDEX idx_conversations_module ON conversations (module);

-- 6. 消息表
CREATE TABLE messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT      NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    role            VARCHAR(16) NOT NULL,            -- user / assistant
    content         TEXT        NOT NULL,
    tokens          INTEGER,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
COMMENT ON TABLE messages IS '会话消息：一轮对话的问答明细';
CREATE INDEX idx_messages_conversation ON messages (conversation_id, created_at);

-- 7. 会话版本表（提示词生成的历史版本快照）
CREATE TABLE conversation_versions (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT      NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    version         INTEGER     NOT NULL,
    snapshot        TEXT        NOT NULL,            -- JSON：生成时刻的消息快照
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_conversation_version UNIQUE (conversation_id, version)
);
COMMENT ON TABLE conversation_versions IS '会话版本：每次生成完成保存一份消息快照，用于历史回看';

-- 8. 需求项目表
CREATE TABLE requirement_projects (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title          VARCHAR(128) NOT NULL,
    raw_requirement TEXT        NOT NULL,
    status         VARCHAR(32)  NOT NULL DEFAULT 'PENDING', -- PENDING/CLARIFYING/ANSWERED/DOCUMENTED
    created_at     TIMESTAMP  NOT NULL DEFAULT now()
);
COMMENT ON TABLE requirement_projects IS '需求助手项目：三步流程（识别→澄清→文档）';
CREATE INDEX idx_requirement_projects_user ON requirement_projects (user_id, created_at DESC);

-- 9. 需求澄清问答表
CREATE TABLE requirement_answers (
    id         BIGSERIAL PRIMARY KEY,
    project_id BIGINT       NOT NULL REFERENCES requirement_projects (id) ON DELETE CASCADE,
    question   TEXT         NOT NULL,
    answer     TEXT,
    skipped    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP  NOT NULL DEFAULT now()
);
COMMENT ON TABLE requirement_answers IS 'AI 生成的澄清问题及用户回答，最多 5 条';
CREATE INDEX idx_requirement_answers_project ON requirement_answers (project_id);

-- 10. 需求文档表（每个项目一份当前文档）
CREATE TABLE requirement_documents (
    id         BIGSERIAL PRIMARY KEY,
    project_id BIGINT      NOT NULL REFERENCES requirement_projects (id) ON DELETE CASCADE,
    content    TEXT        NOT NULL,
    version    INTEGER     NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_requirement_document_project UNIQUE (project_id)
);
COMMENT ON TABLE requirement_documents IS '需求文档当前版本；历史版本见 requirement_document_versions';
CREATE INDEX idx_requirement_documents_project ON requirement_documents (project_id);

-- 11. 需求文档版本表
CREATE TABLE requirement_document_versions (
    id          BIGSERIAL PRIMARY KEY,
    document_id BIGINT      NOT NULL REFERENCES requirement_documents (id) ON DELETE CASCADE,
    version     INTEGER     NOT NULL,
    content     TEXT        NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_requirement_doc_version UNIQUE (document_id, version)
);
COMMENT ON TABLE requirement_document_versions IS '需求文档历史版本，支持版本切换回看';

-- 12. 审计日志表
CREATE TABLE audit_logs (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT,
    action     VARCHAR(64) NOT NULL,
    target     VARCHAR(256),
    ip         VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
COMMENT ON TABLE audit_logs IS '操作审计日志：不设 user_id 外键，保证用户删除后留痕不丢失';
CREATE INDEX idx_audit_logs_user ON audit_logs (user_id, created_at DESC);
CREATE INDEX idx_audit_logs_created ON audit_logs (created_at DESC);

-- ============================================================
-- 预置数据
-- ============================================================
INSERT INTO roles (code, name) VALUES ('ADMIN', '管理员') ON CONFLICT (code) DO NOTHING;
INSERT INTO roles (code, name) VALUES ('USER', '普通用户') ON CONFLICT (code) DO NOTHING;

INSERT INTO model_configs (provider, base_url, api_key_encrypted, model_name, enabled)
VALUES ('deepseek', 'https://api.deepseek.com', '', 'deepseek-chat', FALSE);
