package server.unblocking;

import server.ServerConstants;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class UnblockingServer {
    private final ExecutorService threadPool = Executors.newFixedThreadPool(ServerConstants.DEFAULT_THREADS_NUMBER);

    private final Reader reader;
    private final ExecutorService readerService = Executors.newSingleThreadExecutor();
    private final Writer writer;
    private final ExecutorService writerService = Executors.newSingleThreadExecutor();

    public UnblockingServer() throws IOException {
        this.writer = new Writer();
        this.reader = new Reader(writer, threadPool);
    }

    public void start() {
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            serverSocketChannel.socket().bind(new InetSocketAddress(ServerConstants.PORT));
            readerService.submit(reader);
            writerService.submit(writer);
            while (true) {
                SocketChannel socketChannel = serverSocketChannel.accept();
                socketChannel.configureBlocking(false);
                reader.register(socketChannel);
            }
        } catch (IOException exception) {
            System.err.println(exception.getMessage());
        }
    }

}
