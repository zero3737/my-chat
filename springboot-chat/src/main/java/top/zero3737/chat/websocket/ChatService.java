package top.zero3737.chat.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

@RestController
@RequestMapping(value = "/chat", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@ServerEndpoint("/chat/{shopId}")
public class ChatService {

    private Session session;

    private static CopyOnWriteArraySet<ChatService> chatServices = new CopyOnWriteArraySet<>();
    private static Map<String, Session> sessionPool = new HashMap<String, Session>();

    @RequestMapping(value = "/getId")
    public Map<String, String> getId() {

        HashMap<String, String> map = new HashMap<>();
        map.put("id", UUID.randomUUID().toString().replaceAll("-",""));
        return map;

    }

    @OnOpen
    public void onOpen(Session session, @PathParam(value = "shopId") String shopId) {
        this.session = session;
        chatServices.add(this);
        sessionPool.put(shopId, session);
        System.out.println("【websocket消息】有新的连接，总数为:" + chatServices.size());
    }

    @OnClose
    public void onClose() {
        chatServices.remove(this);
        System.out.println("【websocket消息】连接断开，总数为:" + chatServices.size());
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("【websocket消息】收到客户端消息:" + message);
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> map = mapper.readValue(message, Map.class);
            sendOneMessage(map.get("to"), map.get("msg"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 发送广播消息
    public void sendAllMessage(String message) {
        for (ChatService chatService : chatServices) {
            System.out.println("【websocket消息】广播消息:" + message);
            try {
                chatService.session.getAsyncRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 发送单点消息
    public void sendOneMessage(String shopId, String message) {
        Session session = sessionPool.get(shopId);
        if (session != null) {
            try {
                session.getAsyncRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
