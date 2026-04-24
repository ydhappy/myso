package ego.portable;

import java.util.List;

/**
 * 에고무기 타 서버코어 포팅용 어댑터 인터페이스.
 *
 * Java 8 호환 기준:
 * - interface default method는 Java 8부터 지원되므로 사용 가능.
 * - Java 7 이하 서버코어에 넣을 경우 default method 2개를 제거하거나 구현체에서 직접 구현해야 한다.
 * - var, record, sealed, switch expression, module-info.java 사용 금지.
 *
 * 목적:
 * - 기존 myso용 에고 코드는 lineage.* 클래스에 직접 의존한다.
 * - 다른 서버코어에서는 이 인터페이스를 구현해서 코어별 클래스 차이를 흡수한다.
 * - 에고 판단 로직은 Object 기반으로 동작하고, 실제 캐릭터/아이템/몬스터 접근은 어댑터가 담당한다.
 */
public interface EgoCoreAdapter {

    // 공통 객체
    long getObjectId(Object obj);
    String getName(Object obj);
    boolean isDead(Object obj);
    boolean isDeleted(Object obj);
    int getX(Object obj);
    int getY(Object obj);
    int getMapId(Object obj);
    int getDistance(Object a, Object b);
    boolean isDistance(Object a, Object b, int range);

    // 플레이어
    boolean isPlayer(Object obj);
    boolean isRobotPlayer(Object obj);
    int getLevel(Object player);
    int getNowHp(Object player);
    int getMaxHp(Object player);
    int getNowMp(Object player);
    int getMaxMp(Object player);
    int getClassType(Object player);
    String getClassName(Object player);
    int getClanId(Object player);
    String getClanName(Object player);
    String getTitle(Object player);
    int getLawful(Object player);
    int getNeutralLawfulValue();
    int getPkCount(Object player);
    Object getTarget(Object player);
    void setTarget(Object player, Object target);
    List<?> getVisibleObjects(Object player);
    void sendSystemMessage(Object player, String message);

    // 인벤토리/무기
    Object getEquippedWeapon(Object player);
    boolean isWeaponSlot(Object item);
    String getItemName(Object item);
    String getItemType2(Object item);
    int getEnchantLevel(Object item);
    long getItemObjectId(Object item);

    // 몬스터
    boolean isMonster(Object obj);
    boolean isAggressiveMonster(Object monster);
    boolean isBossMonster(Object monster);
    long getMonsterExp(Object monster);

    // 전투/제어
    void startAutoAttack(Object player, Object target);
    void stopAutoAttack(Object player);
    void healHp(Object player, int amount);
    void healMp(Object player, int amount);
    void damageMonster(Object attacker, Object monster, int damage);
    void sendEffect(Object target, int effectId);

    // 선택 기능
    default boolean isPvpAllowed(Object player, Object target) {
        return true;
    }

    default boolean canDetect(Object scanner, Object target) {
        return true;
    }
}
