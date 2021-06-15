package client;

import app.StatisticService;
import protocols.IOArrayProtocol;
import server.ServerConstants;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;


public class Client implements Runnable {
    private final int arraySize;
    private final int sendingDelta;
    private final int requestsNumber;

    private final StatisticService statisticService;
    private final Map<Integer, Long> startTime = new ConcurrentHashMap<>();

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

            Thread requestsThread = new Thread(() -> {
                try {
                    for (int i = 0; i < requestsNumber; i++) {
                        long start = System.currentTimeMillis();
                        startTime.put(i, start);
                        IOArrayProtocol.write(output, generateArray(i));
                        long finish = System.currentTimeMillis();
                        Thread.sleep(Math.max(0, sendingDelta - (finish - start)));
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            });

            requestsThread.start();

            for (int i = 0; i < requestsNumber; i++) {
                int[] array = IOArrayProtocol.read(input);
                long finish = System.currentTimeMillis();
                statisticService.add(finish - startTime.get(array[0]));
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        statisticService.stop();
    }

    private int[] generateArray(int id) {
        int[] result = new int[arraySize];
        Random random = new Random();
        result[0] = id;
        for (int i = 1; i < arraySize; i++) {
            result[i] = id + random.nextInt(Integer.MAX_VALUE - id);
        }
        return result;
    }
}
