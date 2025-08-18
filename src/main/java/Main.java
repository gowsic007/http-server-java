import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

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
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static void processRequestResponse(Socket socket) throws IOException {
        BufferedInputStream in = new BufferedInputStream(socket.getInputStream());

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        // Reading the request
        String requestMessage = in.readAllBytes().toString();
        System.out.println("Request message: " + requestMessage);

        // Writing the response
        String responseMessage = "HTTP/1.1 200 OK\r\n\r\n";
        out.write(responseMessage);
        System.out.println("Response returned: " + responseMessage);
    }
}
