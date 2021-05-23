package server.nonblocking;

import protocols.IOArrayProtocol;
import server.Server;
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


public class NonBlockingServer implements Server {
    private final ExecutorService threadPool = Executors.newFixedThreadPool(ServerConstants.DEFAULT_THREADS_NUMBER);

    private final Selector readSelector;
    private final Selector writeSelector;

    private final IOService reader;
    private final ExecutorService readerService = Executors.newSingleThreadExecutor();
    private final IOService writer;
    private final ExecutorService writerService = Executors.newSingleThreadExecutor();

    public NonBlockingServer() throws IOException {
        this.readSelector = Selector.open();
        this.writeSelector = Selector.open();
        this.reader = new IOService(readSelector, SelectionKey.OP_READ);
        this.writer = new IOService(writeSelector, SelectionKey.OP_WRITE);
    }

    @Override
    public void start() {
        try (ServerSocketChannel server = ServerSocketChannel.open()) {
            server.socket().bind(new InetSocketAddress(ServerConstants.PORT));
            readerService.submit(reader);
            writerService.submit(writer);
            while (true) {
                SocketChannel socketChannel = server.accept();
                socketChannel.configureBlocking(false);
                reader.register(new ClientHandler(socketChannel));
            }
        } catch (IOException exception) {
            System.err.println(exception.getMessage());
        }
    }

    private class ClientHandler {
        private final SocketChannel channel;

        private final ByteBuffer readBuffer = ByteBuffer.allocate(ServerConstants.BUFFER_SIZE);
        private boolean readingMessage = false;
        private int messageSize = 0;

        private final ByteBuffer writeBuffer = ByteBuffer.allocate(ServerConstants.BUFFER_SIZE);

        public ClientHandler(SocketChannel channel) {
            this.channel = channel;
        }

        public void read() throws IOException {
            channel.read(readBuffer);
            while (check()) {
                byte[] message = takeMessage();
                int[] array = IOArrayProtocol.toIntArray(message);
                threadPool.submit(() -> {
                    SortService.sort(array);
                    putMessage(IOArrayProtocol.toByteArray(array));
                    writer.register(this);
                });
            }
        }

        public void write() throws IOException{
            synchronized (writeBuffer) {
                writeBuffer.flip();
                channel.write(writeBuffer);
                writeBuffer.compact();
                if (writeBuffer.position() == 0) {
                    channel.keyFor(writeSelector).cancel();
                }
            }
        }

        public SocketChannel getChannel() {
            return channel;
        }

        private boolean check() {
            if (!readingMessage && readBuffer.position() >= ServerConstants.PROTOCOL_HEAD_SIZE) {
                readBuffer.flip();
                messageSize = readBuffer.getInt();
                readBuffer.compact();
                readingMessage = true;
            }
            if (readingMessage) {
                return readBuffer.position() >= messageSize;
            }
            return false;
        }

        private byte[] takeMessage() {
            byte[] message = new byte[messageSize];
            readBuffer.flip();
            readBuffer.get(message);
            readBuffer.compact();
            readingMessage = false;
            messageSize = 0;
            return message;
        }

        private void putMessage(byte[] data) {
            synchronized (writeBuffer) {
                writeBuffer.putInt(data.length);
                writeBuffer.put(data);
            }
        }
    }

    private class IOService implements Runnable {
        private final Selector selector;
        private final int mod;
        private final Queue<ClientHandler> queue = new ConcurrentLinkedQueue<>();

        public IOService(Selector selector, int mod) {
            this.selector = selector;
            this.mod = mod;
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

        public void register(ClientHandler handler) {
            queue.add(handler);
            selector.wakeup();
        }

        private void handleClients() throws IOException {
            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                ClientHandler handler = (ClientHandler) key.attachment();
                if (mod == SelectionKey.OP_READ) {
                    handler.read();
                } else if (mod == SelectionKey.OP_WRITE) {
                    handler.write();
                }
                keyIterator.remove();
            }
        }

        private void registerClients() throws IOException {
            while (!queue.isEmpty()) {
                ClientHandler handler = queue.poll();
                SocketChannel socketChannel = handler.getChannel();
                socketChannel.register(selector, mod, handler);
            }
        }
    }
}
