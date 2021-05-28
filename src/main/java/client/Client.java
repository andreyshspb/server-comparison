package client;

import app.StatisticService;
import protocols.IOArrayProtocol;
import server.Server;
import server.ServerConstants;
import server.blocking.BlockingServer;

import java.io.*;
import java.net.Socket;
import java.util.Random;


public class Client implements Runnable {

    public static void main(String[] args) {

        Server server = new BlockingServer();
        new Thread(server::start).start();

        try (Socket socket = new Socket(ServerConstants.HOST, ServerConstants.PORT)) {
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());

            int[] arr = new int[4];
            arr[0] = 4;
            arr[1] = 3;
            arr[2] = 2;
            arr[3] = 1;

            IOArrayProtocol.write(output, arr);
            int[] response = IOArrayProtocol.read(input);

            for (int element : response) {
                System.out.println(element);
            }

        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

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
        } catch (IOException exception) {
            exception.printStackTrace();
        }
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
