ALTER TABLE agent_log_bundles
  ADD COLUMN IF NOT EXISTS original_gzip_bytes BYTEA;
