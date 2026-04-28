package lineage.world.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lineage.database.DatabaseConnection;

/**
 * 에고 무기 타입/능력 허용 규칙 DB 캐시.
 *
 * ego_weapon_rule 테이블이 있으면 DB 값을 우선 사용한다.
 * use_yn=0 행도 반드시 로드해서 DB 차단 규칙이 Java fallback에 덮이지 않게 한다.
 * 테이블/값이 없으면 EgoWeaponTypeUtil의 기존 Java 규칙으로 fallback한다.
 */
public final class EgoWeaponRule {

    private static final Map<String, RuleInfo> ruleMap = new ConcurrentHashMap<String, RuleInfo>();

    private EgoWeaponRule() {
    }

    public static void reload(Connection con) {
        ruleMap.clear();
        load(con);
    }

    public static boolean hasRule(String type2) {
        if (type2 == null)
            return false;
        return ruleMap.containsKey(normalizeType(type2));
    }

    public static boolean isSupportedType(String type2) {
        RuleInfo rule = get(type2);
        return rule != null && rule.useYn;
    }

    public static String displayName(String type2, String defaultName) {
        RuleInfo rule = get(type2);
        if (rule != null && rule.displayName != null && rule.displayName.length() > 0)
            return rule.displayName;
        return defaultName;
    }

    public static String defaultAbility(String type2, String defaultAbility) {
        RuleInfo rule = get(type2);
        if (rule != null && rule.defaultAbility != null && rule.defaultAbility.length() > 0)
            return rule.defaultAbility;
        return defaultAbility;
    }

    public static boolean isAbilityAllowed(String type2, String abilityType, boolean fallback) {
        RuleInfo rule = get(type2);
        if (rule == null)
            return fallback;
        if (!rule.useYn)
            return false;
        if (abilityType == null)
            return false;
        String ability = normalizeAbility(abilityType);
        if (ability.length() == 0)
            return false;
        if (rule.allowedAbilities.length() == 0)
            return fallback;
        return containsToken(rule.allowedAbilities, ability);
    }

    public static String supportedText(String fallback) {
        if (ruleMap.isEmpty())
            return fallback;
        List<RuleInfo> rules = new ArrayList<RuleInfo>(ruleMap.values());
        Collections.sort(rules, new Comparator<RuleInfo>() {
            public int compare(RuleInfo a, RuleInfo b) {
                return a.type2.compareTo(b.type2);
            }
        });

        StringBuilder sb = new StringBuilder();
        sb.append("지원 원본 무기 타입: ");
        boolean first = true;
        for (RuleInfo rule : rules) {
            if (!rule.useYn)
                continue;
            if (!first)
                sb.append(", ");
            first = false;
            sb.append(rule.displayName.length() == 0 ? rule.type2 : rule.displayName);
            sb.append("(").append(rule.type2).append(")");
        }
        if (first)
            return fallback;
        return sb.toString();
    }

    private static RuleInfo get(String type2) {
        if (type2 == null)
            return null;
        return ruleMap.get(normalizeType(type2));
    }

    private static boolean containsToken(String source, String token) {
        if (source == null || token == null)
            return false;
        String[] arr = source.split(",");
        for (int i = 0; i < arr.length; i++) {
            if (token.equals(normalizeAbility(arr[i])))
                return true;
        }
        return false;
    }

    private static void load(Connection con) {
        PreparedStatement st = null;
        ResultSet rs = null;
        boolean closeCon = false;
        try {
            if (con == null) {
                con = DatabaseConnection.getLineage();
                closeCon = true;
            }
            st = con.prepareStatement("SELECT type2, display_name, default_ability, allowed_abilities, use_yn FROM ego_weapon_rule");
            rs = st.executeQuery();
            while (rs.next()) {
                RuleInfo rule = new RuleInfo();
                rule.type2 = normalizeType(rs.getString("type2"));
                rule.displayName = safe(rs.getString("display_name"));
                rule.defaultAbility = normalizeAbility(rs.getString("default_ability"));
                rule.allowedAbilities = normalizeAllowed(rs.getString("allowed_abilities"));
                rule.useYn = rs.getBoolean("use_yn");
                if (rule.type2.length() > 0)
                    ruleMap.put(rule.type2, rule);
            }
        } catch (Exception e) {
            // ego_weapon_rule 미적용 서버에서는 기존 Java 규칙 사용.
        } finally {
            if (closeCon)
                DatabaseConnection.close(con, st, rs);
            else
                DatabaseConnection.close(st, rs);
        }
    }

    private static String normalizeAllowed(String value) {
        String raw = safe(value).toUpperCase();
        if (raw.length() == 0)
            return "";
        StringBuilder sb = new StringBuilder();
        String[] arr = raw.split(",");
        for (int i = 0; i < arr.length; i++) {
            String ability = normalizeAbility(arr[i]);
            if (ability.length() == 0)
                continue;
            if (sb.length() > 0)
                sb.append(',');
            sb.append(ability);
        }
        return sb.toString();
    }

    private static String normalizeAbility(String value) {
        return safe(value).toUpperCase();
    }

    private static String normalizeType(String value) {
        return safe(value).toLowerCase();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class RuleInfo {
        String type2 = "";
        String displayName = "";
        String defaultAbility = "";
        String allowedAbilities = "";
        boolean useYn;
    }
}
