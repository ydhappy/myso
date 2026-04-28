package lineage.world.controller;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 에고 DB 스키마 중앙 연결표/검증기.
 *
 * 목적:
 * - 테이블명/컬럼명 기준을 한곳에 모은다.
 * - Java 코드와 SQL 스키마 연결 누락을 빠르게 확인한다.
 * - 서버 시작/리로드 시 선택적으로 검증할 수 있다.
 *
 * 기준 SQL:
 * - ego/sql/ego_schema.sql
 *
 * 정책:
 * - 신규/기존 서버 모두 ego_schema.sql 1개만 적용한다.
 * - 원클릭 전체삭제/전체초기화 SQL은 제공하지 않는다.
 * - 구버전 테이블은 fallback으로만 허용한다.
 */
public final class EgoSchema {

    public static final String T_EGO = "ego";
    public static final String T_EGO_SKILL = "ego_skill";
    public static final String T_EGO_SKILL_BASE = "ego_skill_base";
    public static final String T_EGO_LOG = "ego_log";
    public static final String T_EGO_TALK_PACK = "ego_talk_pack";
    public static final String T_EGO_CONFIG = "ego_config";
    public static final String T_EGO_LEVEL = "ego_level";
    public static final String T_EGO_WEAPON_RULE = "ego_weapon_rule";

    private static final Map<String, String[]> REQUIRED = new LinkedHashMap<String, String[]>();

    static {
        REQUIRED.put(T_EGO, new String[] {
            "item_id", "char_id", "use_yn", "ego_name", "ego_type", "ego_lv", "ego_exp", "need_exp",
            "talk_lv", "ctrl_lv", "last_talk", "last_warn", "bond", "bond_reason", "reg_date", "mod_date"
        });
        REQUIRED.put(T_EGO_SKILL, new String[] {
            "id", "item_id", "skill", "skill_lv", "rate_bonus", "dmg_bonus", "last_proc", "use_yn", "reg_date", "mod_date"
        });
        REQUIRED.put(T_EGO_SKILL_BASE, new String[] {
            "skill", "label", "memo", "base_rate", "lv_rate", "max_rate", "min_lv", "cool_ms", "effect", "use_yn"
        });
        REQUIRED.put(T_EGO_LOG, new String[] {
            "id", "item_id", "char_id", "char_name", "target_name", "skill", "base_dmg", "final_dmg", "add_dmg", "reg_date"
        });
        REQUIRED.put(T_EGO_TALK_PACK, new String[] {
            "id", "genre", "tone", "keyword", "message", "use_yn", "reg_date", "mod_date"
        });
        REQUIRED.put(T_EGO_CONFIG, new String[] {
            "config_key", "config_value", "memo", "use_yn", "reg_date", "mod_date"
        });
        REQUIRED.put(T_EGO_LEVEL, new String[] {
            "ego_lv", "need_exp", "proc_bonus", "critical_chance", "critical_damage", "counter_chance", "counter_power", "counter_critical",
            "memo", "use_yn", "reg_date", "mod_date"
        });
        REQUIRED.put(T_EGO_WEAPON_RULE, new String[] {
            "type2", "display_name", "default_ability", "allowed_abilities", "use_yn", "reg_date", "mod_date"
        });
    }

    private EgoSchema() {
    }

    public static boolean isValid(Connection con) {
        return validate(con).ok;
    }

    public static String report(Connection con) {
        return validate(con).message;
    }

    public static void silentCheck(Connection con) {
        try {
            validate(con);
        } catch (Throwable e) {
        }
    }

    private static Result validate(Connection con) {
        Result result = new Result();
        StringBuilder sb = new StringBuilder();
        sb.append("[에고 스키마 연결성 점검]\n");
        sb.append("기준 SQL: ego/sql/ego_schema.sql\n");

        if (con == null) {
            result.ok = false;
            sb.append("FAIL: Connection is null\n");
            result.message = sb.toString();
            return result;
        }

        try {
            DatabaseMetaData meta = con.getMetaData();
            boolean allOk = true;

            for (Map.Entry<String, String[]> e : REQUIRED.entrySet()) {
                String table = e.getKey();
                if (!tableExists(meta, table)) {
                    allOk = false;
                    sb.append("MISSING TABLE: ").append(table).append('\n');
                    continue;
                }
                sb.append("OK TABLE: ").append(table).append('\n');

                String[] cols = e.getValue();
                for (int i = 0; i < cols.length; i++) {
                    String col = cols[i];
                    if (!columnExists(meta, table, col)) {
                        allOk = false;
                        sb.append("  MISSING COLUMN: ").append(table).append('.').append(col).append('\n');
                    }
                }
            }

            if (allOk)
                sb.append("RESULT: OK\n");
            else
                sb.append("RESULT: FAIL - ego/sql/ego_schema.sql 적용 후 .에고리로드 필요\n");

            result.ok = allOk;
            result.message = sb.toString();
            return result;
        } catch (Exception ex) {
            result.ok = false;
            sb.append("FAIL: ").append(ex.getMessage()).append('\n');
            result.message = sb.toString();
            return result;
        }
    }

    private static boolean tableExists(DatabaseMetaData meta, String table) throws Exception {
        ResultSet rs = null;
        try {
            rs = meta.getTables(null, null, table, null);
            if (rs != null && rs.next())
                return true;
        } finally {
            close(rs);
        }
        try {
            rs = meta.getTables(null, null, table.toUpperCase(), null);
            return rs != null && rs.next();
        } finally {
            close(rs);
        }
    }

    private static boolean columnExists(DatabaseMetaData meta, String table, String column) throws Exception {
        ResultSet rs = null;
        try {
            rs = meta.getColumns(null, null, table, column);
            if (rs != null && rs.next())
                return true;
        } finally {
            close(rs);
        }
        try {
            rs = meta.getColumns(null, null, table, column.toUpperCase());
            return rs != null && rs.next();
        } finally {
            close(rs);
        }
    }

    private static void close(ResultSet rs) {
        try {
            if (rs != null)
                rs.close();
        } catch (Exception e) {
        }
    }

    private static final class Result {
        boolean ok;
        String message;
    }
}
