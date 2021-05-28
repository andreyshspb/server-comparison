package server.blocking;

import protocols.IOArrayProtocol;
import server.Server;
import server.ServerConstants;
import server.SortService;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

import java.util.concurrent.ExecutorService;


public class BlockingServer implements Server {
    private final ExecutorService threadPool = Executors.newFixedThreadPool(ServerConstants.DEFAULT_THREADS_NUMBER);

    @Override
    public void start() {

        try (ServerSocket serverSocket = new ServerSocket(ServerConstants.PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                try {
                    new ClientHandler(socket).start();
                } catch (IOException exception) {
                    System.err.println("Some problem with a client port " + socket.getPort());
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private class ClientHandler {
        private final ExecutorService requestReader = Executors.newSingleThreadExecutor();
        private final ExecutorService responseWriter = Executors.newSingleThreadExecutor();

        private final DataInputStream input;
        private final DataOutputStream output;

        private ClientHandler(Socket socket) throws IOException {
            this.input = new DataInputStream(socket.getInputStream());
            this.output = new DataOutputStream(socket.getOutputStream());
        }

        public void start() {
            requestReader.submit(() -> {
                try {
                    while (true) {
                        int[] array = IOArrayProtocol.read(input);
                        threadPool.submit(() -> {
                            SortService.sort(array);
                            sendResponse(array);
                        });
                    }
                } catch (IOException ignored) {}
            });
        }

        private void sendResponse(int[] data) {
            responseWriter.submit(() -> {
                try {
                    IOArrayProtocol.write(output, data);
                } catch (IOException ignored) {}
            });
        }
    }
}

