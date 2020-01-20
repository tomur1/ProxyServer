package com.company;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;


public class ProxyServer {

    int port;
    //The socket to connect to this server
    ServerSocket serverSocket;
    String[] words;
    String cacheDir;
    boolean useCache;
    boolean runInHeavyMode;

    ProxyServer(int port, String[] words, String cacheDir, boolean useCache, boolean runInHeavyMode) throws IOException {
        this.port = port;
        this.words = words;
        this.cacheDir = cacheDir;
        this.serverSocket = new ServerSocket(port);
        this.useCache = useCache;
        this.runInHeavyMode = runInHeavyMode;

        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("new client connected");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        new ClientHandler(socket);
                    } catch (Exception e) {
                        e.printStackTrace();
                        try {
                            socket.close();
                            System.out.println("emergency socket close");
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
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


            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientIn));
            StringBuilder requestBuilder = new StringBuilder();
            //whole request that came from the browser
            String request = "";
            //the name of the file to cache
            String fileName = "";
            String fileExtension = "";
            URL requestURL = null;
            boolean isImage = false;
            boolean isText = false;
            boolean readSomething = false;
            while (bufferedReader.ready()) {

                String data = bufferedReader.readLine();

                if (data.contains("Host:")) {
                    //save the host name
                    //80 is the http port
                    System.out.println("!!!!!" + data);
                    String host = data.substring(6);
                    if (host.contains(":")) {
                        //i dont want to handle any connections with port
                        readSomething = false;
                        break;
                    }
                    webSocket = new Socket(data.substring(6), 80);
                    serverIn = webSocket.getInputStream();
                    serverOut = webSocket.getOutputStream();
                } else if (data.contains("HTTP")) {
                    readSomething = true;
                    int firstSpace = data.indexOf(' ');
                    int lastSpace = data.lastIndexOf(' ');

                    fileName = data.substring(firstSpace + 1, lastSpace);
                    System.out.println(fileName);
                    requestURL = new URL(fileName);

                    //get file type
                    if (fileName.charAt(fileName.length() - 1) != '/') {
                        //that's some file that has extension
                        int idx = fileName.lastIndexOf('.');
                        fileExtension = fileName.substring(idx);
                        fileName = fileName.substring(0, idx);
                        isImage = (fileExtension.contains(".png")) || fileExtension.contains(".jpg") ||
                                fileExtension.contains(".jpeg") || fileExtension.contains(".gif");
                        isText = (fileExtension.contains(".css")) || fileExtension.contains(".html") ||
                                fileExtension.contains(".txt") || fileExtension.contains(".scs") || fileExtension.contains(".js");
                        fileExtension = fileExtension.replace("?", "_");
                        fileExtension = fileExtension.replace("/", "_");
                        fileExtension = fileExtension.replace("=", "_");
                        fileExtension = fileExtension.replace("&", "_");
                    }

                    fileName = fileName.replace("/", "_");
                    fileName = fileName.replace(".", "_");
                    fileName = fileName.replace(":", "_");
                    fileName = fileName.replace("?", "_");
                    fileName = fileName.replace(",", "_");
                    fileName = fileName.replace("=", "_");
                    fileName = fileName.replace("-", "_");

                    if (fileName.length() > 100) {
                        fileName = fileName.substring(0, 100);
                    }

                    fileName = fileName + fileExtension;
                    System.out.println("result word: " + fileName);
                } else if (data.contains("Accept-Encoding")) {
                    data = "Accept-Encoding: identity";
                }


                requestBuilder.append(data + "\n");

            }

            if (!readSomething || (!runInHeavyMode && isImage)) {
                BufferedWriter clientWriter = new BufferedWriter(new OutputStreamWriter(clientOut));
                String notFound = "HTTP/1.0 404 NOT FOUND \r\n\n";
                clientWriter.write(notFound);
                clientWriter.flush();
                clientWriter.close();
                clientSocket.close();
                return;
            }

            request = requestBuilder.toString();
            //assuming we have all that we need let's check if we have a cache for the response

