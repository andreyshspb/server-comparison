package server.asynchronous;

import protocols.IOArrayProtocol;
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


public class AsynchronousServer {
    private final ExecutorService threadPool = Executors.newFixedThreadPool(ServerConstants.DEFAULT_THREADS_NUMBER);

    public void start() {
        try (AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open()) {
            server.bind(new InetSocketAddress(ServerConstants.PORT));
            server.accept(
                    null,
                    new CompletionHandler<>() {
                        @Override
                        public void completed(AsynchronousSocketChannel channel, Object unused) {
                            ClientHandler handler = new ClientHandler(channel);
                            channel.read(
                                    handler.getReadBuffer(),
                                    handler,
                                    new CompletionHandler<>() {
                                        @Override
                                        public void completed(Integer unused, ClientHandler handler) {
                                            try {
                                                handler.process();
                                            } catch (IOException ignored) {}
                                            channel.read(handler.getReadBuffer(), handler, this);
                                        }

                                        @Override
                                        public void failed(Throwable exc, ClientHandler attachment) {}
                                    }
                            );
                            server.accept(null, this);
                        }

                        @Override
                        public void failed(Throwable exc, Object attachment) {}
                    }
            );
            try {
                Thread.sleep(10000000);
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        } catch (IOException exception) {
            System.err.println(exception.getMessage());
        }
    }

    private class ClientHandler {
        private final AsynchronousSocketChannel channel;

        private final ByteBuffer readBuffer = ByteBuffer.allocate(ServerConstants.BUFFER_SIZE);
        private boolean readingMessage = false;
        private int messageSize = 0;

        public ClientHandler(AsynchronousSocketChannel channel) {
            this.channel = channel;
        }

        public void process() throws IOException {
            while (check()) {
                byte[] message = takeMessage();
                int[] array = IOArrayProtocol.toIntArray(message);
                threadPool.submit(() -> {
                    SortService.sort(array);
                    ByteBuffer buffer = toBuffer(IOArrayProtocol.toByteArray(array));
                    channel.write(
                            buffer,
                            this,
                            new CompletionHandler<>() {
                                @Override
                                public void completed(Integer unused, ClientHandler handler) {
                                    channel.write(buffer, ClientHandler.this, this);
                                }

                                @Override
                                public void failed(Throwable exc, ClientHandler attachment) {}
                            }
                    );
                });
            }
        }

        public ByteBuffer getReadBuffer() {
            return readBuffer;
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

        private ByteBuffer toBuffer(byte[] data) {
            ByteBuffer buffer = ByteBuffer.allocate(ServerConstants.PROTOCOL_HEAD_SIZE + data.length);
            buffer.putInt(data.length);
            buffer.put(data);
            buffer.flip();
            return buffer;
        }
    }

}
