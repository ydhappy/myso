package lineage.world.controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lineage.bean.lineage.Inventory;
import lineage.database.EgoWeaponDatabase;
import lineage.share.Lineage;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;

/**
 * 짧은 이름용 에고 대화 클래스.
 *
 * ChattingController 연결 시 EgoWeaponControlController 대신 EgoTalk 사용을 권장합니다.
 */
public final class EgoTalk {

    private static final long GENRE_TALK_DELAY_MS = 1200L;
    private static final Map<Long, Long> genreDelayMap = new ConcurrentHashMap<Long, Long>();

    private EgoTalk() {
    }

    /**
     * 일반채팅 처리.
     * true 반환 시 해당 채팅은 주변에 방송하지 않고 소비해야 합니다.
     */
    public static boolean chat(PcInstance pc, String msg) {
        if (tryGenreTalk(pc, msg))
            return true;
        return EgoWeaponControlController.onNormalChat(pc, msg);
    }

    /**
     * 자동 경고 체크.
     */
    public static void warning(PcInstance pc) {
        EgoWeaponControlController.checkAutoWarning(pc);
    }

    private static boolean tryGenreTalk(PcInstance pc, String msg) {
        if (pc == null || msg == null)
            return false;

        Inventory inv = pc.getInventory();
        if (inv == null)
            return false;

        ItemInstance weapon = inv.getSlot(Lineage.SLOT_WEAPON);
        if (weapon == null || !EgoWeaponDatabase.isEgoWeapon(weapon))
            return false;

        String egoName = EgoWeaponDatabase.getEgoName(weapon, "에고");
        String text = msg.trim();
        if (text.length() == 0)
            return false;

        String command = extractCommand(text, egoName);
        if (command == null || command.length() == 0)
            return false;

        if (EgoGenreGuide.isGuideRequest(command)) {
            if (!checkGenreDelay(pc))
                return EgoMessageUtil.consumeNormalChat();
            if (command.indexOf("추천") >= 0)
                EgoMessageUtil.normal(pc, EgoGenreGuide.suggest(weapon));
            else
                EgoMessageUtil.normal(pc, EgoGenreGuide.guide(weapon));
            return EgoMessageUtil.consumeNormalChat();
        }

        if (!EgoGenreTalk.isGenreRequest(command))
            return false;

        if (!checkGenreDelay(pc))
            return EgoMessageUtil.consumeNormalChat();

        String answer = EgoGenreTalk.talk(pc, weapon, command);
        if (answer == null || answer.length() == 0)
            return false;

        EgoMessageUtil.normal(pc, answer);
        return EgoMessageUtil.consumeNormalChat();
    }

    private static String extractCommand(String text, String egoName) {
        if (egoName == null || egoName.length() == 0)
            egoName = "에고";

        if (text.equalsIgnoreCase(egoName))
            return "";
        if (text.toLowerCase().startsWith(egoName.toLowerCase() + " "))
            return text.substring(egoName.length()).trim();
        if (text.equalsIgnoreCase(egoName + "야") || text.equalsIgnoreCase(egoName + "님"))
            return "";
        if (text.startsWith(egoName + "야 "))
            return text.substring((egoName + "야 ").length()).trim();
        if (text.startsWith(egoName + "님 "))
            return text.substring((egoName + "님 ").length()).trim();
        return null;
    }

    private static boolean checkGenreDelay(PcInstance pc) {
        if (pc == null)
            return false;
        long now = java.lang.System.currentTimeMillis();
        Long last = genreDelayMap.get(pc.getObjectId());
        if (last != null && now - last.longValue() < GENRE_TALK_DELAY_MS)
            return false;
        genreDelayMap.put(pc.getObjectId(), now);
        return true;
    }
}
