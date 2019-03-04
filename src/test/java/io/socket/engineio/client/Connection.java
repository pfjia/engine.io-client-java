package io.socket.engineio.client;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.junit.After;
import org.junit.Before;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Logger;

public abstract class Connection {

    private static final Logger logger = Logger.getLogger(Socket.class.getName());

    final static int TIMEOUT = 1_000_000;
    final static int PORT = 3000;

    private Process serverProcess;
    private ExecutorService serverService;
    private Future serverOutout;
    private Future serverError;

    @Before
    public void startServer() throws IOException, InterruptedException {
        logger.fine("Starting server ...");

        final CountDownLatch latch = new CountDownLatch(1);
        serverProcess = Runtime.getRuntime().exec(
                "node src/test/resources/server.js", createEnv());
        serverService = Executors.newCachedThreadPool();
        serverOutout = serverService.submit(new Runnable() {
            @Override
            public void run() {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(serverProcess.getInputStream()));
                String line;
                try {
                    line = reader.readLine();
                    latch.countDown();
                    do {
                        logger.fine("SERVER OUT: " + line);
                    } while ((line = reader.readLine()) != null);
                } catch (IOException e) {
                    logger.warning(e.getMessage());
                }
            }
        });
        serverError = serverService.submit(new Runnable() {
            @Override
            public void run() {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(serverProcess.getErrorStream()));
                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        logger.fine("SERVER ERR: " + line);
                    }
                } catch (IOException e) {
                    logger.warning(e.getMessage());
                }
            }
        });
        latch.await(3000, TimeUnit.MILLISECONDS);
    }

    @After
    public void stopServer() throws InterruptedException {
        logger.fine("Stopping server ...");
        serverProcess.destroy();
        serverOutout.cancel(false);
        serverError.cancel(false);
        serverService.shutdown();
        serverService.awaitTermination(3000, TimeUnit.MILLISECONDS);
    }

    Socket.Options createOptions() {
        Socket.Options opts = new Socket.Options();
        OkHttpClient okHttpClient = new OkHttpClient();
        HttpLoggingInterceptor interceptor=new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
                System.out.println(message);
            }
        });
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        okHttpClient = okHttpClient.newBuilder().addInterceptor(interceptor).build();
        opts.callFactory = okHttpClient;
        opts.webSocketFactory = okHttpClient;
        opts.port = PORT;
        opts.rememberUpgrade = true;
        return opts;
    }

    String[] createEnv() {
        Map<String, String> env = new HashMap<String, String>(System.getenv());
        env.put("DEBUG", "engine*");
        env.put("PORT", String.valueOf(PORT));
        String[] _env = new String[env.size()];
        int i = 0;
        for (String key : env.keySet()) {
            _env[i] = key + "=" + env.get(key);
            i++;
        }
        return _env;
    }
}
