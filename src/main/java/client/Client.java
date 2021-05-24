package client;

import app.StatisticService;
import protocols.IOArrayProtocol;
import server.ServerConstants;

import java.io.*;
import java.net.Socket;
import java.util.Random;


public class Client implements Runnable {
    private final int arraySize;
    private final int sendingDelta;
    private final int requestsNumber;
    private final StatisticService statisticService;

    public Client(int arraySize, int sendingDelta, int requestsNumber, StatisticService statisticService) {
        this.arraySize = arraySize;
        this.sendingDelta = sendingDelta;
        this.requestsNumber = requestsNumber;
        this.statisticService = statisticService;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(ServerConstants.HOST, ServerConstants.PORT)) {
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            for (int i = 0; i < requestsNumber; i++) {
                long start = System.currentTimeMillis();
                IOArrayProtocol.write(output, generateArray());
                IOArrayProtocol.read(input);
                long finish = System.currentTimeMillis();
                statisticService.add(finish - start);
                sleep();
            }
        } catch (IOException ignored) {}
        statisticService.stop();
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
