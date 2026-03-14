-- Robot behavior refine patch
-- 목적:
-- 1) 고과금(whale) 프로필 체감 강화
-- 2) 상황별 멘트 다양화(공성/사냥/PvP 전투 흐름)
-- 3) 확률 옵션 미세 조정

-- 1) whale 프로필 보정: tier + cash 기반으로 meet/cooldown 가중치 정교화
UPDATE `_robot_whale_profile`
SET
  `meet_chance` = LEAST(
      75,
      GREATEST(
          `meet_chance`,
          (8 + (`tier` * 4) + LEAST(18, FLOOR(`monthly_cash` / 900000)))
      )
  ),
  `cooldown_sec` = GREATEST(
      18,
      LEAST(
          `cooldown_sec`,
          (95 - (`tier` * 8) - LEAST(25, FLOOR(`single_purchase` / 180000)))
      )
  )
WHERE `enabled`='true';

-- 2) whale 멘트 확장 (중복 방지)
INSERT IGNORE INTO `_robot_whale_ment`
(`objId`, `trigger_type`, `ment`, `chance`, `weight`, `enabled`, `min_tier`, `max_tier`)
VALUES
(0, 'MEET', '{target}, current setup is fully invested. Keep pace if you can.', 100, 3, 'true', 2, 10),
(0, 'IDLE', 'Rotation check complete. Next push starts now.', 100, 2, 'true', 1, 10),
(0, 'ATTACK', '{target}, this account scales by spend and uptime. Trade carefully.', 100, 4, 'true', 2, 10),
(0, 'ATTACK', 'Single purchase {single_purchase} KRW. Burst window open.', 100, 3, 'true', 3, 10),
(0, 'DAMAGE', 'Damage accepted. Recovered by premium cycle.', 100, 3, 'true', 2, 10),
(0, 'KILL', '{target} cleared. Tier {tier} loop remains stable.', 100, 4, 'true', 2, 10),
(0, 'KILL', 'Spent smart, moved smart, finished clean.', 100, 2, 'true', 1, 10);

-- 3) option 미세 조정
INSERT INTO `_robot_whale_option` (`option_key`, `option_value`) VALUES
('chance_global_bonus', 3),
('idle_divider', 2),
('attack_bonus', 14),
('damage_bonus', 10),
('kill_bonus', 30),
('kill_cooldown_ignore', 1),
('chance_cap', 97)
ON DUPLICATE KEY UPDATE `option_value`=VALUES(`option_value`);
