package ca.surgestorm.notitowin.backend;

import org.json.JSONException;

import java.util.HashSet;
import java.util.Set;

public class PacketTypeOLD {
    public static final String CLIENT_PAIR_REQUEST = "CLIENT_PAIR_REQUEST";
    public static final String SERVER_PAIR_RESPONSE = "SERVER_PAIR_RESPONSE";
    public static final String CLIENT_PAIR_CONFIRM = "CLIENT_PAIR_CONFIRM";
    public static final String NOTI_REQUEST = "ANDR_NOTI_RECEIVE";
    public static final String NOTIFICATION = "NOTIFICATION";
    public static final String SEND_REPLY = "WIN_REPLY_RECEIVE";
    public static final String DATALOAD_REQUEST = "ANDR_DATALOAD_RECEIVE";
    public static final String READY_RESPONSE = "WIN_READY";
    public static final String UNPAIR_CMD = "DISCONNECT_UNPAIR";


    public static Set<String> protocolPacketTypes = new HashSet<String>() {{
        add(CLIENT_PAIR_REQUEST);
        add(SERVER_PAIR_RESPONSE);
        add(CLIENT_PAIR_CONFIRM);
        add(NOTI_REQUEST);
        add(SEND_REPLY);
        add(DATALOAD_REQUEST);
        add(READY_RESPONSE);
        add(UNPAIR_CMD);
    }};

    public static String makeDiscoveryPacket() throws JSONException {
        JSONConverter json = new JSONConverter(CLIENT_PAIR_REQUEST);
        String deviceName = android.os.Build.MODEL;
        json.getMainBody().put("deviceName", deviceName);
        return json.serialize();
    }
}
