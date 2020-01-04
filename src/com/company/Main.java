package com.company;

import java.io.IOException;
import java.net.ServerSocket;

public class Main {

    public static void main(String[] args) {

        if (args.length < 3){
            throw new IllegalArgumentException("insuficient arguments");
        }
        int port = -1;
        String[] blacklistedWords = null;
        String cachePath = null;
        boolean useCache = true;
        for (int i = 0; i < args.length; i++) {

            String[] param = args[i].split("=");

            switch (param[0]){
                case "PROXY_PORT":
                    port = Integer.parseInt(param[1]);
                    break;
                case "WORDS":
                    blacklistedWords = param[1].split(";");
                    break;
                case "CACHE_DIR":
                    cachePath = param[1];
                    break;
                case "USE_CACHE":
                    useCache = Boolean.parseBoolean(param[1]);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid parameter given");
            }
            //the argument were correctly assigned


        }

        try {
            System.out.println("Starting the server at port: " + port);
            new ProxyServer(port, blacklistedWords, cachePath, useCache);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
