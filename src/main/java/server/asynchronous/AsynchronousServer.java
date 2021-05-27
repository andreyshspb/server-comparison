package server.asynchronous;

import protocols.IOArrayProtocol;
import server.Server;
import server.ServerConstants;
import server.SortService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;


public class AsynchronousServer implements Server {
    private final ExecutorService threadPool = Executors.newFixedThreadPool(ServerConstants.DEFAULT_THREADS_NUMBER);

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

    @Override
    public void stop() {}

    private class ClientHandler {
        private final AsynchronousSocketChannel channel;

        private final ByteBuffer readBuffer = ByteBuffer.allocate(ServerConstants.BUFFER_SIZE);
        private boolean readingMessage = false;
        private int messageSize = 0;

        private final ByteBuffer writeBuffer = ByteBuffer.allocate(ServerConstants.BUFFER_SIZE);
        private final Semaphore writeLock = new Semaphore(1);

        public ClientHandler(AsynchronousSocketChannel channel) {
            this.channel = channel;
        }

        public void process() throws IOException {
            System.out.println("processed");
            while (check()) {
                byte[] message = takeMessage();
                int[] array = IOArrayProtocol.toIntArray(message);
                threadPool.submit(() -> {
                    SortService.sort(array);
                    putMessage(IOArrayProtocol.toByteArray(array));
                    try {
                        writeLock.acquire();
                    } catch (InterruptedException ignored) {}
                    channel.write(writeBuffer, this, new WriteHandler());
                });
            }
        }

        public void releaseLock() {
            writeLock.release();
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
                writeBuffer.flip();
            }
        }

        public ByteBuffer getReadBuffer() {
            return readBuffer;
        }

        public ByteBuffer getWriteBuffer() {
            return writeBuffer;
        }

        public AsynchronousSocketChannel getChannel() {
            return channel;
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
                handler.process();
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
            ByteBuffer buffer = handler.getWriteBuffer();
            if (buffer.position() == buffer.limit()) {
                buffer.clear();
                handler.releaseLock();
                return;
            }
            AsynchronousSocketChannel channel = handler.getChannel();
            channel.write(buffer, handler, this);
        }

        @Override
        public void failed(Throwable exc, ClientHandler attachment) {}
    }

}
