import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;

public class Main {

    public static void main(String[] args) {
        System.out.println("Running main Application!");
        try {
            ServerSocket serverSocket = new ServerSocket(4221);

            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);

            Socket socket = serverSocket.accept(); // Wait for connection from client.
            System.out.println("Accepted new connection");

            processRequestResponse(socket);
        } catch (IOException | URISyntaxException e) {
            System.out.println("Failed to Process request : " + e.getMessage());
        }
    }

    private static void processRequestResponse(Socket socket) throws IOException, URISyntaxException {
        BufferedReader inReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        OutputStream outWriter = socket.getOutputStream();

        // Reading the request
        String requestMessage = inReader.readLine();
        System.out.println("Request message: " + requestMessage);
        HttpRequest request = parseRequest(socket, requestMessage);

        // Writing the response
        String responseMessage = "";
        if (request.uri().getPath().equals("/") || request.uri().getPath().equals("/index.html")) {
            responseMessage = "HTTP/1.1 200 OK\r\n\r\n";
        } else if (request.uri().getPath().startsWith("/echo")) {
            String responseString = request.uri().getPath().split("/")[2];
            int length = responseString.length();
            responseMessage = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: "+length+"\r\n\r\n"+responseString;
        } else {
            responseMessage = "HTTP/1.1 404 Not Found\r\n\r\n";
        }
        outWriter.write(responseMessage.getBytes());
        outWriter.flush();
        System.out.println("Response returned: " + responseMessage);
    }

    private static HttpRequest parseRequest(Socket socket, String requestMessage) throws URISyntaxException {
        Builder httpRequestBuilder = HttpRequest.newBuilder();
        String[] requestArray = requestMessage.split("\r\n");
        String[] hostDetailsArray =  requestArray[0].split(" ");
        String methodName = hostDetailsArray[0];
        String path = hostDetailsArray[1];

        switch (methodName) {
            case "GET":
                httpRequestBuilder.GET();
                break;
            default:
                throw new IllegalArgumentException("Unknown method name: " + methodName);
        }

        URI uri = new URI("http", null, socket.getInetAddress().getHostAddress(), -1, path, null, null);
        httpRequestBuilder.uri(uri);

        String httpVersion = hostDetailsArray[2];
        switch (httpVersion) {
            case "HTTP/1.1":
                httpRequestBuilder.version(Version.HTTP_1_1);
                break;
            default:
                throw new IllegalArgumentException("Unknown Http version: " + httpVersion);
        }

        for(int i = 1; i < requestArray.length; i++) {
            String[] headerKV = requestArray[i].split(": ");
            httpRequestBuilder.setHeader(headerKV[0], headerKV[1]);
        }

        return httpRequestBuilder.build();
        }
}
