-- 移除 characters 表的 game_id 和 note 欄位
ALTER TABLE characters DROP COLUMN IF EXISTS game_id;
ALTER TABLE characters DROP COLUMN IF EXISTS note;
