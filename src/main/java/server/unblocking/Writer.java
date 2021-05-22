package server.unblocking;

import server.ServerConstants;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


public class Writer implements Runnable {
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

    public void register(SocketChannel channel, byte[] data) {
        queue.add(new ClientWriteHandler(channel, data));
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
            socketChannel.register(selector, SelectionKey.OP_WRITE, clientWriteHandler);
        }
    }

    private class ClientWriteHandler {
        private final SocketChannel channel;
        private final ByteBuffer buffer;

        public ClientWriteHandler(SocketChannel channel, byte[] data) {
            this.channel = channel;
            this.buffer = ByteBuffer.allocate(ServerConstants.PROTOCOL_HEAD_SIZE + data.length);
            buffer.putInt(data.length);
            buffer.put(data);
            buffer.flip();
        }

        public void process() throws IOException {
            channel.write(buffer);
            if (buffer.position() == buffer.capacity()) {
                channel.keyFor(selector).cancel();
            }
        }

        public SocketChannel getChannel() {
            return channel;
        }
    }
}
