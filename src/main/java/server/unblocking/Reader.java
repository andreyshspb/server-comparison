package server.unblocking;

import protocols.IOArrayProtocol;
import server.ServerConstants;
import server.SortService;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;


public class Reader implements Runnable {
    private final Selector selector;
    private final Writer writer;
    private final ExecutorService threadPool;
    private final Queue<ClientReadHandler> queue = new ConcurrentLinkedQueue<>();

    public Reader(Writer writer, ExecutorService threadPool) throws IOException {
        this.selector = Selector.open();
        this.writer = writer;
        this.threadPool = threadPool;
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

    public void register(SocketChannel channel) {
        queue.add(new ClientReadHandler(channel));
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
                    writer.register(channel, data);
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
            byte[] message = new byte[messageSize];
            buffer.flip();
            buffer.get(message);
            buffer.compact();
            readingMessage = false;
            return message;
        }

        public SocketChannel getChannel() {
            return channel;
        }
    }
}
