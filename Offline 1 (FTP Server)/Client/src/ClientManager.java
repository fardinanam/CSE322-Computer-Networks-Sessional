import java.io.*;
import java.net.Socket;

public class ClientManager extends Thread {
    private final Socket socket;
    private final BufferedReader br;
    private final PrintWriter pr;
    private final DataOutputStream dos;
    private final int PORT = 5087;
    private final String SERVER_IP = "localhost";
    private final int CHUNK_SIZE = 4096;
    private final String request;
    private final boolean DEBUG = false;

    public ClientManager(String request) throws IOException {
        socket = new Socket(SERVER_IP, PORT);
        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        pr = new PrintWriter(socket.getOutputStream(), true);
        dos = new DataOutputStream(socket.getOutputStream());
        this.request = request;
    }

    private void logResponse(String response) {
        if (DEBUG) {
            System.out.println("Server says: " + response);
        }
    }

    @Override
    public void run() {
        // send the command
        System.out.println("sending request");
        pr.println(request);

        try {
            String response = br.readLine();
            logResponse(response);
            if(response.startsWith("400")) {
                return;
            }
            response = br.readLine();
            logResponse(response);
            if(response.startsWith("400") || response.startsWith("500")) {
                // command format was invalid
                logResponse(response);
            } else {
                // valid command format acknowledged from server
                System.out.println("Server received and accepted upload request");
                String fileName = request.split(" ", 2)[1];
                File file = new File(fileName);

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
                    byte[] buffer = new byte[CHUNK_SIZE];
                    System.out.println();
                    while ((bytes = fis.read(buffer)) != -1) {
                        dos.write(buffer, 0, bytes);
                        dos.flush();
                    }
                    System.out.println("\nFile sent");
                    fis.close();
                }
            }
        } catch (IOException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        } finally {
            try {
                System.out.println(br.readLine());
            } catch (IOException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        }

        try {
            br.close();
            pr.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
