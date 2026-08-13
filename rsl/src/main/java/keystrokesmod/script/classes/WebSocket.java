package keystrokesmod.script.classes;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Map;

public class WebSocket {
    private final WebSocketClient client;

    public WebSocket(String serverURI) {
        this.client = createWebSocketClient(serverURI);
    }

    public WebSocket(String serverUri, Map<String, String> httpHeaders) {
        this.client = createWebSocketClient(serverUri, httpHeaders);
    }

    private WebSocketClient createWebSocketClient(String serverUri) {
        return new WebSocketClient(toURI(serverUri)) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                WebSocket.this.onOpen(handshakedata.getHttpStatus(), handshakedata.getHttpStatusMessage());
            }
            @Override
            public void onMessage(String message) {
                WebSocket.this.onMessage(message);
            }
            @Override
            public void onClose(int code, String reason, boolean remote) {
                WebSocket.this.onClose(code, reason, remote);
            }
            @Override
            public void onError(Exception ex) {
                WebSocket.this.onError(ex);
            }
        };
    }

    private WebSocketClient createWebSocketClient(String serverUri, Map<String, String> httpHeaders) {
        return new WebSocketClient(toURI(serverUri), httpHeaders) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                WebSocket.this.onOpen(handshakedata.getHttpStatus(), handshakedata.getHttpStatusMessage());
            }
            @Override
            public void onMessage(String message) {
                WebSocket.this.onMessage(message);
            }
            @Override
            public void onClose(int code, String reason, boolean remote) {
                WebSocket.this.onClose(code, reason, remote);
            }
            @Override
            public void onError(Exception ex) {
                WebSocket.this.onError(ex);
            }
        };
    }

    private static URI toURI(String uri) {
        try {
            return new URI(uri);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void onOpen(short status, String message) {

    }

    public void onMessage(String message) {

    }

    public void onClose(int code, String reason, boolean remote) {

    }

    public void onError(Exception ex) {

    }

    public boolean connect(boolean block) {
        if (block) {
            try {
                return client.connectBlocking();
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            client.connect();
        }
        return true;
    }

    public void send(String message) {
        client.send(message);
    }

    public void close(boolean block) {
        if (block) {
            try {
                client.closeBlocking();
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            client.close();
        }
    }

    public boolean isOpen() {
        return this.client == null ? false : this.client.isOpen();
    }
}