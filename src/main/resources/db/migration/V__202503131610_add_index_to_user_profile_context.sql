--liquibase formatted sql
--changeset wayneyu:V__202503131610_add_index_to_user_profile_context.sql splitStatements:true endDelimiter:;
 
CREATE INDEX idx_user_profiles_context ON user_profiles USING gin (context jsonb_path_ops);
