package client;

import protocols.IOArrayProtocol;

import java.io.*;
import java.net.Socket;
import java.util.Random;


public class Client implements Runnable {

    private final String host;
    private final int port;

    private final int arraySize;
    private final int sendingDelta;
    private final int requestsNumber;

    public Client(String host, int port, int arraySize, int sendingDelta, int requestsNumber) {
        this.host = host;
        this.port = port;
        this.arraySize = arraySize;
        this.sendingDelta = sendingDelta;
        this.requestsNumber = requestsNumber;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(host, port)) {
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            DataInputStream input = new DataInputStream(socket.getInputStream());
            for (int i = 0; i < requestsNumber; i++) {
                IOArrayProtocol.write(output, generateArray());
                IOArrayProtocol.read(input);
                sleep();
            }
        } catch (IOException ignored) {}
    }

    private int[] generateArray() {
        int[] result = new int[arraySize];
        Random random = new Random();
        for (int i = 0; i < arraySize; i++) {
            result[i] = random.nextInt();
        }
        return result;
    }

    private void sleep() {
        try {
            Thread.sleep(sendingDelta);
        } catch (InterruptedException ignored) {}
    }
}
