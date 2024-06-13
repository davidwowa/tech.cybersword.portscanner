package tech.cybersword;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class App {

    private static final Logger logger = LogManager.getLogger(App.class);

    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("udp scan not safe, use it only for educational purposes");
            System.out.println(
                    "use it as: java -jar PortScannerWithTimeouts.jar host startPort endPort prallel(true|false) timeoutMillis isUDP(true|false)");
            System.exit(1);
        }

        if (logger.isInfoEnabled()) {
            logger.info("start port scanner");
        }

        String host = args[0];
        int startPort = Integer.parseInt(args[1]);
        int endPort = Integer.parseInt(args[2]);
        boolean parallel = Boolean.parseBoolean(args[3]);
        int timeoutMillis = Integer.parseInt(args[4]);
        Boolean isUDP = Boolean.valueOf(args[5]);

        if (parallel) {
            if (isUDP) {
                new ParallelPortScannerWithUDP().start(host, startPort, endPort, timeoutMillis);
            } else {
                new PortScannerWithTimeouts().startParallel(host, startPort, endPort,
                        timeoutMillis);
            }
        } else {
            new PortScannerWithTimeouts().start(host, startPort, endPort, timeoutMillis);
        }
    }
}
