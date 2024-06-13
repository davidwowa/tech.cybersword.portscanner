package tech.cybersword;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ParallelPortScannerWithUDP {

    private static final Logger logger = LogManager.getLogger(ParallelPortScannerWithUDP.class);

    public void start(String host, int startPort, int endPort, int timeoutMillis) {

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(availableProcessors);

        List<Future<String>> futures = new ArrayList<>();

        try {
            for (int port = startPort; port <= endPort; port++) {
                Callable<String> scannerTask = new PortScannerTask(host, port, timeoutMillis);
                Future<String> future = executorService.submit(scannerTask);
                futures.add(future);
            }

            for (int port = startPort; port <= endPort; port++) {
                Callable<String> scannerTask = new PortScannerTask(host, port, timeoutMillis);
                Future<String> future = executorService.submit(scannerTask);
                futures.add(future);
            }

            for (int port = startPort; port <= endPort + (endPort - startPort); port++) {
                try {
                    Future<String> future = futures.get(port - startPort);
                    String result = future.get();
                    if (null != result) {
                        if (logger.isInfoEnabled()) {
                            logger.info(result);
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    // Handle any errors during task execution
                }
            }
        } finally {
            executorService.shutdown();
        }
    }
}

class PortScannerTask implements Callable<String> {

    private final String host;
    private final int port;
    private final int timeoutMillis;
    private final boolean isUdpScan;

    PortScannerTask(String host, int port, int timeoutMillis) {
        this.host = host;
        this.port = port;
        this.timeoutMillis = timeoutMillis;
        this.isUdpScan = true;
    }

    @Override
    public String call() {
        try {
            if (isUdpScan) {
                return performUdpScan();
            } else {
                return performTcpScan();
            }
        } catch (IOException e) {
            return "Port " + port + " is closed or unreachable";
        }
    }

    private String performUdpScan() throws IOException {
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            udpSocket.setSoTimeout(timeoutMillis);
            InetAddress targetAddress = InetAddress.getByName(host);
            byte[] requestData = UUID.randomUUID().toString().getBytes();
            DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length, targetAddress, port);
            udpSocket.send(requestPacket);
            udpSocket.receive(requestPacket);
            return "Port " + port + " is open - UDP";
        } catch (SocketTimeoutException e) {
            // return "Port " + port + " is closed - UDP Timeout";
        }
        return null;
    }

    private String performTcpScan() throws IOException {
        try (Socket tcpSocket = new Socket()) {
            tcpSocket.connect(new InetSocketAddress(host, port), timeoutMillis);
            String service = recognizeService(tcpSocket);
            return "Port " + port + " is open - Service: " + service;
        } catch (SocketTimeoutException e) {
            // return "Port " + port + " is closed - TCP Timeout";
        }
        return null;
    }

    private static String recognizeService(Socket socket) throws IOException {
        String response = "";
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            response = in.readLine();
        } catch (IOException e) {
            // Handle any errors during reading the response
        }
        return response;
    }
}
