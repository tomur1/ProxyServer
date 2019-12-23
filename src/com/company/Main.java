package com.company;

import java.io.IOException;
import java.net.ServerSocket;

public class Main {

    public static void main(String[] args) {

        if (args.length != 3){
            throw new IllegalArgumentException("insuficient arguments");
        }
        int port;
        String[] blacklistedWords;
        String cachePath;
        for (int i = 0; i < args.length; i++) {

            String[] param = args[i].split("=");

            switch (param[0]){
                case "PROXY_PORT":
                    port = Integer.valueOf(param[1]);
                    break;
                case "WORDS":
                    blacklistedWords = param[1].split(";");
                    break;
                case "CACHE_DIR":
                    cachePath = param[1];
                    break;
                default:
                    throw new IllegalArgumentException("Invalid parameter given");
            }


        }

//        try {
//            ServerSocket server = new ServerSocket(localport);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        while (true) {
//
//            }
    }
}
