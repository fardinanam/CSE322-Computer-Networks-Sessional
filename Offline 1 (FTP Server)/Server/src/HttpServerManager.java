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
    private final FileWriter logFile;
    private final int CHUNK_SIZE = 4096;
    private final boolean DEBUG = false;

    public HttpServerManager(Socket socket) throws IOException {
        this.socket = socket;
        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        pw = new PrintWriter(socket.getOutputStream(), true);
        dos = new DataOutputStream(socket.getOutputStream());
        dis = new DataInputStream(socket.getInputStream());
        // create a new log file
        logFile = new FileWriter("log.txt", true);
    }

    private void log(String message) {
        try {
            logFile.write(message + "\n");
        } catch (IOException e) {
            System.out.println("Error writing to log file");
        }
    }
    private void printResponse(String response) {
        System.out.println("Sending: " + response);
        pw.println(response);
        pw.flush();
    }

    private void writeResponseAndLog(String response) {
        log("-----------------------------\nResponse: " + response);
        pw.write(response);
        pw.flush();
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
        StringBuilder htmlContent = new StringBuilder();

        if(path.equals("/")) {
            htmlContent.append("<ul><li><a class=\"folder\" href=\"root\">root</a></li></ul>");
            htmlContent.append("<ul><li><a class=\"folder\" href=\"uploaded\">uploaded</a></li></ul>");
            return htmlContent.toString();
        }
        File folder = new File(path.substring(1));

        if(!folder.exists()) {
            System.out.println("Requested path does not exist");
            throw new FileNotFoundException("No such file or directory");
        }

        File[] listOfFiles = folder.listFiles();


        htmlContent.append("<p class=\"directory\">").append(path).append("</p>\r\n");

        if(listOfFiles == null) {
            System.out.println("listOfFiles is null");
            htmlContent.append("<p class=\"empty\">Empty folder</p>");
            return htmlContent.toString();
        }

        htmlContent.append("<ul>\r\n");

        for (File file : listOfFiles) {
            if (file.isFile()) {
                htmlContent.append("<li>\r\n");
                htmlContent.append("<a href=\"").append(path).append("/").append(file.getName()).append("\" target=\"blank\">");
                htmlContent.append(file.getName());
                htmlContent.append("</a>\r\n</li>\r\n");
            } else if (file.isDirectory()) {
                htmlContent.append("<li >\r\n");
                htmlContent.append("<a class=\"folder\" href=\"").append(path).append("/").append(file.getName()).append("\">");
                htmlContent.append(file.getName());
                htmlContent.append("</a>\r\n</li>\r\n");
            }
        }
        htmlContent.append("</ul>\r\n");
        return htmlContent.toString();
    }

    /**
     * Returns the response header for a request.
     * @param responseType the type of response. e.g: 200 OK, 404 Not Found
     * @param contentLength the length of the html content
     * @return a string of the response header
     */
    private String generateHtmlResponseHeader(String responseType, String contentType, int contentLength) {
        String contentDisposition = contentType.contains("pdf") || contentType.contains("video") ? "attachment" : "inline";
        String responseHeader = "HTTP/1.1 " + responseType + "\r\n" +
                "Server: Java HTTP Server: 1.1\r\n" +
                "Date: " + new Date() + "\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Disposition: " + contentDisposition + "\r\n" +
                "Content-Length: " + contentLength + "\r\n" +
                "\r\n";
        return responseHeader;
    }

    /**
     * Generates an HTML page content for a specified path.
     * @param path the path of the folder to be iterated through
     * @return a string of html content
     * @throws FileNotFoundException if the path does not exist
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
            if(DEBUG) {
                e.printStackTrace();
            }
        }

        html.append(generateFileNamesAsHtmlList(path));
        html.append("</body>\r\n</html>\r\n");
        return html.toString();
    }

    private String fileNameToMime(String fileName) {
        String extension = fileName.toLowerCase().substring(fileName.lastIndexOf(".") + 1);
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
            case "mov":
                return "video/quicktime";
            default:
                return "text/html";
        }
    }

    private boolean isValidFileType(String fileName) {
        if(!fileName.contains(".")) {
            return false;
        }

        String extension = fileName.toLowerCase().substring(fileName.lastIndexOf(".") + 1);
        switch (extension) {
            case "txt":
            case "docx":
            case "pdf":
            case "png":
            case "bpm":
            case "jpg":
            case "jpeg":
            case "mp4":
            case "mov":
                return true;
            default:
                return false;
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
                if(isValidFileType(path)) {
                    File file = new File(path.substring(1));
                    if(!file.exists()) {
                        throw new NoSuchFileException("No such file or directory");
                    }

                    FileInputStream fis = new FileInputStream(file);

                    htmlResponse.append(generateHtmlResponseHeader("200 OK",
                            fileNameToMime(path), (int) file.length()));
                    writeResponseAndLog(htmlResponse.toString());
                    byte[] buffer = new byte[CHUNK_SIZE];
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
                    writeResponseAndLog(htmlResponse.toString());
                }
            } catch (FileNotFoundException e) {
                htmlResponse.append(generateHtmlResponseHeader("404 Not Found", "", 0));
                writeResponseAndLog(htmlResponse.toString());
            } catch (Exception e) {
                if(DEBUG) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Communicates with the client while uploading a file. Firstly it checks the command format of the
     * request. If the request is valid, it sends "start" to the client. Otherwise, sends a "400 Bad Request"
     * error message. Then it waits for an acknowledgement from the client. If the client sends "start", it
     * starts to receive the file. Otherwise, it ends the connection.
     * @param request the request string
     */
    private void handleUploadRequest(String request) {
        // command received
        String[] requestSegments = request.split(" ", 2);

        if(requestSegments.length != 2) {
            // invalid command format
            printResponse("400 Bad Request");
            return;
        }

        String fileName = requestSegments[1];

        if(isValidFileType(fileName)) {
            // Check if the "uploaded" folder exists. If not, create it.
            File uploadedFolder = new File("uploaded");
            if(!uploadedFolder.exists()) {
                if(!uploadedFolder.mkdir()) {
                    System.out.println("Failed to create the uploaded folder");
                    printResponse("500 Internal Server Error");
                    return;
                }
            }

            // A valid request and command format
            System.out.println("starting to handle upload");
            printResponse("start");

            FileOutputStream fos = null;
            long size = -1;
            try {
                // expecting a message from the client
                if(!br.readLine().equals("start")) {
                    // client couldn't transfer the file
                    System.out.println("Something went wrong from client");
                    return;
                }

                // Starting to upload file
                int bytes = 0;
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                fos = new FileOutputStream("uploaded/" + fileName);
                size = dis.readLong();
                System.out.println("Receiving " + size + " bytes of data");
                byte[] buffer = new byte[CHUNK_SIZE];
                while (size > 0 && (bytes = dis.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                    fos.write(buffer, 0, bytes);
                    size -= bytes;
                }
            } catch (IOException e) {
                System.out.println("Could not complete upload");
                if(DEBUG) {
                    e.printStackTrace();
                }
            } finally {
                if(size != 0) {
                    printResponse("417 upload failed");
                } else {
                    printResponse("200 upload successful");
                }
                try {
                    if (fos != null) {
                        fos.close();
                        System.out.println("File closed");
                    }
                } catch (IOException e) {
                    if(DEBUG) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            // invalid file type
            printResponse("400 Bad Request");
        }
    }
    private void handleRequest(String request) {
        log("===============================\nRequest: " + request);
        if(request.startsWith("GET")) {
            handleGetRequest(request);
        } else if(request.startsWith("UPLOAD")) {
            printResponse("Command received");
            handleUploadRequest(request);
        } else {
            System.out.println("Invalid request");
            printResponse("400 Invalid command");
        }
    }

    @Override
    public void run() {
        try {
            String input = br.readLine();
            if (input != null && input.length() > 0) {
                handleRequest(input);
            }
        } catch (IOException e) {
            if(DEBUG) {
                e.printStackTrace();
            }
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        }

        try {
            logFile.close();
            br.close();
            dos.close();
            dis.close();
            pw.close();
            socket.close();
        } catch (IOException e) {
            if(DEBUG) {
                e.printStackTrace();
            }
        }
    }
}
