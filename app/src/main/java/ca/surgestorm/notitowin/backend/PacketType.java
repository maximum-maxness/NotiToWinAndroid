package ca.surgestorm.notitowin.backend;

import java.util.HashSet;
import java.util.Set;

public class PacketType {
    public static final String CLIENT_PAIR_REQUEST = "CLIENT_PAIR_REQUEST";
    public static final String SERVER_PAIR_RESPONSE = "SERVER_PAIR_RESPONSE";
    public static final String CLIENT_PAIR_CONFIRM = "CLIENT_PAIR_CONFIRM";
    public static final String NOTI_REQUEST = "ANDR_NOTI_RECEIVE";
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
}
