package lineage.world.controller;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lineage.util.Util;
import lineage.world.object.instance.PcInstance;

/**
 * 에고 대사 반복 방지 헬퍼.
 *
 * 캐릭터별 최근 대사 5개를 메모리에만 저장한다.
 * DB 저장 없음. 서버 재시작 시 초기화.
 */
public final class EgoTalkHistory {

    private static final int MAX_RECENT = 5;
    private static final Map<Long, LinkedList<String>> recentMap = new ConcurrentHashMap<Long, LinkedList<String>>();

    private EgoTalkHistory() {
    }

    public static String pick(PcInstance pc, String[] messages) {
        if (messages == null || messages.length == 0)
            return "...";
        if (pc == null)
            return messages[Util.random(0, messages.length - 1)];

        LinkedList<String> recent = getRecent(pc);
        List<String> candidates = new ArrayList<String>();
        synchronized (recent) {
            for (int i = 0; i < messages.length; i++) {
                String msg = messages[i];
                if (msg != null && msg.length() > 0 && !recent.contains(msg))
                    candidates.add(msg);
            }

            if (candidates.isEmpty()) {
                recent.clear();
                for (int i = 0; i < messages.length; i++) {
                    String msg = messages[i];
                    if (msg != null && msg.length() > 0)
                        candidates.add(msg);
                }
            }

            String picked = candidates.isEmpty() ? "..." : candidates.get(Util.random(0, candidates.size() - 1));
            remember(recent, picked);
            return picked;
        }
    }

    public static void remember(PcInstance pc, String message) {
        if (pc == null || message == null || message.length() == 0)
            return;
        LinkedList<String> recent = getRecent(pc);
        synchronized (recent) {
            remember(recent, message);
        }
    }

    private static LinkedList<String> getRecent(PcInstance pc) {
        LinkedList<String> list = recentMap.get(pc.getObjectId());
        if (list == null) {
            list = new LinkedList<String>();
            recentMap.put(pc.getObjectId(), list);
        }
        return list;
    }

    private static void remember(LinkedList<String> recent, String message) {
        recent.remove(message);
        recent.addFirst(message);
        while (recent.size() > MAX_RECENT)
            recent.removeLast();
    }
}
