import java.net.*;
import java.io.*;
import java.util.Random;

public class Relay {

    static int delay;
    static int splitSize;
    static Random random = new Random();

    static void connect(Socket cs, Socket ts) throws IOException {
        DataInputStream csin = new DataInputStream(cs.getInputStream());
        DataOutputStream csout = new DataOutputStream(cs.getOutputStream());
        DataInputStream tsin = new DataInputStream(ts.getInputStream());
        DataOutputStream tsout = new DataOutputStream(ts.getOutputStream());

        new Thread(() -> {
            try {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = csin.read(buffer)) >= 0) {
                    tsout.write(buffer, 0, n);
                }
                csin.close();
                tsout.close();
            } catch (IOException e) {
            }
            System.out.println("Close connection to server");
         }).start();

        new Thread(() -> {
            try {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = tsin.read(buffer)) >= 0) {
                    int n0 = n;
                    while (n > splitSize) {
                        int i = random.nextInt(splitSize);
                        csout.write(buffer, n0 - n, i);
                        n -= i;
                        if (random.nextInt(20) == 0) {
                            Thread.sleep(random.nextInt(delay)); // random delay
                        }
                    }
                    if (random.nextInt(20) == 0) {
                        Thread.sleep(random.nextInt(delay)); // random delay
                    }
                    csout.write(buffer, n0 - n, n);
                }
                tsin.close();
                csout.close();
            } catch (Exception e) {
            }
            System.out.println("Close connection to client");
        }).start();
    }

    public static void main(String[] args) throws IOException {

        String splitStr = System.getenv("SPLIT_SIZE");
        String delayStr = System.getenv("DELAY");
        String portStr = System.getenv("PORT");
        String targetStr = System.getenv("TARGET");
        String targetportStr = System.getenv("TARGETPORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 25432;
        String target = targetStr != null ? targetStr : "localhost";
        int targetPort = targetportStr != null ? Integer.parseInt(targetportStr) : 35432;
        delay = delayStr != null ? Integer.parseInt(delayStr) : 2000;
        splitSize = splitStr != null ? Integer.parseInt(splitStr) : 512;

        System.out.printf("Starting relay: delay %d port %d target %s target-port %d\n", delay, port, target, targetPort);

        ServerSocket server = new ServerSocket(port);
        while (true) {
            Socket cs = server.accept();
            System.out.println("New connection");

            try {
                Socket ts = new Socket(target, targetPort);
                connect(cs, ts);
            }
            catch (IOException e) {
                System.out.printf("Relay error: %s\n", e.getMessage());
                cs.close();
            }
        }
    }
}

