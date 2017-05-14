package main.java.com.djrapitops.plan.data.handling.info;

import java.util.UUID;
import main.java.com.djrapitops.plan.data.UserData;
import main.java.com.djrapitops.plan.data.handling.ChatHandling;

/**
 * HandlingInfo Class for ChatEvent information.
 *
 * @author Rsl1122
 * @since 3.0.0
 */
public class ChatInfo extends HandlingInfo {

    private String nickname;
    private String message;

    /**
     * Constructor.
     *
     * @param uuid UUID of the player.
     * @param nickname Nickname of the player.
     * @param message Message the player sent.
     */
    public ChatInfo(UUID uuid, String nickname, String message) {
        super(uuid, InfoType.CHAT, 0L);
        this.nickname = nickname;
        this.message = message;
    }

    @Override
    public boolean process(UserData uData) {
        if (!uData.getUuid().equals(uuid)) {
            return false;
        }
        ChatHandling.processChatInfo(uData, nickname, message);
        return true;
    }
}
