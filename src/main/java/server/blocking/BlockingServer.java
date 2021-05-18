package server.blocking;

import protocols.IOArrayProtocol;
import server.SortService;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

import java.util.concurrent.ExecutorService;

import static server.ServerConstants.*;


public class BlockingServer {
    private final ExecutorService threadPool = Executors.newFixedThreadPool(DEFAULT_THREADS_NUMBER);

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket).start();
            }
        } catch (IOException exception) {
            System.err.println(exception.getMessage());
        }
    }

    private class ClientHandler {
        private final ExecutorService requestReader = Executors.newSingleThreadExecutor();
        private final ExecutorService responseWriter = Executors.newSingleThreadExecutor();

        private final InputStream input;
        private final OutputStream output;

        private ClientHandler(Socket socket) throws IOException {
            this.input = socket.getInputStream();
            this.output = socket.getOutputStream();
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

