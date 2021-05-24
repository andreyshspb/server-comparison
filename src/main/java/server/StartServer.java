package server;

import server.asynchronous.AsynchronousServer;
import server.nonblocking.NonBlockingServer;
import server.blocking.BlockingServer;

import java.io.IOException;


public class StartServer {
    public static void main(String[] args) throws IOException {
        new BlockingServer().start();
    }
}
