import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.Date;
import java.util.Objects;

public class HttpServerManager extends Thread {
    private final Socket socket;
    private final BufferedReader br;
    private final PrintWriter pw;
    private final DataOutputStream dos;
    private final int chunkSize = 1024;
    public HttpServerManager(Socket socket) throws IOException {
        this.socket = socket;
        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        pw = new PrintWriter(socket.getOutputStream());
        dos = new DataOutputStream(socket.getOutputStream());
    }

    /**
     * Iterates through the folder of a specified path and returns a html content
     * in a list of links as a string.
     * @param path the path of the folder to be iterated through
     * @return a string of html content
     * @throws NoSuchFileException if the path does not exist
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

    /**
     * Returns the response header for a request.
     * @param responseType the type of response. e.g: 200 OK, 404 Not Found
     * @param contentLength the length of the html content
     * @return a string of the response header
     */
    private String generateHtmlResponseHeader(String responseType, String contentType, int contentLength) {
        String responseHeader = "HTTP/1.1 " + responseType + "\r\n" +
                "Server: Java HTTP Server: 1.1\r\n" +
                "Date: " + new Date() + "\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + contentLength + "\r\n" +
                "\r\n";
        return responseHeader;
    }

    /**
     * Generates an HTML page content for a specified path.
     * @param path the path of the folder to be iterated through
     * @return a string of html content
     * @throws NoSuchFileException if the path does not exist
     */
    private String generateHtmlContent(String path) throws NoSuchFileException {
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

    private String fileNameToMime(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
        switch (extension) {
            case "txt":
            case "docx":
                return "text/plain";
            case "pdf":
                return "application/pdf";
            case "png":
            case "bpm":
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            default:
                return "text/html";
        }
    }

    /**
     * Generates a response for a GET request.
     * @param request the request string
     */
    private void handleGetRequest(String request) {
        StringBuilder htmlResponse = new StringBuilder();
        if(request.startsWith("GET")) {
            System.out.println("GET request : " + request);
            String path = request.split(" ")[1];
            try {
                if(path.endsWith(".pdf") || path.endsWith(".txt") || path.endsWith(".docx") ||path.endsWith(".jpg") || path.endsWith(".png") || path.endsWith(".jpeg")) {
                    File file = new File(path.substring(1));
                    if(!file.exists()) {
                        throw new NoSuchFileException("No such file or directory");
                    }

                    FileInputStream fis = new FileInputStream(file);

                    htmlResponse.append(generateHtmlResponseHeader("200 OK", fileNameToMime(path), (int) file.length()));
                    pw.write(htmlResponse.toString());
                    pw.flush();
                    byte[] buffer = new byte[chunkSize];
                    int bytes;
                    while ((bytes = fis.read(buffer)) != -1) {
                        dos.write(buffer, 0, bytes);
                        dos.flush();
                    }
                } else {
                    String html;

                    html = generateHtmlContent(path);
                    htmlResponse.append(generateHtmlResponseHeader("200 OK", "text/html", html.length()));
                    htmlResponse.append(html);
                    pw.write(htmlResponse.toString());
                    pw.flush();
                }
            } catch (NoSuchFileException | FileNotFoundException e) {
                htmlResponse.append(generateHtmlResponseHeader("404 Not Found", "", 0));
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    private void handleRequest(String request) {
        if(request.startsWith("GET")) {
            handleGetRequest(request);
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                String input = br.readLine();
                if (input == null) continue;
                if (input.length() > 0) {
                    handleRequest(input);
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
