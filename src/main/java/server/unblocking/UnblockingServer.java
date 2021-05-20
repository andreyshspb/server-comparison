package server.unblocking;

import protocols.IOArrayProtocol;
import server.ServerConstants;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UnblockingServer {
    private final ExecutorService threadPool = Executors.newFixedThreadPool(ServerConstants.DEFAULT_THREADS_NUMBER);

    private final Reader reader;
    private final ExecutorService readerService = Executors.newSingleThreadExecutor();

    public UnblockingServer() throws IOException {
        this.reader = new Reader();
    }

    public void start() {
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            serverSocketChannel.socket().bind(new InetSocketAddress(ServerConstants.PORT));
            readerService.submit(reader);
            while (true) {
                SocketChannel socketChannel = serverSocketChannel.accept();
                socketChannel.configureBlocking(false);
                reader.register(new ClientHandler(socketChannel));
            }
        } catch (IOException exception) {
            System.err.println(exception.getMessage());
        }
    }

    private class Reader implements Runnable {
        private final Selector selector;
        private final Queue<ClientHandler> queue = new ConcurrentLinkedQueue<>();

        public Reader() throws IOException {
            this.selector = Selector.open();
        }

        @Override
        public void run() {
            try {
                while (true) {
                    if (selector.select() > 0) {
                        handleClients();
                    }
                    registerClients();
                }
            } catch (IOException ignored) {}
        }

        public void register(ClientHandler clientHandler) {
            queue.add(clientHandler);
            selector.wakeup();
        }

        private void handleClients() throws IOException {
            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                ClientHandler clientHandler = (ClientHandler) key.attachment();
                clientHandler.process();
                keyIterator.remove();
            }
        }

        private void registerClients() throws IOException {
            while (!queue.isEmpty()) {
                ClientHandler clientHandler = queue.poll();
                SocketChannel socketChannel = clientHandler.getChannel();
                socketChannel.register(selector, SelectionKey.OP_READ, clientHandler);
            }
        }
    }

    private class ClientHandler {
        private final SocketChannel channel;
        private final ByteBuffer buffer = ByteBuffer.allocate(ServerConstants.BUFFER_SIZE);
        private boolean readingMessage = false;
        private int messageSize;

        public ClientHandler(SocketChannel channel) {
            this.channel = channel;
        }

        public void process() throws IOException {
            if (channel.read(buffer) > 0) {
                if (check()) {
                    byte[] message = takeMessage();
                    int[] array = IOArrayProtocol.toIntArray(message);
                    for (int element : array) {
                        System.out.println(element);
                    }
                }
            }
        }

        private boolean check() {
            if (readingMessage) {
                return buffer.position() >= messageSize;
            }
            if (buffer.position() >= ServerConstants.PROTOCOL_HEAD_SIZE) {
                buffer.flip();
                messageSize = buffer.getInt();
            }
            return false;
        }

        private byte[] takeMessage() {
            byte[] message = new byte[messageSize];
            for (int i = 0; i < messageSize; i++) {
                message[i] = buffer.get();
            }
            buffer.compact();
            return message;
        }

        public SocketChannel getChannel() {
            return channel;
        }
    }

}
