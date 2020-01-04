package com.company;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;


public class ProxyServer {

    int port;
    //The socket to connect to this server
    ServerSocket serverSocket;
    String[] words;
    String cacheDir;
    boolean useCache;

    ProxyServer(int port, String[] words, String cacheDir, boolean useCache) throws IOException {
        this.port = port;
        this.words = words;
        this.cacheDir = cacheDir;
        this.serverSocket = new ServerSocket(port);
        this.useCache = useCache;

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
                //whole request that came from the browser
                String request = "";
                //the name of the file to cache
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
                        int firstSpace = data.indexOf(' ');
                        int lastSpace = data.lastIndexOf(' ');
                        String fileExtension = "";
                        fileName = data.substring(firstSpace + 1, lastSpace);

                        //get file type
                        if(fileName.charAt(fileName.length() - 1) != '/'){
                            //that's some file that has extension
                            int idx = fileName.lastIndexOf('.');
                            fileExtension = fileName.substring(idx);
                            fileName = fileName.substring(0, idx);
                        }

                        fileName = fileName.replace("/", "_");
                        fileName = fileName.replace(".", "_");
                        fileName = fileName.replace(":", "_");
                        fileName = fileName.replace("?", "_");

                        fileName = fileName + fileExtension;
                        System.out.println("result word: " + fileName);
                    }else if(data.contains("Accept-Encoding")){
                        data = "Accept-Encoding: identity";
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
                System.out.println("request: " + request + "\nend request");


                //Wait for response. I don't want to have some random sleep here
                while (serverIn.available() == 0) {

                }

                BufferedReader bufferedReaderServer = new BufferedReader(new InputStreamReader(serverIn));
                BufferedWriter clientWriter = new BufferedWriter(new OutputStreamWriter(clientOut));

                File cacheFile = new File(cacheDir + "\\" + fileName);
                System.out.println("path: " + cacheDir + "\\" + fileName);

                //check if the file exists
                String data;
                if(useCache){
                    if (cacheFile.exists()) {
                        //return the saved file
                        System.out.println("File exists");
                        BufferedReader cachedFileBufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(cacheFile)));
                        while (cachedFileBufferedReader.ready()) {
                            data = cachedFileBufferedReader.readLine() + "\n";

                            clientWriter.write(data);
                        }
                    } else {
                        cacheFile.getParentFile().mkdirs();
                        cacheFile.createNewFile();
                        System.out.println("creating cache file: " + cacheFile.getAbsolutePath());
                        FileWriter fileWriter = new FileWriter(cacheFile);

                        while (bufferedReaderServer.ready()) {
                            //let's send the data back to client and to cache
                            try {
                                Thread.sleep(5);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            data = bufferedReaderServer.readLine() + "\n";

                            clientWriter.write(data);
                            fileWriter.write(data);
                        }
                        fileWriter.flush();
                        fileWriter.close();
                    }
                }else{
                    while (bufferedReaderServer.ready()) {
                        //let's send the data back to client and to cache
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        data = bufferedReaderServer.readLine() + "\n";
                        clientWriter.write(data);
                    }

                }
                clientWriter.flush();
                System.out.println("finished");

            }
        }


    }


}
