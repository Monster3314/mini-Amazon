package edu.duke.ece568.amazon;

import java.sql.*;

public class SQLExecutor {
    private static final String PACKAGE_TABLE = "\"mysite_package\"";
    private static final String ORDER_TABLE = "\"mysite_order\"";
    private static final String PRODUCT_TABLE = "\"mysite_product\"";

    private final Connection c;

    public SQLExecutor() throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        this.c = DriverManager.getConnection("jdbc:postgresql://db:5432/amazon", "postgres", "postgres");
        c.setAutoCommit(false);
    }

    public ResultSet getPackage(long packageID) throws SQLException {
        Statement work = c.createStatement();
        String sql = String.format("select * from %s where id = %d", PACKAGE_TABLE, packageID);
        return work.executeQuery(sql);
    }

    public ResultSet getOrders(long packageId) throws SQLException {
        Statement work = c.createStatement();
        String sql = String.format("select products.description, orders.item_num, products.id" +
                " from %s as products, %s as orders " +
                "where orders.package_id = %d and orders.item_id = products.id", PRODUCT_TABLE, ORDER_TABLE, packageId);
        return work.executeQuery(sql);

    }


    /**
     * @param packageID
     * @param status: packing, packed, loading, loaded, delivering, delivered.
     * Send APack, Receive APacked, Send APutOnTruck, Receive ALoaded, Send AUDeliver, Receive UADelivered
     * @throws SQLException
     */
    public void updateStatus(long packageID, String status) throws SQLException {
        Statement work = c.createStatement();
        String sql = String.format("update %s set status = '%s' where id = %d", PACKAGE_TABLE, status, packageID);
        work.executeUpdate(sql);
        c.commit();
        work.close();
    }

    public ResultSet getProduct(int productId) throws SQLException {
        Statement work = c.createStatement();
        String sql = String.format("select inventory, description from %s where id = %d", PRODUCT_TABLE, productId);
        return work.executeQuery(sql);
    }

    public long getTruckIdOfPackage(long packageId) throws SQLException{
        Statement work = c.createStatement();
        String sql = String.format("select truck_id from %s where id = %d", PACKAGE_TABLE, packageId);
        ResultSet result = work.executeQuery(sql);
        result.next();
        return result.getLong("truck_id");
    }

    public void refillStock(int warehouseId, long productId, int count) throws SQLException {
        Statement work = c.createStatement();
        String sql = String.format("update %s set inventory = inventory+%d where id=%d AND warehouse_id=%d", PRODUCT_TABLE, count, productId, warehouseId);
    }

    public ResultSet getAllProducts(int warehouseId) throws SQLException {
        Statement work = c.createStatement();
        String sql = String.format("select * from %s where warehouse_id=%d", PRODUCT_TABLE, warehouseId);
        return work.executeQuery(sql);
    }

}
