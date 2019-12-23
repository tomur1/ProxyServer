import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ProxyServer {

    int port;
    //The socket to connet to this server
    ServerSocket serverSocket;
    String[] words;
    String cacheDir;

    ProxyServer(int port, String[] words, String cacheDir) throws IOException {
        this.port = port;
        this.words = words;
        this.cacheDir = cacheDir;
        this.serverSocket = new ServerSocket(port);

        while (true){
            new ClientHandler(serverSocket.accept());
        }
    }

    //one client is connected to the handler.
    class ClientHandler {

        int bufferSize = 1024;
        Socket clientSocket;
        InputStream clientIn;
        OutputStream clientOut;
        InputStream serverIn;
        OutputStream serverOut;
        ClientHandler(Socket clientSocket) throws IOException {
            this.clientSocket = clientSocket;
            this.clientIn = clientSocket.getInputStream();
            this.clientOut = clientSocket.getOutputStream();
            handle();
        }

        //handles the connection when client want to access the web
        void handle(){
            byte[] buffer = new byte[bufferSize];
            while (true){

                try {
                    if (clientIn.read(buffer) != -1){

                    }


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }








}
