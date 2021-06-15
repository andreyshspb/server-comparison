package server.asynchronous;

import app.StatisticService;
import protocols.IOArrayProtocol;
import server.Server;
import server.ServerConstants;
import server.SortService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


public class AsynchronousServer implements Server {

    private final ExecutorService threadPool = Executors.newFixedThreadPool(ServerConstants.DEFAULT_THREADS_NUMBER);

    private final StatisticService statistic;

    public AsynchronousServer(StatisticService statistic) {
        this.statistic = statistic;
    }

    @Override
    public void start() {
        try (AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open()) {
            server.bind(new InetSocketAddress(ServerConstants.PORT));
            server.accept(server, new AcceptHandler());
            Thread.sleep(Integer.MAX_VALUE);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private class ClientHandler {
        private final AsynchronousSocketChannel channel;

        private final ByteBuffer readSizeBuffer = ByteBuffer.allocate(Integer.BYTES);
        private boolean readingMessage = false;
        private ByteBuffer readMessageBuffer = null;

        private ByteBuffer writeBuffer = null;
        private final Queue<ByteBuffer> writeBuffersQueue = new ConcurrentLinkedQueue<>();
        private final AtomicInteger unsentMessagesNumber = new AtomicInteger(0);

        private final Queue<Long> startTimesQueue = new ConcurrentLinkedQueue<>();
        private volatile Long startTime = null;

        public ClientHandler(AsynchronousSocketChannel channel) {
            this.channel = channel;
        }

        public AsynchronousSocketChannel getChannel() {
            return channel;
        }

        public ByteBuffer getReadBuffer() {
            return readingMessage ? readMessageBuffer : readSizeBuffer;
        }

        public ByteBuffer getWriteBuffer() {
            if (writeBuffer == null) {
                writeBuffer = writeBuffersQueue.poll();
                startTime = startTimesQueue.poll();
            }
            return writeBuffer;
        }

        public void read() throws IOException {
            if (readingMessage) {
                if (readMessageBuffer.position() == readMessageBuffer.capacity()) {
                    readMessageBuffer.flip();
                    int[] array = IOArrayProtocol.toIntArray(readMessageBuffer);
                    long start = System.currentTimeMillis();
                    threadPool.submit(() -> {
                        SortService.sort(array);
                        ByteBuffer buffer = IOArrayProtocol.toByteBuffer(array);
                        writeBuffersQueue.add(buffer);
                        startTimesQueue.add(start);
                        if (unsentMessagesNumber.incrementAndGet() == 1) {
                            channel.write(getWriteBuffer(), this, new WriteHandler());
                        }
                    });
                    readingMessage = false;
                    readMessageBuffer = null;
                }
            } else {
                if (readSizeBuffer.position() == readSizeBuffer.capacity()) {
                    readSizeBuffer.flip();
                    int size = readSizeBuffer.getInt();
                    readSizeBuffer.clear();
                    readingMessage = true;
                    readMessageBuffer = ByteBuffer.allocate(size);
                }
            }
        }

        public void write() throws IOException {
            if (writeBuffer.position() == writeBuffer.capacity()) {
                long finish = System.currentTimeMillis();
                if (startTime != null) {
                    statistic.add(finish - startTime);
                }
                writeBuffer = null;
                startTime = null;
                unsentMessagesNumber.decrementAndGet();
            }
        }

    }

    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel> {
        @Override
        public void completed(AsynchronousSocketChannel channel, AsynchronousServerSocketChannel server) {
            ClientHandler handler = new ClientHandler(channel);
            channel.read(handler.getReadBuffer(), handler, new ReadHandler());
            server.accept(server, this);
        }

        @Override
        public void failed(Throwable exc, AsynchronousServerSocketChannel attachment) {}
    }

    private class ReadHandler implements CompletionHandler<Integer, ClientHandler> {
        @Override
        public void completed(Integer read, ClientHandler handler) {
            if (read == -1) {
                return;
            }
            try {
                handler.read();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
            AsynchronousSocketChannel channel = handler.getChannel();
            channel.read(handler.getReadBuffer(), handler, this);
        }

        @Override
        public void failed(Throwable exc, ClientHandler attachment) {}
    }

    private class WriteHandler implements CompletionHandler<Integer, ClientHandler> {
        @Override
        public void completed(Integer unused, ClientHandler handler) {
            try {
                handler.write();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
            if (handler.getWriteBuffer() == null) {
                return;
            }
            AsynchronousSocketChannel channel = handler.getChannel();
            channel.write(handler.getWriteBuffer(), handler, this);
        }

        @Override
        public void failed(Throwable exc, ClientHandler attachment) {}
    }

}
