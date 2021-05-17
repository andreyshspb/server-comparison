package server.blocking;

import protocols.IOArrayProtocol;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class Writer implements Runnable {

    private final Socket client;
    private final int[] array;

    public Writer(Socket client, int[] array) {
        this.client = client;
        this.array = array;
    }

    @Override
    public void run() {
        try {
            OutputStream output = client.getOutputStream();
            IOArrayProtocol.write(output, array);
        } catch (IOException ignored) {}
    }
}
