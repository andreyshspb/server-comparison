package server;

import server.blocking.BlockingServer;
import server.unblocking.UnblockingServer;

import java.io.IOException;


public class StartServer {
    public static void main(String[] args) throws IOException {
        new UnblockingServer().start();
    }
}
