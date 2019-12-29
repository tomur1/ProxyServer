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

            while (true) {


                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientIn));
                StringBuilder requestBuilder = new StringBuilder();
                String request = "";
                String fileName = "";
                boolean readSomething = false;
                while (bufferedReader.ready()) {

                    String data = bufferedReader.readLine();

                    if (data.contains("Host:")) {
                        //save the host name
                        //80 is the http port
                        webSocket = new Socket(data.substring(6), 80);
                        serverIn = webSocket.getInputStream();
                        serverOut = webSocket.getOutputStream();
                    }else if(data.contains("HTTP")){
                        readSomething = true;
                        System.out.println("!!!!!!!!!!!!!!!" + data);
                        int firstSpace = data.indexOf(' ');
                        int lastSpace = data.lastIndexOf(' ');
                        fileName = data.substring(firstSpace + 1, lastSpace);

                        fileName = fileName.replace("/", "_");
                        fileName = fileName.replace(".", "_");
                        fileName = fileName.replace(":", "_");

                        System.out.println("result word: " + fileName);
                    }


                    requestBuilder.append(data + "\n");

                }

                if(!readSomething){
                    break;
                }


                request = requestBuilder.toString();
                //assuming we have all that we need let's check if we have a cache for the response

                OutputStreamWriter writer = new OutputStreamWriter(serverOut);

                writer.write(request);
                writer.flush();
                System.out.println("request: " + request + "\n end request");


                //Wait for response. I don't want to have some random sleep here
                while (serverIn.available() == 0) {

                }

                BufferedReader bufferedReaderServer = new BufferedReader(new InputStreamReader(serverIn));
                BufferedWriter clientWriter = new BufferedWriter(new OutputStreamWriter(clientOut));

                File cacheFile = new File(cacheDir + "\\" + fileName);
                System.out.println("path: " + cacheDir + "\\" + fileName);

                //check if the file exists
                if (cacheFile.exists()) {
                    //return the saved file
                    System.out.println("File exists");
                    BufferedReader cachedFileBufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(cacheFile)));
                    while (cachedFileBufferedReader.ready()) {

                        clientWriter.write(cachedFileBufferedReader.readLine());
                    }
                } else {
                    cacheFile.getParentFile().mkdirs();
                    cacheFile.createNewFile();
                    System.out.println("creating cache file: " + cacheFile.getAbsolutePath());
                    FileWriter fileWriter = new FileWriter(cacheFile);
                    String data;
                    while (bufferedReaderServer.ready()) {
                        //let's send the data back to client and to cache
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        data = bufferedReaderServer.readLine() + "\n";
                        clientWriter.write(data);
                        fileWriter.write(data);
                        if(data.equals("\n")){
                            clientWriter.flush();
                        }
                    }
                    fileWriter.flush();
                    fileWriter.close();
                }
                clientWriter.flush();

                System.out.println("finished");

            }
        }


    }


}
