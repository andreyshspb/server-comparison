package server.blocking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BlockingServer implements Runnable {

    private final static int DEFAULT_THREADS_NUMBER = 8;

    private final int port;
    private final Executor threadPool = Executors.newFixedThreadPool(DEFAULT_THREADS_NUMBER);

    public BlockingServer(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try (ServerSocket server = new ServerSocket(port)) {
            while (true) {
                try (Socket client = server.accept()) {
                    Thread thread = new Thread(new Reader(client, threadPool));
                    thread.start();
                }
            }
        } catch (IOException ignored) {}
    }
}
