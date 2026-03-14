-- Robot whale profile + ment upgrade
-- Source concept: shashapak tb_mjbot_brain / tb_mjbot_ment style SQL-driven bot behavior

CREATE TABLE IF NOT EXISTS `_robot_whale_profile` (
  `objId` int(10) NOT NULL,
  `enabled` enum('true','false') NOT NULL DEFAULT 'true',
  `tier` tinyint(3) unsigned NOT NULL DEFAULT 1,
  `monthly_cash` bigint(20) unsigned NOT NULL DEFAULT 0,
  `single_purchase` bigint(20) unsigned NOT NULL DEFAULT 0,
  `weapon_enchant_bonus` tinyint(3) unsigned NOT NULL DEFAULT 0,
  `ac_bonus` tinyint(3) unsigned NOT NULL DEFAULT 0,
  `mr_bonus` tinyint(3) unsigned NOT NULL DEFAULT 0,
  `sp_bonus` tinyint(3) unsigned NOT NULL DEFAULT 0,
  `hp_bonus` int(10) unsigned NOT NULL DEFAULT 0,
  `mp_bonus` int(10) unsigned NOT NULL DEFAULT 0,
  `meet_chance` tinyint(3) unsigned NOT NULL DEFAULT 15,
  `cooldown_sec` int(10) unsigned NOT NULL DEFAULT 90,
  `force_action` varchar(32) NOT NULL DEFAULT '',
  `force_title` varchar(40) NOT NULL DEFAULT '',
  `force_clan` varchar(40) NOT NULL DEFAULT '',
  `force_weapon_name` varchar(255) NOT NULL DEFAULT '',
  `force_doll_name` varchar(255) NOT NULL DEFAULT '',
  `force_mythic_poly` enum('inherit','random','true') NOT NULL DEFAULT 'inherit',
  PRIMARY KEY (`objId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

INSERT INTO `_robot_whale_profile` (
  `objId`, `enabled`, `tier`, `monthly_cash`, `single_purchase`,
  `weapon_enchant_bonus`, `ac_bonus`, `mr_bonus`, `sp_bonus`, `hp_bonus`, `mp_bonus`,
  `meet_chance`, `cooldown_sec`, `force_action`, `force_title`, `force_clan`,
  `force_weapon_name`, `force_doll_name`, `force_mythic_poly`
)
SELECT
  b.objId,
  'true' AS enabled,
  b.tier,
  (b.tier * 450000) + (MOD(b.objId, 37) * 60000) AS monthly_cash,
  (b.tier * 70000) + (MOD(b.objId, 11) * 9000) AS single_purchase,
  CASE WHEN b.tier >= 5 THEN 5 WHEN b.tier = 4 THEN 4 WHEN b.tier = 3 THEN 3 WHEN b.tier = 2 THEN 2 ELSE 1 END AS weapon_enchant_bonus,
  CASE WHEN b.tier >= 5 THEN 7 WHEN b.tier = 4 THEN 5 WHEN b.tier = 3 THEN 3 WHEN b.tier = 2 THEN 2 ELSE 1 END AS ac_bonus,
  CASE WHEN b.tier >= 5 THEN 20 WHEN b.tier = 4 THEN 15 WHEN b.tier = 3 THEN 10 WHEN b.tier = 2 THEN 6 ELSE 3 END AS mr_bonus,
  CASE WHEN b.tier >= 5 THEN 4 WHEN b.tier = 4 THEN 3 WHEN b.tier = 3 THEN 2 WHEN b.tier = 2 THEN 1 ELSE 1 END AS sp_bonus,
  CASE WHEN b.tier >= 5 THEN 450 WHEN b.tier = 4 THEN 300 WHEN b.tier = 3 THEN 200 WHEN b.tier = 2 THEN 120 ELSE 60 END AS hp_bonus,
  CASE WHEN b.tier >= 5 THEN 220 WHEN b.tier = 4 THEN 160 WHEN b.tier = 3 THEN 110 WHEN b.tier = 2 THEN 70 ELSE 40 END AS mp_bonus,
  CASE WHEN b.tier >= 5 THEN 34 WHEN b.tier = 4 THEN 28 WHEN b.tier = 3 THEN 22 WHEN b.tier = 2 THEN 16 ELSE 12 END AS meet_chance,
  CASE WHEN b.tier >= 5 THEN 35 WHEN b.tier = 4 THEN 45 WHEN b.tier = 3 THEN 60 WHEN b.tier = 2 THEN 75 ELSE 90 END AS cooldown_sec,
  '' AS force_action,
  CONCAT('[VIP-T', b.tier, ']') AS force_title,
  '' AS force_clan,
  '' AS force_weapon_name,
  '' AS force_doll_name,
  CASE WHEN b.tier >= 4 THEN 'true' WHEN b.tier = 3 THEN 'random' ELSE 'inherit' END AS force_mythic_poly
FROM (
  SELECT
    r.objId,
    CASE
      WHEN r.level >= 80 THEN 5
      WHEN r.level >= 70 THEN 4
      WHEN r.level >= 60 THEN 3
      WHEN r.level >= 50 THEN 2
      ELSE 1
    END AS tier
  FROM `_robot` r
  WHERE r.objId >= 1900000
) b
ON DUPLICATE KEY UPDATE
  `enabled` = VALUES(`enabled`),
  `tier` = VALUES(`tier`),
  `monthly_cash` = VALUES(`monthly_cash`),
  `single_purchase` = VALUES(`single_purchase`),
  `weapon_enchant_bonus` = VALUES(`weapon_enchant_bonus`),
  `ac_bonus` = VALUES(`ac_bonus`),
  `mr_bonus` = VALUES(`mr_bonus`),
  `sp_bonus` = VALUES(`sp_bonus`),
  `hp_bonus` = VALUES(`hp_bonus`),
  `mp_bonus` = VALUES(`mp_bonus`),
  `meet_chance` = VALUES(`meet_chance`),
  `cooldown_sec` = VALUES(`cooldown_sec`),
  `force_action` = VALUES(`force_action`),
  `force_title` = VALUES(`force_title`),
  `force_clan` = VALUES(`force_clan`),
  `force_weapon_name` = VALUES(`force_weapon_name`),
  `force_doll_name` = VALUES(`force_doll_name`),
  `force_mythic_poly` = VALUES(`force_mythic_poly`);

CREATE TABLE IF NOT EXISTS `_robot_whale_ment` (
  `uid` int(10) NOT NULL AUTO_INCREMENT,
  `objId` int(10) NOT NULL DEFAULT 0,
  `trigger_type` enum('MEET','IDLE','ATTACK','DAMAGE','KILL') NOT NULL DEFAULT 'MEET',
  `ment` varchar(255) NOT NULL,
  `chance` tinyint(3) unsigned NOT NULL DEFAULT 100,
  `weight` tinyint(3) unsigned NOT NULL DEFAULT 1,
  `enabled` enum('true','false') NOT NULL DEFAULT 'true',
  `min_tier` tinyint(3) unsigned NOT NULL DEFAULT 1,
  `max_tier` tinyint(3) unsigned NOT NULL DEFAULT 10,
  PRIMARY KEY (`uid`),
  UNIQUE KEY `uq_robot_whale_ment` (`objId`, `trigger_type`, `ment`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

INSERT IGNORE INTO `_robot_whale_ment` (`objId`, `trigger_type`, `ment`, `chance`, `weight`, `enabled`, `min_tier`, `max_tier`) VALUES
(0, 'MEET', '{target}, monthly topup is {monthly_cash} KRW. Ready to speed-farm?', 100, 5, 'true', 2, 10),
(0, 'MEET', 'VIP {vip} online. Single purchase today was {single_purchase} KRW.', 100, 4, 'true', 3, 10),
(0, 'MEET', 'No budget cap this week. Tier {tier} account is fully loaded.', 100, 3, 'true', 2, 10),
(0, 'MEET', 'Package opened again. {target}, let''s rotate faster.', 100, 3, 'true', 1, 10),
(0, 'IDLE', 'Market checked. Next charge planned after this hunt cycle.', 100, 4, 'true', 1, 10),
(0, 'IDLE', 'Premium consumables restocked. Keep pushing.', 100, 3, 'true', 1, 10),
(0, 'IDLE', 'Tier {tier} routine: hunt, recharge, upgrade, repeat.', 100, 2, 'true', 2, 10),
(0, 'ATTACK', '{target}, this build is funded. Let''s test your limit.', 100, 5, 'true', 2, 10),
(0, 'ATTACK', 'Spent {single_purchase} KRW in one shot. Damage check now.', 100, 4, 'true', 3, 10),
(0, 'ATTACK', 'No downgrade mindset. Full-commit rotation only.', 100, 3, 'true', 1, 10),
(0, 'ATTACK', 'Whale tempo online. {target}, trade hard or move.', 100, 3, 'true', 2, 10),
(0, 'DAMAGE', 'Nice hit. I just recharge and push back harder.', 100, 4, 'true', 1, 10),
(0, 'DAMAGE', '{target}, that was expensive gear. Try again.', 100, 3, 'true', 2, 10),
(0, 'KILL', '{target} down. VIP rotation stays clean.', 100, 5, 'true', 2, 10),
(0, 'KILL', 'Another clear. Monthly spend {monthly_cash} KRW is doing work.', 100, 4, 'true', 3, 10);

CREATE TABLE IF NOT EXISTS `_robot_whale_option` (
  `option_key` varchar(64) NOT NULL,
  `option_value` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`option_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

INSERT INTO `_robot_whale_option` (`option_key`, `option_value`) VALUES
('chance_global_bonus', 0),
('idle_divider', 3),
('attack_bonus', 10),
('damage_bonus', 8),
('kill_bonus', 25),
('kill_cooldown_ignore', 1),
('chance_cap', 95)
ON DUPLICATE KEY UPDATE `option_value` = VALUES(`option_value`);
