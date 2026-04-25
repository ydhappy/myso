-- ============================================================
-- 에고무기 한글 테이블 설치 SQL
-- Java 8 / MySQL 또는 MariaDB / UTF-8
-- ============================================================

SET NAMES utf8;

CREATE TABLE IF NOT EXISTS `에고` (
    `아이템번호` BIGINT NOT NULL COMMENT '에고무기 아이템 objectId',
    `캐릭터번호` BIGINT NOT NULL DEFAULT 0 COMMENT '소유 캐릭터 objectId',
    `사용` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '사용 여부',
    `이름` VARCHAR(50) NOT NULL DEFAULT '에고' COMMENT '호출 이름',
    `성격` VARCHAR(30) NOT NULL DEFAULT '수호' COMMENT '수호/광전/냉정/현자',
    `레벨` INT NOT NULL DEFAULT 1 COMMENT '에고 레벨',
    `경험치` BIGINT NOT NULL DEFAULT 0 COMMENT '현재 경험치',
    `필요경험치` BIGINT NOT NULL DEFAULT 100 COMMENT '다음 레벨 필요 경험치',
    `대화단계` INT NOT NULL DEFAULT 1 COMMENT '대화 단계',
    `제어단계` INT NOT NULL DEFAULT 1 COMMENT '제어 단계',
    `마지막대화` BIGINT NOT NULL DEFAULT 0 COMMENT '마지막 대화 시간',
    `마지막경고` BIGINT NOT NULL DEFAULT 0 COMMENT '마지막 경고 시간',
    `형태` VARCHAR(40) NOT NULL DEFAULT '' COMMENT 'dagger/sword/tohandsword/axe/spear/bow/staff/wand',
    `이전방패` BIGINT NOT NULL DEFAULT 0 COMMENT '자동 해제한 방패 objectId',
    `생성일` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `수정일` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`아이템번호`),
    INDEX `캐릭터번호_idx` (`캐릭터번호`),
    INDEX `이름_idx` (`이름`),
    INDEX `형태_idx` (`형태`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='에고무기 기본 정보';

CREATE TABLE IF NOT EXISTS `에고능력` (
    `번호` BIGINT NOT NULL AUTO_INCREMENT,
    `아이템번호` BIGINT NOT NULL COMMENT '에고무기 아이템 objectId',
    `능력` VARCHAR(40) NOT NULL COMMENT '능력 코드',
    `레벨` INT NOT NULL DEFAULT 1 COMMENT '능력 레벨',
    `확률보너스` INT NOT NULL DEFAULT 0 COMMENT '추가 확률',
    `피해보너스` INT NOT NULL DEFAULT 0 COMMENT '추가 피해',
    `마지막발동` BIGINT NOT NULL DEFAULT 0 COMMENT '마지막 발동 시간',
    `사용` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사용 여부',
    `생성일` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `수정일` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`번호`),
    UNIQUE KEY `아이템능력_uk` (`아이템번호`, `능력`),
    INDEX `아이템번호_idx` (`아이템번호`),
    INDEX `능력_idx` (`능력`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='에고무기 능력 정보';

CREATE TABLE IF NOT EXISTS `에고성격` (
    `성격` VARCHAR(30) NOT NULL,
    `표시명` VARCHAR(50) NOT NULL,
    `설명` VARCHAR(255) NOT NULL DEFAULT '',
    `위험체력` INT NOT NULL DEFAULT 30,
    `자동공격` TINYINT(1) NOT NULL DEFAULT 1,
    `자동경고` TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (`성격`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='에고 성격 기본값';

INSERT INTO `에고성격`
(`성격`, `표시명`, `설명`, `위험체력`, `자동공격`, `자동경고`)
VALUES
('수호', '수호형', '생존을 우선하는 안정형', 35, 1, 1),
('광전', '광전형', '공격을 우선하는 전투형', 25, 1, 1),
('냉정', '냉정형', '짧고 정확하게 판단하는 침착형', 30, 1, 1),
('현자', '현자형', '분석과 조언을 중시하는 분석형', 40, 1, 1)
ON DUPLICATE KEY UPDATE
    `표시명` = VALUES(`표시명`),
    `설명` = VALUES(`설명`),
    `위험체력` = VALUES(`위험체력`),
    `자동공격` = VALUES(`자동공격`),
    `자동경고` = VALUES(`자동경고`);

CREATE TABLE IF NOT EXISTS `에고대화` (
    `번호` INT NOT NULL AUTO_INCREMENT,
    `성격` VARCHAR(30) NOT NULL DEFAULT '수호',
    `명령` VARCHAR(100) NOT NULL COMMENT '반응 명령',
    `대사` TEXT NOT NULL COMMENT '응답 대사',
    `최소레벨` INT NOT NULL DEFAULT 1,
    `사용` TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (`번호`),
    INDEX `성격_idx` (`성격`),
    INDEX `명령_idx` (`명령`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='에고 대화 기본값';

CREATE TABLE IF NOT EXISTS `에고능력기본` (
    `능력` VARCHAR(40) NOT NULL COMMENT '능력 코드',
    `이름` VARCHAR(50) NOT NULL COMMENT '표시명',
    `설명` VARCHAR(255) NOT NULL DEFAULT '',
    `기본확률` INT NOT NULL DEFAULT 3,
    `레벨확률` INT NOT NULL DEFAULT 1,
    `최대확률` INT NOT NULL DEFAULT 25,
    `최소레벨` INT NOT NULL DEFAULT 1,
    `쿨타임` INT NOT NULL DEFAULT 0,
    `이펙트` INT NOT NULL DEFAULT 0,
    `사용` TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (`능력`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='에고 능력 기본값';

INSERT INTO `에고능력기본`
(`능력`, `이름`, `설명`, `기본확률`, `레벨확률`, `최대확률`, `최소레벨`, `쿨타임`, `이펙트`, `사용`)
VALUES
('EGO_BALANCE', '공명', '균형형 추가 피해', 3, 1, 25, 1, 0, 3940, 1),
('BLOOD_DRAIN', '흡혈', 'HP 회복', 3, 1, 20, 1, 0, 8150, 1),
('MANA_DRAIN', '흡마', 'MP 회복', 3, 1, 20, 1, 0, 7300, 1),
('CRITICAL_BURST', '치명', '강한 추가 피해', 2, 1, 20, 3, 0, 12487, 1),
('GUARDIAN_SHIELD', '수호', '위험 시 HP 회복', 3, 1, 20, 2, 3000, 6321, 1),
('AREA_SLASH', '광역', '주변 몬스터 피해', 2, 1, 15, 5, 3000, 12248, 1),
('EXECUTION', '처형', '약한 대상 추가 피해', 2, 1, 15, 7, 0, 8683, 1),
('FLAME_BRAND', '화염', '화염 추가 피해', 3, 1, 20, 1, 0, 1811, 1),
('FROST_BIND', '서리', '서리 추가 피해', 3, 1, 20, 1, 0, 3684, 1)
ON DUPLICATE KEY UPDATE
    `이름` = VALUES(`이름`),
    `설명` = VALUES(`설명`),
    `기본확률` = VALUES(`기본확률`),
    `레벨확률` = VALUES(`레벨확률`),
    `최대확률` = VALUES(`최대확률`),
    `최소레벨` = VALUES(`최소레벨`),
    `쿨타임` = VALUES(`쿨타임`),
    `이펙트` = VALUES(`이펙트`),
    `사용` = VALUES(`사용`);

CREATE TABLE IF NOT EXISTS `에고기록` (
    `번호` BIGINT NOT NULL AUTO_INCREMENT,
    `아이템번호` BIGINT NOT NULL DEFAULT 0,
    `캐릭터번호` BIGINT NOT NULL DEFAULT 0,
    `캐릭터명` VARCHAR(50) NOT NULL DEFAULT '',
    `대상명` VARCHAR(80) NOT NULL DEFAULT '',
    `능력` VARCHAR(40) NOT NULL DEFAULT '',
    `기본피해` INT NOT NULL DEFAULT 0,
    `최종피해` INT NOT NULL DEFAULT 0,
    `추가피해` INT NOT NULL DEFAULT 0,
    `기록일` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`번호`),
    INDEX `아이템번호_idx` (`아이템번호`),
    INDEX `캐릭터번호_idx` (`캐릭터번호`),
    INDEX `기록일_idx` (`기록일`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='에고 능력 발동 기록';

SELECT '에고 한글 테이블 설치 완료' AS result;
SHOW TABLES LIKE '에고';
SHOW TABLES LIKE '에고능력';
SHOW TABLES LIKE '에고성격';
SHOW TABLES LIKE '에고대화';
SHOW TABLES LIKE '에고능력기본';
SHOW TABLES LIKE '에고기록';
