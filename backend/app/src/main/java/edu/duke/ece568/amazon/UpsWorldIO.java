package edu.duke.ece568.amazon;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.GeneratedMessageV3;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;

public class UpsWorldIO implements Runnable {

    Random random = new Random();

    public Socket socket;
    public OutputStream outputStream;
    public InputStream inputStream;

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public UpsWorldIO(String ip, int port) throws IOException {
        System.out.println("Ups trying to connect world.");
        socket = new Socket(ip, port);
        //socket.setSoTimeout(50);
        outputStream = socket.getOutputStream();
        inputStream = socket.getInputStream();
        System.out.println("Mock Ups connected to World");
    }

    public void sendConnect() throws IOException {
        WorldUps.UInitTruck uInitTruck = WorldUps.UInitTruck.newBuilder()
                .setId(random.nextInt())
                .setX(1)
                .setY(2)
                .build();
        WorldUps.UConnect uConnect = WorldUps.UConnect.newBuilder()
                .setIsAmazon(false)
                .addTrucks(uInitTruck)
                .build();
        sendToWorld(uConnect.toByteArray());
        System.out.println("Mock ups send connect to world \n"+uConnect);
    }

    public long recvConnected() throws IOException {
        WorldUps.UConnected.Builder builder = WorldUps.UConnected.newBuilder();
        receiveFromWorld(builder);
        WorldUps.UConnected uConnected = builder.build();
        System.out.println("Mock ups receive connected from world:\n" + uConnected);
        return uConnected.getWorldid();
    }

    public void sendToWorld(byte[] data) throws IOException {
        CodedOutputStream cos = CodedOutputStream.newInstance(outputStream);
        cos.writeUInt32NoTag(data.length);
        cos.writeRawBytes(data);
        cos.flush();
    }

    public <T extends GeneratedMessageV3.Builder<?>> void receiveFromWorld(T response) throws IOException {
        CodedInputStream cis = CodedInputStream.newInstance(inputStream);
        int size = cis.readRawVarint32();
        int oldLimit = cis.pushLimit(size);
        response.mergeFrom(cis);
        cis.popLimit(oldLimit);
    }

    @Override
    public void run() {
        while (true) {
            try {
                recvConnected();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
