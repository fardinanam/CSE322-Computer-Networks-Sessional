import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.Date;

public class HttpServerManager extends Thread {
    private final Socket socket;
    private final BufferedReader br;
    private final PrintWriter pw;
    private final DataOutputStream dos;
    private final DataInputStream dis;
    private final int CHUNKSIZE = 4096;
    public HttpServerManager(Socket socket) throws IOException {
        this.socket = socket;
        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        pw = new PrintWriter(socket.getOutputStream(), true);
        dos = new DataOutputStream(socket.getOutputStream());
        dis = new DataInputStream(socket.getInputStream());
    }

    /**
     * Iterates through the folder of a specified path and returns a html content
     * in a list of links as a string.
     * @param path the path of the folder to be iterated through
     * @return a string of html content
     * @throws NoSuchFileException if the path does not exist
     */
    private String generateFileNamesAsHtmlList(String path) throws FileNotFoundException {
        System.out.println("Requested path: " + path);
        if(path.equals("/")) {
            return "<ul><li><a class=\"folder\" href=\"root\">root</a></li></ul>";
        }
        File folder = new File(path.substring(1));

        if(!folder.exists()) {
            System.out.println("Requested path does not exist");
            throw new FileNotFoundException("No such file or directory");
        }

        File[] listOfFiles = folder.listFiles();

        StringBuilder sb = new StringBuilder();
        sb.append("<p class=\"directory\">").append(path).append("</p>\r\n");
        System.out.println("listOfFiles: " + listOfFiles.toString());
        if(listOfFiles == null) {
            System.out.println("listOfFiles is null");
            sb.append("<p class=\"empty\">Empty folder</p>");
            return sb.toString();
        }

        sb.append("<ul>\r\n");

        for (File file : listOfFiles) {
            if (file.isFile()) {
                sb.append("<li>\r\n");
                sb.append("<a href=\"").append(path).append("/").append(file.getName()).append("\">");
                sb.append(file.getName());
                sb.append("</a>\r\n</li>\r\n");
            } else if (file.isDirectory()) {
                sb.append("<li >\r\n");
                sb.append("<a class=\"folder\" href=\"").append(path).append("/").append(file.getName()).append("\">");
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
    private String generateHtmlContent(String path) throws FileNotFoundException {
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

        html.append(generateFileNamesAsHtmlList(path));
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
            case "mp4":
                return "video/mp4";
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
                if(path.endsWith(".pdf") || path.endsWith(".txt") || path.endsWith(".docx") ||path.endsWith(".jpg")
                        || path.endsWith(".png") || path.endsWith(".jpeg") || path.endsWith(".mp4")) {
                    File file = new File(path.substring(1));
                    if(!file.exists()) {
                        throw new NoSuchFileException("No such file or directory");
                    }

                    FileInputStream fis = new FileInputStream(file);

                    htmlResponse.append(generateHtmlResponseHeader("200 OK",
                            fileNameToMime(path), (int) file.length()));
                    pw.write(htmlResponse.toString());
                    pw.flush();
                    byte[] buffer = new byte[CHUNKSIZE];
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
            } catch (FileNotFoundException e) {
                htmlResponse.append(generateHtmlResponseHeader("404 Not Found", "", 0));
                pw.write(htmlResponse.toString());
                pw.flush();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    private void handleUploadRequest(String request) {
        // command received
        String[] requestSegments = request.split(" ");
        String fileName = requestSegments[1];

        if(requestSegments.length > 2) {
            // invalid command format
            pw.println("400 Bad Request");
            pw.flush();
            return;
        }
        if(fileName.toLowerCase().endsWith(".txt") || fileName.toLowerCase().endsWith(".docx")
                ||fileName.toLowerCase().endsWith(".jpg")  || fileName.toLowerCase().endsWith(".png")
                || fileName.toLowerCase().endsWith(".jpeg") || fileName.toLowerCase().endsWith(".mp4")) {
            // A valid request and command format
            System.out.println("starting to handle upload");
            pw.println("start");
            pw.flush();

            FileOutputStream fos = null;
            try {
                // expecting a message from the client
                if(!br.readLine().equals("start")) {
                    // client couldn't transfer the file
                    System.out.println("Something went wrong from client");
                    return;
                }

                // file upload stating
                int bytes = 0;
                fos = new FileOutputStream("upload/" + fileName);
                long size = dis.readLong();
                byte[] buffer = new byte[CHUNKSIZE];
                while (size > 0 && (bytes = dis.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                    System.out.println("Bytes: " + bytes);
                    fos.write(buffer, 0, bytes);
                    size -= bytes;
                }
            } catch (IOException e) {
                System.out.println("Could not complete upload");
                e.printStackTrace();
//                pw.write("Could not complete upload");
//                pw.flush();
            } finally {
                try {
                    if (fos != null) fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            // invalid file type
            pw.println("400 Bad Request");
            pw.flush();
        }

        pw.println("408 close");
        pw.flush();
    }
    private void handleRequest(String request) {
        if(request.startsWith("GET")) {
            handleGetRequest(request);
        } else if(request.startsWith("UPLOAD")) {
            pw.println("Command received");
            pw.flush();
            handleUploadRequest(request);
        } else {
            System.out.println("Invalid request");
            pw.println("400 Invalid command");
            pw.flush();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                String input = br.readLine();
                System.out.println(input);
                if (input == null) break;
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
