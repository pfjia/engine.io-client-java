package io.socket.engineio.client.executions;

import io.socket.emitter.Emitter;
import io.socket.engineio.client.Socket;
import okhttp3.OkHttpClient;

import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

public class Connection {

    public static void main(String[] args) throws URISyntaxException, InterruptedException {
        final OkHttpClient client = new OkHttpClient();
        Socket.Options opts = new Socket.Options();
        opts.webSocketFactory = client;
        opts.callFactory = client;

        int port=8080;
        final Socket socket = new Socket("http://localhost:" + port, opts);
        socket.on(Socket.EVENT_OPEN, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("open");
                socket.close();
            }
        });
        socket.on(Socket.EVENT_CLOSE, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                client.dispatcher().executorService().shutdown();
            }
        });
        socket.open();
        TimeUnit.DAYS.sleep(1);
    }
}
