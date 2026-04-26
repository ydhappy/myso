package lineage.world.controller;

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

        String command = null;
        if (text.equalsIgnoreCase(egoName))
            command = "";
        else if (text.toLowerCase().startsWith(egoName.toLowerCase() + " "))
            command = text.substring(egoName.length()).trim();
        else if (text.equalsIgnoreCase(egoName + "야") || text.equalsIgnoreCase(egoName + "님"))
            command = "";
        else if (text.startsWith(egoName + "야 "))
            command = text.substring((egoName + "야 ").length()).trim();
        else if (text.startsWith(egoName + "님 "))
            command = text.substring((egoName + "님 ").length()).trim();

        if (command == null || command.length() == 0)
            return false;
        if (!EgoGenreTalk.isGenreRequest(command))
            return false;

        String answer = EgoGenreTalk.talk(pc, weapon, command);
        if (answer == null || answer.length() == 0)
            return false;

        EgoMessageUtil.normal(pc, answer);
        return EgoMessageUtil.consumeNormalChat();
    }
}
