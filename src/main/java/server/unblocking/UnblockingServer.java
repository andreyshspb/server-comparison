package server.unblocking;

import protocols.IOArrayProtocol;
import server.ServerConstants;
import server.SortService;

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
    private final Writer writer;
    private final ExecutorService writerService = Executors.newSingleThreadExecutor();

    public UnblockingServer() throws IOException {
        this.reader = new Reader();
        this.writer = new Writer();
    }

    public void start() {
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            serverSocketChannel.socket().bind(new InetSocketAddress(ServerConstants.PORT));
            readerService.submit(reader);
            writerService.submit(writer);
            while (true) {
                SocketChannel socketChannel = serverSocketChannel.accept();
                socketChannel.configureBlocking(false);
                reader.register(new ClientReadHandler(socketChannel));
            }
        } catch (IOException exception) {
            System.err.println(exception.getMessage());
        }
    }

    private class Reader implements Runnable {
        private final Selector selector;
        private final Queue<ClientReadHandler> queue = new ConcurrentLinkedQueue<>();

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

        public void register(ClientReadHandler clientReadHandler) {
            queue.add(clientReadHandler);
            selector.wakeup();
        }

        private void handleClients() throws IOException {
            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                ClientReadHandler clientReadHandler = (ClientReadHandler) key.attachment();
                clientReadHandler.process();
                keyIterator.remove();
            }
        }

        private void registerClients() throws IOException {
            while (!queue.isEmpty()) {
                ClientReadHandler clientReadHandler = queue.poll();
                SocketChannel socketChannel = clientReadHandler.getChannel();
                socketChannel.register(selector, SelectionKey.OP_READ, clientReadHandler);
            }
        }
    }

    private class Writer implements Runnable {
        private final Selector selector;
        private final Queue<ClientWriteHandler> queue = new ConcurrentLinkedQueue<>();

        public Writer() throws IOException {
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

        public void register(ClientWriteHandler clientWriteHandler) {
            queue.add(clientWriteHandler);
            selector.wakeup();
        }

        private void handleClients() throws IOException {
            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                ClientWriteHandler clientWriteHandler = (ClientWriteHandler) key.attachment();
                clientWriteHandler.process();
                keyIterator.remove();
            }
        }

        private void registerClients() throws IOException {
            while (!queue.isEmpty()) {
                ClientWriteHandler clientWriteHandler = queue.poll();
                SocketChannel socketChannel = clientWriteHandler.getChannel();
                SelectionKey key = socketChannel.register(selector, SelectionKey.OP_WRITE, clientWriteHandler);
                clientWriteHandler.setKey(key);
            }
        }
    }

    private class ClientReadHandler {
        private final SocketChannel channel;
        private final ByteBuffer buffer = ByteBuffer.allocate(ServerConstants.BUFFER_SIZE);
        private boolean readingMessage = false;
        private int messageSize;

        public ClientReadHandler(SocketChannel channel) {
            this.channel = channel;
        }

        public void process() throws IOException {
            channel.read(buffer);
            if (check()) {
                byte[] message = takeMessage();
                int[] array = IOArrayProtocol.toIntArray(message);
                threadPool.submit(() -> {
                    SortService.sort(array);
                    byte[] data = IOArrayProtocol.toByteArray(array);
                    writer.register(new ClientWriteHandler(channel, data));
                });
            }
        }

        private boolean check() {
            if (buffer.position() >= ServerConstants.PROTOCOL_HEAD_SIZE) {
                buffer.flip();
                messageSize = buffer.getInt();
                buffer.compact();
                readingMessage = true;
            }
            if (readingMessage) {
                return buffer.position() >= messageSize;
            }
            return false;
        }

        private byte[] takeMessage() {
            buffer.flip();
            byte[] message = new byte[messageSize];
            for (int i = 0; i < messageSize; i++) {
                message[i] = buffer.get();
            }
            buffer.compact();
            readingMessage = false;
            return message;
        }

        public SocketChannel getChannel() {
            return channel;
        }
    }

    private class ClientWriteHandler {
        private final SocketChannel channel;
        private final ByteBuffer buffer;
        private SelectionKey key;

        public ClientWriteHandler(SocketChannel channel, byte[] data) {
            this.channel = channel;
            this.buffer = ByteBuffer.allocate(ServerConstants.PROTOCOL_HEAD_SIZE + data.length);
            buffer.putInt(data.length);
            buffer.put(data);
            buffer.flip();
        }

        public void process() throws IOException {
            System.out.println(buffer.position());
            System.out.println(buffer.capacity());
            channel.write(buffer);
            if (buffer.position() == buffer.capacity()) {
                key.cancel();
            }
        }

        public SocketChannel getChannel() {
            return channel;
        }

        public void setKey(SelectionKey key) {
            this.key = key;
        }
    }

}
