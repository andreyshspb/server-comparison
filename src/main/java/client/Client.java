package client;

import protocols.IOArrayProtocol;
import server.ServerConstants;

import java.io.*;
import java.net.Socket;
import java.util.Random;


public class Client implements Runnable {

    public static void main(String[] args) {
        new Client(100, 1000, 10).run();
    }

    private final int arraySize;
    private final int sendingDelta;
    private final int requestsNumber;

    public Client(int arraySize, int sendingDelta, int requestsNumber) {
        this.arraySize = arraySize;
        this.sendingDelta = sendingDelta;
        this.requestsNumber = requestsNumber;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(ServerConstants.HOST, ServerConstants.PORT)) {
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            for (int i = 0; i < requestsNumber; i++) {
                IOArrayProtocol.write(output, generateArray());
                int[] response = IOArrayProtocol.read(input);
                if (!isSorted(response)) {
                    System.out.println("non sorted array");
                    return;
                }
                sleep();
            }
            System.out.println("Everything is OK");
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

    private boolean isSorted(int[] array) {
        if (arraySize != array.length) {
            return false;
        }
        for (int i = 0; i < array.length - 1; i++) {
            if (array[i] > array[i + 1]) {
                return false;
            }
        }
        return true;
    }
}
