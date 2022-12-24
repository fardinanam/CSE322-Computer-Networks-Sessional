import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class HttpServerManager extends Thread {
    private final Socket socket;
    public HttpServerManager(Socket socket) {
        this.socket = socket;
    }

    private String getFileNamesAsHtmlList(String path) {
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        StringBuilder sb = new StringBuilder();
        sb.append(path);
        sb.append("\r\n<ul>\r\n");
        for (File file : listOfFiles) {
            if (file.isFile()) {
                sb.append("<li>\r\n");
                sb.append(file.getName());
                sb.append("\r\n</li>\r\n");
            } else if (file.isDirectory()) {
                sb.append("<li>\r\n");
                sb.append(getFileNamesAsHtmlList(path + "/" + file.getName()));
                sb.append("\r\n</li>\r\n");
            }
        }
        sb.append("</ul>\r\n");
        return sb.toString();
    }

    @Override
    public void run() {
        File file = new File("index.html");
        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;

            // Append the index.html content in a string
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }

            sb.append(getFileNamesAsHtmlList("root"));
            sb.append("</body>\r\n</html>\r\n");

            while (true) {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter pr = new PrintWriter(socket.getOutputStream());
                String input = in.readLine();

                if (input == null) break;
                System.out.println("input : " + input);
                if (input.length() > 0) {
                    if (input.startsWith("GET")) {
                        pr.write("HTTP/1.1 200 OK\r\n");
                        pr.write("Server: Java HTTP Server: 1.1\r\n");
                        pr.write("Date: " + new Date() + "\r\n");
                        pr.write("Content-Type: text/html\r\n");
                        pr.write("Content-Length: " + sb.length() + "\r\n");
                        pr.write("\r\n");
                        pr.write(sb.toString());

                        System.out.println("Response sent");
                        pr.flush();
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e);
        } finally {
            System.out.println("One client left.");
            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
