package com.shop.management.Controller.SellItems;

import com.shop.management.CustomDialog;
import com.shop.management.Main;
import com.shop.management.Method.CountCartQty;
import com.shop.management.Method.Method;
import com.shop.management.Model.CartModel;
import com.shop.management.PropertiesLoader;
import com.shop.management.util.DBConnection;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.ResourceBundle;

public class CartQuantityUpdate implements Initializable {

    public TextField quantityTf;
    public ComboBox<String> quantityUnit;
    public TextField sellingPriceTf;
    public Label availableQuantityL;
    public Label minSellPriceL;
    public Label mrpL , requiredQty;
    public Label avl_in_pcs;
    private Method method;
    private CartModel cartModel;
    private double minSellingPrice, productMrp;
    private String quantity_unit;
    private long availableQuantity;
    private int requiredQuantity;
    private long avlQty;
    private Properties propUpdate, propRead;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        method = new Method();
        PropertiesLoader propLoader = new PropertiesLoader();
        propUpdate = propLoader.getUpdateProp();
        propRead = propLoader.getReadProp();

        cartModel = (CartModel) Main.primaryStage.getUserData();
        if (null == cartModel) {
            return;
        }

        getStockSetting();
        getProductStock();
    }


    private void setDefaultValue() {

        quantityTf.setText(String.valueOf(cartModel.getQuantity()));
        BigDecimal sell_price = BigDecimal.valueOf(cartModel.getSellingPrice());
        sellingPriceTf.setText(sell_price.stripTrailingZeros().toPlainString());
        minSellPriceL.setText(minSellingPrice + " INR");
        mrpL.setText(productMrp + " INR");
        quantityUnit.getItems().add(cartModel.getQuantityUnit());
        quantityUnit.getSelectionModel().selectFirst();
        requiredQty.setText(requiredQuantity + " PCS");

        CountCartQty ccq = new CountCartQty();
        availableQuantity = availableQuantity-ccq.countQty(cartModel.getProductStockID(),
                cartModel.getQuantityUnit() , cartModel.getPcsPerPacket());

        String qtyUnit = quantity_unit;

        if (qtyUnit.equals("PKT")) {

            long p = availableQuantity * cartModel.getPcsPerPacket();
            avlQty = p - requiredQuantity;
            qtyUnit = "PCS";
        } else {
            avlQty = availableQuantity - requiredQuantity;
        }

        avl_in_pcs.setText(avlQty + " - PCS");

        String fullQuantity;

        if (qtyUnit.equals("PCS")) {

            long pkt = avlQty / cartModel.getPcsPerPacket();
            long pcs = avlQty % cartModel.getPcsPerPacket();

            fullQuantity = pkt + " - PKT , " + pcs + " - PCS";
        } else {
            fullQuantity = avlQty + " - " + qtyUnit;
        }
        availableQuantityL.setText(fullQuantity);
    }

    public void enterPress(KeyEvent e) {

        if (e.getCode() == KeyCode.ENTER) {
            bnUpdate(null);
        }
    }

    private void updateQuantity(long quantity, String quantity_Unit, double sellingPrice) {


        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = new DBConnection().getConnection();
            if (null == connection) {
                return;
            }

            ps = connection.prepareStatement(propUpdate.getProperty("UPDATE_CART_QUANTITY"));
            ps.setLong(1, quantity);
            ps.setString(2, quantity_Unit);
            ps.setDouble(3, sellingPrice);
            ps.setInt(4, cartModel.getCartId());

            int res = ps.executeUpdate();

            if (res > 0) {
                Stage stage = CustomDialog.stage2;

                if (stage.isShowing()) {
                    stage.close();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.closeConnection(connection, ps, rs);
        }

    }

    public void bnUpdate(ActionEvent event) {

        String quan = quantityTf.getText();
        String sellPrice = sellingPriceTf.getText();
        String unit = quantityUnit.getSelectionModel().getSelectedItem();

        long quantity;
        double sellingPrice;

        if (quan.isEmpty()) {
            method.show_popup("ENTER QUANTITY", quantityTf);
            return;
        }

        try {
            quantity = Long.parseLong(quan.replaceAll("[^0-9.]", ""));

        } catch (NumberFormatException e) {
            method.show_popup("ENTER VALID QUANTITY", quantityUnit);
            return;
        }

        if (quantity < 1) {
            method.show_popup("ENTER VALID QUANTITY", quantityUnit);
            return;

        } else if (sellPrice.isEmpty()) {

            method.show_popup("ENTER SELLING PRICE", sellingPriceTf);
            return;
        } else if (unit.isEmpty()) {
            method.show_popup("SELECT UNIT", quantityUnit);
            return;
        }

        try {
            sellingPrice = Double.parseDouble(sellPrice.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            method.show_popup("ENTER VALID PRICE", sellingPriceTf);
            return;
        }
        if (sellingPrice < 1) {
            method.show_popup("ENTER VALID PRICE", sellingPriceTf);
            return;

        }


        long qty;

        if (unit.equals("PKT")) {
            qty = quantity * cartModel.getPcsPerPacket();
        } else {
            qty = Long.parseLong(quan.replaceAll("[^0-9.]", ""));
        }

        if (qty <= avlQty) {
            if (sellingPrice >= cartModel.getMinSellPrice()) {

                if (sellingPrice <= cartModel.getProductMRP()) {

                    updateQuantity(quantity, unit, sellingPrice);

                } else {
                    method.show_popup("PLEASE ENTER LESS THEN " + cartModel.getProductMRP(), sellingPriceTf);
                }
            } else {
                method.show_popup("PLEASE ENTER MORE THAN " + cartModel.getMinSellPrice() + " RS.", sellingPriceTf);
            }
        } else {


            String msg;

            if (unit.equals("PKT")) {
                msg = availableQuantityL.getText();
            } else {
                msg = avl_in_pcs.getText();
            }

            method.show_popup("QUANTITY NOT AVAILABLE! Tot Avl : " + msg, quantityTf);
        }
    }

    private void getProductStock() {

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = new DBConnection().getConnection();

            if (null == connection) {
                return;
            }

            ps = connection.prepareStatement(propRead.getProperty("READ_PRODUCT_STOCK_IN_CART_UPDATE"));
            ps.setInt(1, cartModel.getProductStockID());

            rs = ps.executeQuery();

            if (rs.next()) {

                availableQuantity = rs.getLong("quantity");
                quantity_unit = rs.getString("quantity_unit");
                minSellingPrice = rs.getDouble("min_sellingprice");
                productMrp = rs.getDouble("product_mrp");

                setDefaultValue();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.closeConnection(connection, ps, rs);
        }


    }

    private void getStockSetting() {
        requiredQuantity = 0;

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            connection = new DBConnection().getConnection();
            if (null == connection) {
                return;
            }

            String query = propRead.getProperty("READ_STOCK_CONTROL");

            ps = connection.prepareStatement(query);
            rs = ps.executeQuery();

            if (rs.next()) {

                requiredQuantity = rs.getInt("REQUIRED");
            }


        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.closeConnection(connection, ps, rs);
        }
    }

    public void cancel(ActionEvent event) {

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        if (stage.isShowing()) {
            stage.close();
        }
    }
}
