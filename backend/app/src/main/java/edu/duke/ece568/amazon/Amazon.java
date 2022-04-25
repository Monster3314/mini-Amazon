package edu.duke.ece568.amazon;

import com.google.protobuf.Message;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class Amazon {
    private final WebServer webServer;
    private final UPSServer upsServer;
    private final WarehouseServer warehouseServer;
    private final SQLExecutor sqlExecutor;

    static public final int TIMEOUT = 10000;
    protected long seqNum;
    private final Map<Long, Timer> warehouseCommands;
    private final Map<Long, Timer> upsCommands;
    private final Set<Long> warehouseSeq;
    private final Set<Long> upsSeq;


    public Amazon() throws IOException, SQLException, ClassNotFoundException {
        this.seqNum = 0;
        this.upsServer = new UPSServer();
//        this.upsServer = null;
//        UpsWorldIO upsWorldIO = new UpsWorldIO("vcm-26543.vm.duke.edu", 12345);
//        upsWorldIO.sendConnect();
//        long worldId = upsWorldIO.recvConnected();
        long worldId = upsServer.getWorldId();
        this.warehouseServer = new WarehouseServer(worldId);
        this.webServer = new WebServer();
        this.warehouseCommands = new ConcurrentHashMap<>();
        this.upsCommands = new ConcurrentHashMap<>();
        this.warehouseSeq = new HashSet<>();
        this.upsSeq = new HashSet<>();
        this.sqlExecutor = new SQLExecutor();
    }

    public void runTest() {
        System.out.println("Run test.");
        Thread warehouseThread = new Thread(() -> {
            initWhStock(1);
            initWhStock(2);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    handleWarehouseResponses(listenFromWarehouse());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread upsThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    handleUPSCommands(listenFromUPS());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        warehouseThread.start();
//        upsThread.start();


        // mock AUCommands to UPS
//        AmazonUps.AUOrderTruck.Builder builder = AmazonUps.AUOrderTruck.newBuilder();
//        long seqNum = getSeqNum();
//        builder.setSeqnum(seqNum);
//        AmazonUps.Package.Builder packageBuilder = AmazonUps.Package.newBuilder();
//        packageBuilder.setPackageid(0);
//        packageBuilder.setProductDestinationAddressX(0);
//        packageBuilder.setProductDestinationAddressY(0);
//        packageBuilder.setItems("items");
//        builder.addPackage(packageBuilder);
//        builder.setWhid(0);
//
//        AmazonUps.AUCommands.Builder command = AmazonUps.AUCommands.newBuilder();
//        command.addAskTruck(builder);
//
//        AmazonUps.AUDeliver.Builder deliver = AmazonUps.AUDeliver.newBuilder();
//        deliver.setTruckid(0);
//        deliver.addPackageid(0);
//        deliver.setSeqnum(getSeqNum());
//
//        command.addDeliver(deliver);
//
//        sendCommandsToUPS(seqNum, command);
        Thread webThread = new Thread(() -> {
            System.out.println("Start to listen from web.");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    pack(webServer.receivePackageId());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        webThread.start();

    }

    public void run() {
        Thread warehouseThread = new Thread(() -> {
            initWhStock(1);
            initWhStock(2);
            System.out.println("Start to listen from warehouse.");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    handleWarehouseResponses(listenFromWarehouse());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread upsThread = new Thread(() -> {
            System.out.println("Start to listen from ups.");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    handleUPSCommands(listenFromUPS());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread webThread = new Thread(() -> {
            System.out.println("Start to listen from web.");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    pack(webServer.receivePackageId());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        warehouseThread.start();
        upsThread.start();
        webThread.start();
    }

    protected synchronized long getSeqNum() {
        return seqNum++;
    }

    private void sendCommandsToWarehouse(long seqNum, WorldAmazon.ACommands.Builder builder) {
        Message message = builder.build();
        sendCommands(seqNum, message, warehouseServer, warehouseCommands);
    }

    private void sendCommandsToUPS(long seqNum, AmazonUps.AUCommands.Builder builder) {
        Message message = builder.build();
        sendCommands(seqNum, message, upsServer, upsCommands);
    }

    private void sendCommands(long seqNum, Message message, ProtobufServer server, Map<Long, Timer> map) {
        if (seqNum < 0) {
            try {
                server.send(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        Timer resendTimer = new Timer();
        resendTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    server.send(message);
                    System.out.println("Send message.");
                    System.out.println(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0, TIMEOUT);
        map.put(seqNum, resendTimer);
        System.out.println(map);
    }

    private void initWhStock(int warehouseId) {
        try {
            ResultSet products = sqlExecutor.getAllProducts(warehouseId);
            ArrayList<WorldAmazon.AProduct> productList = new ArrayList<>();
            while (products.next()) {
                WorldAmazon.AProduct.Builder builder = WorldAmazon.AProduct.newBuilder();
                builder.setId(products.getLong("id")).setCount(products.getInt("inventory")).setDescription(products.getString("description"));
                productList.add(builder.build());
            }
            WorldAmazon.APurchaseMore.Builder purchaseBuilder = WorldAmazon.APurchaseMore.newBuilder();
            long seqNum = getSeqNum();
            purchaseBuilder.setWhnum(warehouseId).setSeqnum(seqNum);
            purchaseBuilder.addAllThings(productList);

            WorldAmazon.ACommands.Builder command = WorldAmazon.ACommands.newBuilder();
            command.addBuy(purchaseBuilder);
            sendCommandsToWarehouse(seqNum, command);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private WorldAmazon.AResponses listenFromWarehouse() throws IOException {
        WorldAmazon.AResponses.Builder builder = WorldAmazon.AResponses.newBuilder();
        while (warehouseServer.getSocket().getInputStream().available() == 0) {}
        warehouseServer.receive(builder);
        return builder.build();
    }

    private AmazonUps.UACommands listenFromUPS() throws IOException {
        AmazonUps.UACommands.Builder builder = AmazonUps.UACommands.newBuilder();
        while (upsServer.getSocket().getInputStream().available() == 0) {}
        upsServer.receive(builder);
        return builder.build();
    }

    private void handleWarehouseResponses(WorldAmazon.AResponses responses) {
        System.out.println("Handle response from warehouse.");
        System.out.println(responses);
        ArrayList<Long> seqNums = new ArrayList<>();

        for (WorldAmazon.APurchaseMore purchaseMore : responses.getArrivedList()) {
            long seqNum = purchaseMore.getSeqnum();
            seqNums.add(seqNum);
            if (!warehouseSeq.contains(seqNum)) {
                warehouseSeq.add(seqNum);
                int warehouseId = purchaseMore.getWhnum();
                for (WorldAmazon.AProduct product : purchaseMore.getThingsList()) {
                    try {
                        sqlExecutor.refillStock(warehouseId, product.getId(), product.getCount());
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                warehouseSeq.add(seqNum);
            }
        }
        for (WorldAmazon.APacked packed : responses.getReadyList()) {
            long seqNum = packed.getSeqnum();
            seqNums.add(seqNum);
            if (!warehouseSeq.contains(seqNum)) {
                warehouseSeq.add(seqNum);
                try {
                    sqlExecutor.updateStatus(packed.getShipid(), "packed");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                orderTruck(packed.getShipid());
            }
        }
        for (WorldAmazon.ALoaded loaded : responses.getLoadedList()) {
            long seqNum = loaded.getSeqnum();
            seqNums.add(seqNum);
            if (!warehouseSeq.contains(seqNum)) {
                warehouseSeq.add(seqNum);
                try {
                    sqlExecutor.updateStatus(loaded.getShipid(), "loaded");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                deliver(loaded.getShipid());
            }
        }
        for (WorldAmazon.AErr err : responses.getErrorList()) {
            // TODO: handle original seqNum
            System.err.println(err.getErr());
            seqNums.add(err.getSeqnum());
        }
        for (WorldAmazon.APackage aPackage : responses.getPackagestatusList()) {
            seqNums.add(aPackage.getSeqnum());
        }

        for (long ack : responses.getAcksList()) {
            Timer timer = warehouseCommands.remove(ack);
            if (timer != null) {
                timer.cancel();
            }
        }

        // Send back ACKs
        if (!seqNums.isEmpty()) {
            WorldAmazon.ACommands.Builder builder = WorldAmazon.ACommands.newBuilder();
            builder.addAllAcks(seqNums);
            sendCommandsToWarehouse(-1, builder);
        }
    }

    private void handleUPSCommands(AmazonUps.UACommands commands) {
        System.out.println("Handle command from UPS.");
        System.out.println(commands);
        ArrayList<Long> seqNums = new ArrayList<>();
        for (AmazonUps.UADelivered delivered : commands.getDeliveredList()) {
            long seqNum = delivered.getSeqnum();
            seqNums.add(seqNum);
            if (!upsSeq.contains(seqNum)) {
                upsSeq.add(seqNum);
                try {
                    for (long packageId : delivered.getPackageidList()) {
                        sqlExecutor.updateStatus(packageId, "delivered");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        for (AmazonUps.UATruckArrived truckArrived : commands.getTruckarrivedList()) {
            long seqNum = truckArrived.getSeqnum();
            seqNums.add(seqNum);
            if (!upsSeq.contains(seqNum)) {
                upsSeq.add(seqNum);
                putOnTruck(truckArrived.getTruckid(), truckArrived.getPackageidList());
            }
        }

        for (long ack : commands.getAcksList()) {
            Timer timer = upsCommands.remove(ack);
            if (timer != null) {
                timer.cancel();
            }
        }

        // Send back ACKs
        if (!seqNums.isEmpty()) {
            AmazonUps.AUCommands.Builder builder = AmazonUps.AUCommands.newBuilder();
            builder.addAllAcks(seqNums);
            sendCommandsToUPS(-1, builder);
        }
    }

    private void purchaseMore(List<Integer> products, int warehouseId) {
        try {
            ArrayList<WorldAmazon.AProduct> productList = new ArrayList<>();

            for (int productId : products) {
                ResultSet result = sqlExecutor.getProduct(productId);
                result.next();
                int inventory = result.getInt("inventory");
                if (inventory < 10) {
                    WorldAmazon.AProduct.Builder builder = WorldAmazon.AProduct.newBuilder();
                    builder.setId(productId);
                    builder.setDescription(result.getString("description"));
                    builder.setCount(100);
                    productList.add(builder.build());
                }
            }

            if (!productList.isEmpty()) {
                WorldAmazon.APurchaseMore.Builder builder = WorldAmazon.APurchaseMore.newBuilder();
                builder.addAllThings(productList);
                builder.setWhnum(warehouseId);
                long seqNum = getSeqNum();
                builder.setSeqnum(seqNum);

                WorldAmazon.ACommands.Builder command = WorldAmazon.ACommands.newBuilder();
                command.addBuy(builder);
                sendCommandsToWarehouse(seqNum, command);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void putOnTruck(int truckId, Iterable<Long> packageIds) {
        try {
            long seqNum = -1;
            WorldAmazon.ACommands.Builder command = WorldAmazon.ACommands.newBuilder();
            for (long packageId : packageIds) {
                WorldAmazon.APutOnTruck.Builder builder = WorldAmazon.APutOnTruck.newBuilder();
                ResultSet result = sqlExecutor.getPackage(packageId);
                result.next();
                builder.setWhnum(result.getInt("warehouse_id"));
                builder.setTruckid(truckId);
                builder.setShipid(packageId);
                seqNum = getSeqNum();
                builder.setSeqnum(seqNum);
                command.addLoad(builder);

                sqlExecutor.updateStatus(packageId, "loading");
            }
            sendCommandsToWarehouse(seqNum, command);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void pack(long packageId) {
        try {
            System.out.println("Handle request from web interface.");
            WorldAmazon.APack.Builder builder = WorldAmazon.APack.newBuilder();

            ResultSet packageResult = sqlExecutor.getPackage(packageId);
            ResultSet ordersResult = sqlExecutor.getOrders(packageId);

            ArrayList<Integer> products = new ArrayList<>();
            while (ordersResult.next()) {
                WorldAmazon.AProduct.Builder productBuilder = WorldAmazon.AProduct.newBuilder();
                int productId = ordersResult.getInt("id");
                products.add(productId);
                productBuilder.setId(productId);
                productBuilder.setDescription(ordersResult.getString("description"));
                productBuilder.setCount(ordersResult.getInt("item_num"));
                builder.addThings(productBuilder);
            }
            packageResult.next();
            int warehouseId = packageResult.getInt("warehouse_id");
            builder.setWhnum(warehouseId);
            builder.setShipid(packageId);
            long seqNum = getSeqNum();
            builder.setSeqnum(seqNum);

            WorldAmazon.ACommands.Builder command = WorldAmazon.ACommands.newBuilder();
            command.addTopack(builder);
            sendCommandsToWarehouse(seqNum, command);

            sqlExecutor.updateStatus(packageId, "packing");

            purchaseMore(products, warehouseId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void orderTruck(long packageId) {
        try {
            ResultSet result = sqlExecutor.getPackage(packageId);
            result.next();
            ResultSet orders = sqlExecutor.getOrders(packageId);
            AmazonUps.AUOrderTruck.Builder builder = AmazonUps.AUOrderTruck.newBuilder();
            builder.setWhid(result.getInt("warehouse_id"));
            AmazonUps.Package.Builder packageBuilder = AmazonUps.Package.newBuilder();
            packageBuilder.setPackageid(packageId);
            packageBuilder.setProductDestinationAddressX(result.getInt("deliver_add_x"));
            packageBuilder.setProductDestinationAddressY(result.getInt("deliver_add_y"));

            ArrayList<String> items = new ArrayList<>();
            while (orders.next()) {
                items.add(orders.getString("description"));
            }
            String itemsString = String.join(",", items);
            packageBuilder.setItems(itemsString);
            packageBuilder.setUPSAccountid(result.getLong("ups_id"));

            builder.addPackage(packageBuilder);
            long seqNum = getSeqNum();
            builder.setSeqnum(seqNum);

            AmazonUps.AUCommands.Builder command = AmazonUps.AUCommands.newBuilder();
            command.addAskTruck(builder);
            sendCommandsToUPS(seqNum, command);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deliver(long packageId) {
        try {
            AmazonUps.AUDeliver.Builder builder = AmazonUps.AUDeliver.newBuilder();
            builder.setTruckid(sqlExecutor.getTruckIdOfPackage(packageId));
            builder.addPackageid(packageId);
            long seqNum = getSeqNum();

            AmazonUps.AUCommands.Builder command = AmazonUps.AUCommands.newBuilder();
            command.addDeliver(builder);
            sendCommandsToUPS(seqNum, command);

            sqlExecutor.updateStatus(packageId, "delivering");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
