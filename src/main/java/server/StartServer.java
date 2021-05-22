package server;

import server.asynchronous.AsynchronousServer;
import server.nonblocking.NonBlockingServer;

import java.io.IOException;


public class StartServer {
    public static void main(String[] args) throws IOException {
        new AsynchronousServer().start();
    }
}
