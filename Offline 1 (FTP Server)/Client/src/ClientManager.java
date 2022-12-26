import java.io.*;
import java.net.Socket;
import java.nio.file.NoSuchFileException;

public class ClientManager extends Thread {
    private final Socket socket;
    private final BufferedReader br;
    private final PrintWriter pr;
    private final DataOutputStream dos;
    private final int PORT = 5087;
    private final String SERVERIP = "localhost";
    private final int CHUNKSIZE = 4096;
    private final String request;

    public ClientManager(String request) throws IOException {
        socket = new Socket(SERVERIP, PORT);
        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        pr = new PrintWriter(socket.getOutputStream(), true);
        dos = new DataOutputStream(socket.getOutputStream());
        this.request = request;
        start();
    }

    @Override
    public void run() {
        // send the command
        System.out.println("sending request");
        pr.println(request);

        try {
            String response = br.readLine();
            System.out.println(response);
            if(response.startsWith("400")) {
                return;
            }
            response = br.readLine();
            System.out.println(response);
            if(response.startsWith("400") || response.startsWith("500")) {
                // command format was invalid
                System.out.println("Invalid request");
            } else {
                // valid command format acknowledged from server
                System.out.println("Server received and accepted upload request");
                String fileName = request.split(" ")[1];
                File file = new File("files/" + fileName);

                // server is expecting a string
                if(!file.exists()) {
                    // send file not exist message
                    System.out.println("File not found");
                    pr.println("No such file");
                } else {
                    // message the server that file transfer is going to start
                    pr.println("start");

                    // file upload starting
                    FileInputStream fis = new FileInputStream(file);
                    System.out.println("Sending " + file.length() + " bytes of data");
                    dos.writeLong(file.length());
                    dos.flush();
                    int bytes = 0;
                    byte[] buffer = new byte[CHUNKSIZE];

                    while ((bytes = fis.read(buffer)) != -1) {
                        dos.write(buffer, 0, bytes);
                        dos.flush();
                    }
                    System.out.println("File sent");
                    fis.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {

            try {
                System.out.println(br.readLine());
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


    }
}
