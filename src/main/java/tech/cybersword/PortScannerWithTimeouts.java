package tech.cybersword;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PortScannerWithTimeouts {

    private static final Logger logger = LogManager.getLogger(PortScannerWithTimeouts.class);

    public void start(String host, int startPort, int endPort, int timeoutMillis) {

        if (logger.isInfoEnabled()) {
            logger.info("start normal port scan with timeouts");
        }

        for (int port = startPort; port <= endPort; port++) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), timeoutMillis);

                String service = recognizeService(socket);

                if (logger.isInfoEnabled()) {
                    logger.info(String.format("Port %s is open - Service: %s", port, service));
                }
            } catch (SocketTimeoutException e) {
                if (logger.isInfoEnabled()) {
                    // logger.info(String.format("Port %s is closed - Timeout %s", port,
                    // timeoutMillis));
                }
            } catch (Exception e) {
                if (logger.isInfoEnabled()) {
                    logger.info(String.format("Port %s is closed or unreachable", port));
                }
            }
        }
    }

    public void startParallel(String host, int startPort, int endPort, int timeoutMillis) {
        if (logger.isInfoEnabled()) {
            logger.info("Start parallel port scan with timeouts");
        }

        int numberOfThreads = 20; // Anzahl der Threads im Pool
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        for (int port = startPort; port <= endPort; port++) {
            final int finalPort = port;
            executor.submit(() -> {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(host, finalPort), timeoutMillis);
                    String service = recognizeService(socket);

                    if (logger.isInfoEnabled()) {
                        logger.info(String.format("Port %d is open - Service: %s", finalPort, service));
                    }
                } catch (SocketTimeoutException e) {
                    // Bei Bedarf wieder einloggen
                } catch (Exception e) {
                    if (logger.isInfoEnabled()) {
                        logger.info(String.format("Port %d is closed or unreachable", finalPort));
                    }
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.HOURS); // Warten Sie maximal eine Stunde auf das Ende aller Scans
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (logger.isInfoEnabled()) {
            logger.info("Parallel port scan completed.");
        }
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