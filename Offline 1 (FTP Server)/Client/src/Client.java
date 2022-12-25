import java.io.IOException;
import java.util.Scanner;

public class Client {
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        String request;
        try {
            while (true) {
                request = scanner.nextLine();

                Thread clientManager = new ClientManager(request);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
