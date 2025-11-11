import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {
        System.out.println("Running main Application!");
        try {
            ServerSocket serverSocket = new ServerSocket(4221);

            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);
            while (true) {
                Socket socket = serverSocket.accept(); // Wait for connection from client.
                System.out.println("Accepted new connection");

                Thread thread = new Thread(() -> {
                    try {
                        processRequestResponse(socket,args);
                    } catch (IOException | URISyntaxException e) {
                        System.out.println("Failed to Process request : " + e.getMessage());
                    }
                });
                thread.start();
            }
        } catch (IOException e) {
            System.out.println("Failed to Process request : " + e.getMessage());
        }
    }

    private static void processRequestResponse(Socket socket, String[] args) throws IOException, URISyntaxException {
        System.out.println("Processing request");
        // Reading the request
        String requestMessage = readRequest(socket.getInputStream());
        System.out.println("Request message: " + requestMessage);

        // Setting the output stream
        OutputStream outWriter = socket.getOutputStream();

        HttpRequest request = parseRequest(socket, requestMessage);

        // Writing the response
        String responseMessage = "";
        if (request.uri().getPath().equals("/") || request.uri().getPath().equals("/index.html")) {
            responseMessage = "HTTP/1.1 200 OK\r\n\r\n";
        } else if (request.uri().getPath().startsWith("/echo")) {
            String responseString = request.uri().getPath().split("/")[2];
            responseMessage = getSuccessPrefix("text/plain") + responseString.length() + "\r\n\r\n" + responseString;
        } else if (request.uri().getPath().startsWith("/user-agent")) {
            System.out.println("User-Agent is: " + request.headers().map());
            String userAgent = request.headers().firstValue("User-Agent").get();
            responseMessage = getSuccessPrefix("text/plain") + userAgent.length() + "\r\n\r\n" + userAgent;
        } else if (request.uri().getPath().startsWith("/files")) {
            responseMessage = handleFileRequestAndGetResponse(request.uri().getPath(), args);
        } else {
            responseMessage = "HTTP/1.1 404 Not Found\r\n\r\n";
        }
        outWriter.write(responseMessage.getBytes());
        outWriter.flush();
        System.out.println("Response returned: " + responseMessage);
    }

    private static String handleFileRequestAndGetResponse(String path, String[] args) {
        String filePath = path.split("/")[2];
        String content = readFileContent(args[1]+filePath);
        if(content == null) {
            return "HTTP/1.1 404 Not Found\r\n\r\n";
        }
        return getSuccessPrefix("application/octet-stream") + content.length() + "\r\n\r\n" + content;
    }

    private static String readRequest(InputStream inputStream) throws IOException {
        String requestLine = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            requestLine += line + "\r\n";
        }

        return requestLine;
    }

    private static String getSuccessPrefix(String contentType) {
        return "HTTP/1.1 200 OK\r\nContent-Type: " + contentType + "\r\nContent-Length: ";
    }

    private static HttpRequest parseRequest(Socket socket, String requestMessage) throws URISyntaxException {
        Builder httpRequestBuilder = HttpRequest.newBuilder();
        String[] requestArray = requestMessage.split("\r\n");
        String[] requestDetailsArray = requestArray[0].split(" ");
        String methodName = requestDetailsArray[0];
        String path = requestDetailsArray[1];

        switch (methodName) {
            case "GET":
                httpRequestBuilder.GET();
                break;
            default:
                throw new IllegalArgumentException("Unknown method name: " + methodName);
        }

        URI uri = new URI("http", null, socket.getInetAddress().getHostAddress(), -1, path, null, null);
        httpRequestBuilder.uri(uri);

        String httpVersion = requestDetailsArray[2];
        switch (httpVersion) {
            case "HTTP/1.1":
                httpRequestBuilder.version(Version.HTTP_1_1);
                break;
            default:
                throw new IllegalArgumentException("Unknown Http version: " + httpVersion);
        }

        for (int i = 1; i < requestArray.length; i++) {
            String[] headerKV = requestArray[i].split(": ");
            if (headerKV[0].equalsIgnoreCase("Host")) {
                continue;
            }
            httpRequestBuilder.setHeader(headerKV[0], headerKV[1]);
        }

        return httpRequestBuilder.build();
    }

    private static String readFileContent(String file) {
        try {
            return new String(Files.readAllBytes(Paths.get(file)));
        } catch (IOException e) {
            System.out.println("Failed to read file: " + e.getMessage());
            return null;
        }
    }
}
