-- schema.sql
-- Create tables for the leaderboard service
-- Run: psql -d leaderboard_db -f schema.sql

CREATE TABLE IF NOT EXISTS users (
  id SERIAL PRIMARY KEY,
  username VARCHAR(255) UNIQUE NOT NULL,
  join_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS game_sessions (
  id SERIAL PRIMARY KEY,
  user_id INT REFERENCES users(id) ON DELETE CASCADE,
  score INT NOT NULL,
  game_mode VARCHAR(50) NOT NULL,
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS leaderboard (
  id SERIAL PRIMARY KEY,
  user_id INT REFERENCES users(id) ON DELETE CASCADE UNIQUE,
  total_score INT NOT NULL,
  rank INT
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_game_sessions_user_id ON game_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_game_sessions_timestamp ON game_sessions(timestamp);

-- Optional seed data (uncomment to use)
-- INSERT INTO users (username) VALUES ('alice'), ('bob'), ('carol');

-- Example: manual seed sessions
-- INSERT INTO game_sessions (user_id, score, game_mode) VALUES (1, 200, 'default'), (2, 300, 'default');
-- After inserting sessions you may run application logic to fill leaderboard or compute:
-- Example SQL aggregate to view totals:
-- SELECT user_id, SUM(score) AS total FROM game_sessions GROUP BY user_id ORDER BY total DESC;