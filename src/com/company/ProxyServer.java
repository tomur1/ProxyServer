package com.company;

import sun.net.www.MessageHeader;

import javax.xml.ws.spi.http.HttpHandler;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLOutput;
import java.util.Arrays;
import java.util.Scanner;

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

        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("new client connected");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        new ClientHandler(socket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }
    }

    //one client is connected to the handler.
    class ClientHandler {

        int bufferSize = 1024;
        Socket clientSocket;
        Socket webSocket;
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
        void handle() throws IOException {
            byte[] buffer = new byte[bufferSize];
            int length;
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientIn));
            StringBuilder requestBuilder = new StringBuilder();
            String request = "";
            while (bufferedReader.ready()) {

                String data = bufferedReader.readLine();

                if (data.contains("Host:")) {
                    //save the host name
                    //80 is the http port
                    webSocket = new Socket(data.substring(6), 80);
                    serverIn = webSocket.getInputStream();
                    serverOut = webSocket.getOutputStream();
                }

                requestBuilder.append(data + "\n");

                System.out.println("SOME DATA INCOMING");

            }
            request = requestBuilder.toString();
            //assuming we have all that we need let's check if we have a cache for the response

            OutputStreamWriter writer = new OutputStreamWriter(serverOut);
            if (request != null) {
                writer.write(request);
                writer.flush();
                System.out.println("request: " + request);
                System.out.println("send ");

            } else {
                throw new NullPointerException("I want to know about this");
            }



            //Wait for response. I don't want to have some random sleep here
            while(serverIn.available() == 0){

            }

            BufferedReader bufferedReaderServer = new BufferedReader(new InputStreamReader(serverIn));
            while (bufferedReaderServer.ready()) {
                System.out.println(bufferedReaderServer.readLine());
            }

            System.out.println("finished");
        }


    }


}
