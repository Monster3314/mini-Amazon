package edu.duke.ece568.amazon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class WebServer {
    public static final int PORT = 45678;
    private final ServerSocket serverSocket;

    public WebServer() throws IOException {
        System.out.println("Set up web ServerSocket.");
        this.serverSocket = new ServerSocket(PORT);
    }

    public long receivePackageId() throws IOException {
        Socket socket = serverSocket.accept();
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String strPackageId = reader.readLine();
        long packageId = Long.parseLong(strPackageId);
        System.out.println("Receive package id: " + packageId);
        socket.close();
        return packageId;
    }

}
