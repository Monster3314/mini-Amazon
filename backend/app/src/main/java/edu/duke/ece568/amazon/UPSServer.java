package edu.duke.ece568.amazon;

import java.io.IOException;

public class UPSServer extends ProtobufServer{

    public static final int PORT = 6666;
    public static final String HOST = "vcm-24203.vm.duke.edu";

    private long worldId;

    public UPSServer() throws IOException {
        super(HOST, PORT);
        AmazonUps.AUConnect.Builder auBuilder = AmazonUps.AUConnect.newBuilder();
        send(auBuilder.build());
        System.out.println("Send AUConnect:" + auBuilder.build());
        AmazonUps.UAConnected.Builder uaBuilder = AmazonUps.UAConnected.newBuilder();
        receive(uaBuilder);
        AmazonUps.UAConnected uaConnected = uaBuilder.build();
        this.worldId = uaConnected.getWorldid();
        System.out.println("Receive world Id: " + worldId);
    }

    public long getWorldId() {
        return worldId;
    }

}
