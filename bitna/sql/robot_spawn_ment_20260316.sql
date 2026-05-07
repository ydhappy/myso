-- Robot whale SPAWN trigger addition
-- 목적:
-- 1) _robot_whale_ment trigger_type enum에 SPAWN 추가
-- 2) 스폰 시 멘트 등록 (준비되었습니까?)
-- 3) spawn_bonus 옵션 등록

-- 1) trigger_type enum에 SPAWN 추가
ALTER TABLE `_robot_whale_ment`
  MODIFY COLUMN `trigger_type` enum('MEET','IDLE','ATTACK','DAMAGE','KILL','SPAWN') NOT NULL DEFAULT 'MEET';

-- 2) SPAWN 멘트 등록 (스폰/부활 시 출력)
INSERT IGNORE INTO `_robot_whale_ment`
(`objId`, `trigger_type`, `ment`, `chance`, `weight`, `enabled`, `min_tier`, `max_tier`)
VALUES
(0, 'SPAWN', '준비되었습니까?', 100, 5, 'true', 1, 10),
(0, 'SPAWN', 'Tier {tier} account online. Ready to farm.', 100, 4, 'true', 2, 10),
(0, 'SPAWN', '{vip} reporting in. Hunt cycle begins now.', 100, 3, 'true', 2, 10),
(0, 'SPAWN', 'Back online. Premium account, premium results.', 100, 3, 'true', 1, 10),
(0, 'SPAWN', 'Monthly budget {monthly_cash} KRW. Full rotation start.', 100, 2, 'true', 3, 10);

-- 3) spawn_bonus 옵션 등록 (이미 존재하면 값 갱신)
INSERT INTO `_robot_whale_option` (`option_key`, `option_value`) VALUES
('spawn_bonus', 50)
ON DUPLICATE KEY UPDATE `option_value`=VALUES(`option_value`);
