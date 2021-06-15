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
import java.util.concurrent.atomic.AtomicInteger;

public class NonBlockingServer implements Server {

    private final ExecutorService workersThreadPool = Executors.newFixedThreadPool(ServerConstants.DEFAULT_THREADS_NUMBER);

    private final IOService reader;
    private final ExecutorService readerService = Executors.newSingleThreadExecutor();

    private final IOService writer;
    private final ExecutorService writerService = Executors.newSingleThreadExecutor();

    public NonBlockingServer() throws IOException {
        this.reader = new IOService(SelectionKey.OP_READ);
        this.writer = new IOService(SelectionKey.OP_WRITE);
    }

    @Override
    public void start() {
        try (ServerSocketChannel server = ServerSocketChannel.open()) {
            server.bind(new InetSocketAddress(ServerConstants.PORT));
            readerService.submit(reader);
            writerService.submit(writer);
            while (true) {
                SocketChannel socketChannel = server.accept();
                socketChannel.configureBlocking(false);
                reader.register(new ClientHandler(socketChannel));
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private class ClientHandler {
        private final SocketChannel channel;

        private final ByteBuffer readSizeBuffer = ByteBuffer.allocate(Integer.BYTES);
        private ByteBuffer readMessageBuffer = null;
        private boolean readingMessage = false;

        private final Queue<ByteBuffer> writeBuffersQueue = new ConcurrentLinkedQueue<>();
        private volatile ByteBuffer writeBuffer = null;
        private final AtomicInteger unsentMessageNumber = new AtomicInteger(0);

        public ClientHandler(SocketChannel channel) {
            this.channel = channel;
        }

        public SocketChannel getChannel() {
            return channel;
        }

        public void read() throws IOException {
            if (readingMessage) {
                channel.read(readMessageBuffer);
                if (readMessageBuffer.position() == readMessageBuffer.capacity()) {
                    readMessageBuffer.flip();
                    int[] array = IOArrayProtocol.toIntArray(readMessageBuffer);
                    workersThreadPool.submit(() -> {
                        SortService.sort(array);
                        writeBuffersQueue.add(IOArrayProtocol.toByteBuffer(array));
                        if (unsentMessageNumber.incrementAndGet() == 1) {
                            writer.register(this);
                        }
                    });
                    readingMessage = false;
                    readMessageBuffer = null;
                }
            } else {
                channel.read(readSizeBuffer);
                if (readSizeBuffer.position() == readSizeBuffer.capacity()) {
                    readSizeBuffer.flip();
                    int size = readSizeBuffer.getInt();
                    readingMessage = true;
                    readSizeBuffer.clear();
                    readMessageBuffer = ByteBuffer.allocate(size);
                }
            }
        }

        public void write() throws IOException {
            if (writeBuffer == null) {
                writeBuffer = writeBuffersQueue.poll();
            }
            channel.write(writeBuffer);
            if (writeBuffer.position() == writeBuffer.capacity()) {
                writeBuffer = null;
                if (unsentMessageNumber.decrementAndGet() == 0) {
                    writer.unregister(this);
                }
            }
        }
    }

    private class IOService implements Runnable {
        private final int mod;
        private final Selector selector;
        private final Queue<ClientHandler> queue = new ConcurrentLinkedQueue<>();

        public IOService(int mod) throws IOException {
            this.mod = mod;
            this.selector = Selector.open();
        }

        @Override
        public void run() {
            try {
                while (true) {
                    int n = selector.select();
                    registerClients();
                    if (n > 0) {
                        handleClients();
                    }
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        public void register(ClientHandler handler) {
            queue.add(handler);
            selector.wakeup();
        }

        public void unregister(ClientHandler handler) {
            SocketChannel channel = handler.getChannel();
            channel.keyFor(selector).cancel();
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