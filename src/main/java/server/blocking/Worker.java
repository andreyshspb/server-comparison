package server.blocking;

import server.SortService;

import java.net.Socket;
import java.util.concurrent.Executor;

public class Worker implements Runnable {

    private final Socket client;
    private final Executor sender;
    private final int[] array;

    public Worker(Socket client, Executor sender, int[] array) {
        this.client = client;
        this.sender = sender;
        this.array = array;
    }

    @Override
    public void run() {
        SortService.sort(array);
        sender.execute(new Writer(client, array));
    }
}
