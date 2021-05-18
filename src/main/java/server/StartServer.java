package server;

import server.blocking.BlockingServer;


public class StartServer {
    public static void main(String[] args) {
        new BlockingServer().start();
    }
}
