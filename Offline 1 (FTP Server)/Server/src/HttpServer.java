import java.io.IOException;
import java.net.ServerSocket;

public class HttpServer {
    private static final int PORT = 5087;
    private static int totalClientCount = 0;
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Waiting for clients to connect...");
            while (true) {
                Thread httpServerManager = new HttpServerManager(serverSocket.accept());
                System.out.println("Connected to " + ++totalClientCount + " clients");
                httpServerManager.start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
