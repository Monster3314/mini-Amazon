package edu.duke.ece568.amazon;

import java.io.IOException;

public class WarehouseServer extends ProtobufServer{

    public static final int PORT = 23456;
    public static final String HOST = "vcm-26543.vm.duke.edu";

    public WarehouseServer(long worldId) throws IOException {
        super(HOST, PORT);
        WorldAmazon.AConnect.Builder reqBuilder = WorldAmazon.AConnect.newBuilder();
        WorldAmazon.AInitWarehouse.Builder whBuilder1 = WorldAmazon.AInitWarehouse.newBuilder();
        whBuilder1.setId(1);
        whBuilder1.setX(5);
        whBuilder1.setY(4);

        WorldAmazon.AInitWarehouse.Builder whBuilder2 = WorldAmazon.AInitWarehouse.newBuilder();
        whBuilder2.setId(2);
        whBuilder2.setX(8);
        whBuilder2.setY(2);
        reqBuilder.addInitwh(whBuilder1);
        reqBuilder.addInitwh(whBuilder2);
        reqBuilder.setWorldid(worldId);
        reqBuilder.setIsAmazon(true);
        send(reqBuilder.build());
        System.out.println("Connect Warehouse.");
        System.out.println(reqBuilder.build());

        WorldAmazon.AConnected.Builder resBuilder = WorldAmazon.AConnected.newBuilder();
        receive(resBuilder);
        WorldAmazon.AConnected aConnected = resBuilder.build();
    }

}
