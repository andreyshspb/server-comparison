package app;

import client.Client;
import server.Server;
import server.asynchronous.AsynchronousServer;
import server.blocking.BlockingServer;
import server.nonblocking.NonBlockingServer;

import java.io.IOException;
import java.util.Scanner;

public class Main {

    enum TestingType {
        ARRAY_SIZE,
        CLIENTS_NUMBER,
        SENDING_DELTA
    }

    public static void main(String[] args) throws IOException {
        Scanner in = new Scanner(System.in);


        System.out.println("1. Please, choose server architecture for testing");
        System.out.println("1 -- Blocking architecture");
        System.out.println("2 -- Non-blocking architecture");
        System.out.println("3 -- Asynchronous architecture");
        Server server;
        while (true) {
            System.out.print(">> ");
            int serverType = in.nextInt();
            if (serverType == 1) {
                server = new BlockingServer();
                break;
            } else if (serverType == 2) {
                server = new NonBlockingServer();
                break;
            } else if (serverType == 3) {
                server = new AsynchronousServer();
                break;
            }
            System.out.println("Incorrect value of parameter, try again");
        }

        System.out.println("\n2. Please, choose number of requests for one client");
        int requestNumber;
        while (true) {
            System.out.print(">> ");
            requestNumber = in.nextInt();
            if (requestNumber >= 0) {
                break;
            }
            System.out.println("It is a negative number, try again");
        }


        System.out.println("\n3. Please, choose the parameter for server testing");
        System.out.println("1 -- size of array");
        System.out.println("2 -- number of clients");
        System.out.println("3 -- delta of sending");
        TestingType testingType;
        while (true) {
            System.out.print(">> ");
            int parameterType = in.nextInt();
            if (parameterType == 1) {
                testingType = TestingType.ARRAY_SIZE;
                break;
            } else if (parameterType == 2) {
                testingType = TestingType.CLIENTS_NUMBER;
                break;
            } else if (parameterType == 3) {
                testingType = TestingType.SENDING_DELTA;
                break;
            }
            System.out.println("Incorrect value of parameter, try again");
        }


        System.out.println("\n4. Please, choose bounds and step");
        System.out.println("Lower bound:");
        int lowerBound;
        while (true) {
            System.out.print(">> ");
            lowerBound = in.nextInt();
            if (lowerBound >= 0) {
                break;
            }
            System.out.println("It is a negative number, try again");
        }
        System.out.println("Upper bound:");
        int upperBound;
        while (true) {
            System.out.print(">> ");
            upperBound = in.nextInt();
            if (upperBound >= lowerBound) {
                break;
            }
            System.out.println("The specified upper bound is less than lower bound, try again");
        }
        System.out.println("Step:");
        int step;
        while (true) {
            System.out.print(">> ");
            step = in.nextInt();
            if (step > 0) {
                break;
            }
            System.out.println("It is not a positive number, try again");
        }


        System.out.println("\n5. Please, choose the default value for other parameters");
        int arraySize;
        int clientsNumber;
        int sendingDelta;
        if (testingType != TestingType.ARRAY_SIZE) {
            System.out.println("Size of array");
            while (true) {
                System.out.print(">> ");
                arraySize = in.nextInt();
                if (arraySize >= 0) {
                    break;
                }
                System.out.println("It is a negative number, try again");
            }
        } else {
            arraySize = lowerBound;
        }
        if (testingType != TestingType.CLIENTS_NUMBER) {
            System.out.println("number of clients");
            while (true) {
                System.out.print(">> ");
                clientsNumber = in.nextInt();
                if (clientsNumber > 0) {
                    break;
                }
                System.out.println("It is a negative number, try again");
            }
        } else {
            clientsNumber = lowerBound;
        }
        if (testingType != TestingType.SENDING_DELTA) {
            System.out.println("Delta of sending");
            while (true) {
                System.out.print(">> ");
                sendingDelta = in.nextInt();
                if (sendingDelta >= 0) {
                    break;
                }
                System.out.println("It is a negative number, try again");
            }
        } else {
            sendingDelta = lowerBound;
        }

        new Thread(server::start).start();

        StatisticService statisticService = new StatisticService();

        Client client = new Client(1000, 100, 10, statisticService);
        client.run();

        System.out.println(statisticService.get());
    }
}
