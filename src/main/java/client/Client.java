package client;

import protocols.IOArrayProtocol;
import server.ServerConstants;

import java.io.*;
import java.net.Socket;
import java.util.Random;


public class Client implements Runnable {

    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", ServerConstants.PORT)) {
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());

            for (int j = 0; j < 2; j++) {
                int[] array = new int[4];
                array[0] = 2;
                array[1] = 5;
                array[2] = 1;
                array[3] = 3;
                IOArrayProtocol.write(output, array);
                int[] result = IOArrayProtocol.read(input);

                for (int i = 0; i < 4; i++) {
                    System.out.println(result[i]);
                }
                System.out.println("---");
            }

        } catch (IOException ignored) {}
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
