package com.company;

import sun.net.www.MessageHeader;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLOutput;
import java.util.Arrays;

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
        void handle(){
            byte[] buffer = new byte[bufferSize];
            int length;
            while (true){

                try {
                    if ((length = clientIn.read(buffer)) != -1){
                        System.out.println("SOME DATA INCOMING");
                        MessageHeader msgServer = new MessageHeader(clientIn);
                        String host = msgServer.findValue("Host");

                        if (webSocket == null || webSocket.isClosed()){
                            System.out.println("new Host: " + host);
                            webSocket = new Socket(host, 80);
                            serverIn = webSocket.getInputStream();
                            serverOut = webSocket.getOutputStream();
                        }

                        //send data to the web and get response
                        serverOut.write(buffer, 0, length);
                        serverOut.flush();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }








}
