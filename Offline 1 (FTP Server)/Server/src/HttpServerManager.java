import java.io.*;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.Date;
import java.util.Objects;

public class HttpServerManager extends Thread {
    private final Socket socket;
    public HttpServerManager(Socket socket) {
        this.socket = socket;
    }

    /**
     * Iterates through the folder of a specified path and returns a html content
     * in a list of links as a string.
     * @param path the path of the folder to be iterated through
     * @return a string of html content
     */
    private String generateFileNamesAsHtmlList(String path) throws NoSuchFileException {
        System.out.println("Requested path: " + path);
        if(path.equals("/")) {
            return "<a href=\"root\">root</a><br>";
        }
        File folder = new File(path.substring(1));

        if(!folder.exists()) {
            throw new NoSuchFileException("No such file or directory");
        }

        File[] listOfFiles = folder.listFiles();



        StringBuilder sb = new StringBuilder();
        sb.append(path).append(">");

        if(listOfFiles == null) {
            sb.append("<p>Empty folder</p>");
            return sb.toString();
        }

        sb.append("\r\n<ul>\r\n");

        for (File file : listOfFiles) {
            if (file.isFile()) {
                sb.append("<li>\r\n");
                sb.append("<a href=\"").append(path).append("/").append(file.getName()).append("\">");
                sb.append(file.getName());
                sb.append("</a>\r\n</li>\r\n");
            } else if (file.isDirectory()) {
                sb.append("<li style=\"font-weight:bold; font-style:italic;\">\r\n");
                sb.append("<a href=\"").append(path).append("/").append(file.getName()).append("\">");
                sb.append(file.getName());
                sb.append("</a>\r\n</li>\r\n");
            }
        }
        sb.append("</ul>\r\n");
        return sb.toString();
    }

    private String generateHtmlResponseHeader(String responseType, int contentLength) {
        String responseHeader = "HTTP/1.1 " + responseType + "\r\n" +
                "Server: Java HTTP Server: 1.1\r\n" +
                "Date: " + new Date() + "\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + contentLength + "\r\n" +
                "\r\n";
        return responseHeader;
    }

    private String generateHtml(String path) throws NoSuchFileException {
        File file = new File("index.html");
        StringBuilder html = new StringBuilder();

        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));

            String line;

            // Append the index.html content in a string
            while ((line = br.readLine()) != null) {
                html.append(line).append("\r\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        html.append(generateFileNamesAsHtmlList(Objects.requireNonNullElse(path, "/")));
        html.append("</body>\r\n</html>\r\n");
        return html.toString();
    }

    private String generateHtmlResponse(String request) {
        StringBuilder htmlResponse = new StringBuilder();
        if(request.startsWith("GET")) {
            String path = request.split(" ")[1];
            String html;
            try {
                html = generateHtml(path);
                htmlResponse.append(generateHtmlResponseHeader("200 OK", html.length()));
                htmlResponse.append(html);
            } catch (NoSuchFileException e) {
                htmlResponse.append(generateHtmlResponseHeader("404 Not Found", 0));
            }

        }
        return htmlResponse.toString();
    }

    @Override
    public void run() {
        try {
            while (true) {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter pr = new PrintWriter(socket.getOutputStream());
                String input = in.readLine();

                if (input == null) break;
                System.out.println("input : " + input);
                if (input.length() > 0) {
                    pr.write(generateHtmlResponse(input));
                    System.out.println("Response sent");
                    pr.flush();
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
