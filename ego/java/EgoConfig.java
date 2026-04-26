package lineage.world.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lineage.database.DatabaseConnection;

/**
 * 에고 DB 설정 헬퍼.
 *
 * ego_config 테이블이 있으면 DB 값을 우선 사용한다.
 * 테이블/값이 없으면 Java 기본값으로 fallback한다.
 */
public final class EgoConfig {

    private static final Map<String, String> configMap = new ConcurrentHashMap<String, String>();

    private EgoConfig() {
    }

    public static void reload(Connection con) {
        configMap.clear();
        load(con);
    }

    public static String getString(String key, String defaultValue) {
        if (key == null)
            return defaultValue;
        String value = configMap.get(key.trim());
        if (value == null || value.trim().length() == 0)
            return defaultValue;
        return value.trim();
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(getString(key, String.valueOf(defaultValue)));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static long getLong(String key, long defaultValue) {
        try {
            return Long.parseLong(getString(key, String.valueOf(defaultValue)));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key, defaultValue ? "1" : "0");
        return "1".equals(value) || "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value);
    }

    public static int percent(String key, int defaultValue) {
        int value = getInt(key, defaultValue);
        if (value < 0)
            return 0;
        if (value > 100)
            return 100;
        return value;
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
            st = con.prepareStatement("SELECT config_key, config_value FROM ego_config WHERE use_yn=1");
            rs = st.executeQuery();
            while (rs.next()) {
                String key = rs.getString("config_key");
                String value = rs.getString("config_value");
                if (key != null && key.trim().length() > 0)
                    configMap.put(key.trim(), value == null ? "" : value.trim());
            }
        } catch (Exception e) {
            // ego_config 미적용 서버에서도 Java 기본값으로 동작한다.
        } finally {
            if (closeCon)
                DatabaseConnection.close(con, st, rs);
            else
                DatabaseConnection.close(st, rs);
        }
    }
}
