package server.unblocking;

import server.ServerConstants;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UnblockingServer {
    private final ExecutorService threadPool = Executors.newFixedThreadPool(ServerConstants.DEFAULT_THREADS_NUMBER);

    private final ExecutorService requestReader = Executors.newSingleThreadExecutor();

    private final Selector readSelector;

    public UnblockingServer() throws IOException {
        this.readSelector = Selector.open();
    }

    public void start() {
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            serverSocketChannel.socket().bind(new InetSocketAddress(ServerConstants.PORT));
            requestReader.submit(new Reader());
            while (true) {
                SocketChannel socketChannel = serverSocketChannel.accept();
                socketChannel.configureBlocking(false);
                socketChannel.register(readSelector, SelectionKey.OP_READ, new ClientHandler(socketChannel));
            }
        } catch (IOException exception) {
            System.err.println(exception.getMessage());
        }
    }

    private class Reader implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    readSelector.select();
                    Set<SelectionKey> selectedKeys = readSelector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                    while (keyIterator.hasNext()) {
                        SelectionKey key = keyIterator.next();
                        ClientHandler clientHandler = (ClientHandler) key.attachment();
                        clientHandler.process();
                        keyIterator.remove();
                    }
                }
            } catch (IOException ignored) {}
        }
    }

    private class ClientHandler {
        private final SocketChannel channel;
        private int leftForSize = 4;
        private byte[] size = new byte[4];
        private int leftForMessage;
        private byte[] message;

        public ClientHandler(SocketChannel channel) {
            this.channel = channel;
        }

        public void process() throws IOException {
            ByteBuffer buffer = ByteBuffer.allocate(ServerConstants.BUFFER_SIZE);
            while (channel.read(buffer) != -1) {

            }
        }
    }

}
