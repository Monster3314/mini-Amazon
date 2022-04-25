package edu.duke.ece568.amazon;

import com.google.protobuf.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class ProtobufServer {

    protected final Socket socket;
    protected final CodedInputStream inputStream;
    protected final CodedOutputStream outputStream;


    public ProtobufServer(Socket socket) throws IOException {
        this.socket = socket;
        this.inputStream = CodedInputStream.newInstance(this.socket.getInputStream());
        this.outputStream = CodedOutputStream.newInstance(this.socket.getOutputStream());
    }

    public ProtobufServer(String host, int port) throws IOException {
        this(new Socket(host, port));
    }

    public synchronized void send(Message message) throws IOException {
        byte[] data = message.toByteArray();
        outputStream.writeUInt32NoTag(data.length);
        outputStream.writeRawBytes(data);
        outputStream.flush();
    }

    public synchronized void receive(GeneratedMessageV3.Builder<?> builder) throws IOException {
        int length = inputStream.readRawVarint32();
        int parseLimit = inputStream.pushLimit(length);
        builder.mergeFrom(inputStream);
        inputStream.popLimit(parseLimit);
    }

    public Socket getSocket() {
        return socket;
    }
}
