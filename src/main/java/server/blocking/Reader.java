package server.blocking;

import protocols.IOArrayProtocol;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Reader implements Runnable {

    private final Socket client;
    private final Executor threadPool;
    private final Executor sender = Executors.newSingleThreadExecutor();

    public Reader(Socket client, Executor threadPool) {
        this.client = client;
        this.threadPool = threadPool;
    }

    @Override
    public void run() {
        try {
            while (true) {
                InputStream input = client.getInputStream();
                int[] array = IOArrayProtocol.read(input);
                threadPool.execute(new Worker(client, sender, array));
            }
        } catch (IOException ignored) {}
    }
}