            OutputStreamWriter writer = new OutputStreamWriter(serverOut);

            writer.write(request);
            writer.flush();
            System.out.println("request: " + request + "\nend request");


            //Wait for response. I don't want to have some random sleep here
            long start = System.currentTimeMillis();
            while (serverIn.available() == 0) {
                if (System.currentTimeMillis() - start > 500) {
                    throw new IOException("Server did not respond");
                }
            }

            BufferedReader bufferedReaderServer = new BufferedReader(new InputStreamReader(serverIn));
            BufferedWriter clientWriter = new BufferedWriter(new OutputStreamWriter(clientOut));

            File cacheFile = new File(cacheDir + "\\" + fileName);
            System.out.println("path: " + cacheDir + "\\" + fileName);

            //check if the file exists
            String data;
            if (useCache) {
                if (cacheFile.exists()) {

                    //return the saved file
                    System.out.println("File exists");

                    if (isImage) {

                        BufferedImage loadedImage = ImageIO.read(cacheFile);

                        // Send response code to client
                        String line = "HTTP/1.0 200 OK\n" +
                                "Proxy-agent: ProxyServer/1.0\n" +
                                "\r\n";
                        clientWriter.write(line);
                        clientWriter.flush();

                        // Send them the image data
                        ImageIO.write(loadedImage, fileExtension.substring(1), clientOut);
                    } else {
                        //mark the page as cached
                        BufferedReader cachedFileBufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(cacheFile)));
                        while (cachedFileBufferedReader.ready()) {
                            data = cachedFileBufferedReader.readLine() + "\n";
                            data = filterWords(data);
                            if(data.contains("<title>")){
                                //let's get a place in thext where the html tag closes
                                //that way we can add an information that this page is cached
                                int tagOpens = data.indexOf("<title>");
                                String resultData = "";
                                resultData = data.substring(0, tagOpens + "<title>".length());
                                resultData += "Ta strona zostala załadowana z cache ";
                                resultData += data.substring(tagOpens + "<title>".length());
                                data = resultData;
                            }

                            clientWriter.write(data);
                        }
                    }


                } else {
                    //does not exist yet
                    if (isImage) {


                        BufferedImage loadedImage = ImageIO.read(requestURL);

                        // Send response code to client
                        String line = "HTTP/1.0 200 OK\n" +
                                "Proxy-agent: ProxyServer/1.0\n" +
                                "\r\n";
                        clientWriter.write(line);
                        clientWriter.flush();

                        // Send them the image data
                        ImageIO.write(loadedImage, fileExtension.substring(1), clientOut);
                        ImageIO.write(loadedImage, fileExtension.substring(1), cacheFile);

                    } else {
                        //text file
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
                            //add filtering
                            data = filterWords(data);
                            System.out.println(data);
                            clientWriter.write(data);
                            fileWriter.write(data);
                        }
                        fileWriter.flush();
                        fileWriter.close();
                    }

                }

            } else {

                if (isImage) {


                    BufferedImage loadedImage = ImageIO.read(requestURL);
                    // Send response code to client
                    String line = "HTTP/1.1 200 OK\n" +
                            "Proxy-agent: ProxyServer/1.0\n" +
                            "\r\n";
                    clientWriter.write(line);
                    clientWriter.flush();

                    // Send them the image data
                    ImageIO.write(loadedImage, fileExtension.substring(1), clientOut);

                } else {
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

            }
            clientWriter.flush();
            clientWriter.close();
            System.out.println("finished");

        }
    }

    String filterWords(String data){
        for (String bannedWord:
                words) {
            //I don't like spaces in the program arguments so please write _ instead. I convert it to space here
            bannedWord = bannedWord.replace('_', ' ');
            int index;
            if((index = data.indexOf(bannedWord)) != -1){
                //what to do is the question. For now i'll just delete it
                String resultLine = data.substring(0, index);
                resultLine += data.substring(index + bannedWord.length());
                data = resultLine;
            }
        }
        return data;
    }
}