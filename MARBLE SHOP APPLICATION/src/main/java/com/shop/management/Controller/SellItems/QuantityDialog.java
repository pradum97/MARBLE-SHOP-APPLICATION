package com.shop.management.Controller.SellItems;

import com.shop.management.CustomDialog;
import com.shop.management.Main;
import com.shop.management.Method.CountCartQty;
import com.shop.management.Method.Method;
import com.shop.management.Method.StaticData;
import com.shop.management.Model.Quantity;
import com.shop.management.Model.Stock;
import com.shop.management.PropertiesLoader;
import com.shop.management.util.DBConnection;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.ResourceBundle;

public class QuantityDialog implements Initializable {
    final private String PACKET = "PKT";
    final private String PCS = "PCS";
    public Label size;
    public Label purchasePrice;
    public Label productMrp;
    public Label minSellingPrice;
    public Label quantity_L;
    public TextField quantityTf;
    public ComboBox<String> quantityUnit;
    public TextField sellingPriceTf;
    public HBox sellPriceContainer;
    public Button bnAddCart;
    public Label errorL, requiredQty, avl_in_pcs;
    private Stock stock;
    private Method method;
    private DBConnection dbConnection;
    private int stock_id;
    private int requiredQuantity;
    private int avlQty;
    private Properties propUpdate, propRead;
    private final static String UPDATE_QUANTITY = "UPDATE QUANTITY";
    private final static String ADD_CART = "➕ ADD TO CART";

    private boolean isPcsExists, isPktExists;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        stock = (Stock) Main.primaryStage.getUserData();
        PropertiesLoader propLoader = new PropertiesLoader();
        propUpdate = propLoader.getUpdateProp();
        propRead = propLoader.getReadProp();

        if (null == stock) {
            return;
        }

        method = new Method();
        dbConnection = new DBConnection();

        synchronized (this){
            checkStockExist();
            getStockSetting();
        }


    }

    private void checkStockExist() {

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = dbConnection.getConnection();
            if (null == connection) {
                return;
            }

            ps = connection.prepareStatement(propRead.getProperty("CHECK_STOCK_IS_EXIST_IN_QTY_DIALOG"));
            ps.setInt(1, stock.getStockID());

            rs = ps.executeQuery();
            int count = 0;

            while (rs.next()) {
                count = count + 1;

                stock_id = rs.getInt("stock_id");

                String cartQtyUnit = rs.getString("quantity_unit");

                if (cartQtyUnit.equals("PCS")) {
                    isPcsExists = true;
                    bnAddCart.setText(ADD_CART);
                    BigDecimal mrp = BigDecimal.valueOf(stock.getProductMRP());
                    sellingPriceTf.setText(mrp.stripTrailingZeros().toPlainString());

                    if (null != quantityUnit) {
                        quantityUnit.getItems().clear();
                    }

                    quantityUnit.getItems().add(PACKET);
                    quantityUnit.getSelectionModel().selectFirst();
                }

                if (cartQtyUnit.equals("PKT")) {
                    isPktExists = true;
                    bnAddCart.setText(ADD_CART);
                    BigDecimal mrp = BigDecimal.valueOf(stock.getProductMRP());
                    sellingPriceTf.setText(mrp.stripTrailingZeros().toPlainString());

                    if (null != quantityUnit) {
                        quantityUnit.getItems().clear();
                    }

                    quantityUnit.getItems().add(PCS);
                    quantityUnit.getSelectionModel().selectFirst();
                }
            }

            if (isPktExists && isPcsExists) {
                if (null != quantityUnit) {
                    quantityUnit.getItems().clear();
                }
                quantityUnit.setItems(new StaticData().getSizeQuantityUnit());
                quantityUnit.getSelectionModel().selectFirst();
                getExitsItem(stock_id, quantityUnit.getSelectionModel().getSelectedItem());
            }
            quantityUnit.valueProperty().addListener((observableValue, s, t1) -> {

                if (isPktExists && isPcsExists) {
                    getExitsItem(stock_id, t1);
                }

                CountCartQty ccq = new CountCartQty();

                setDefaultValue( ccq.countQty(stock.getStockID(),
                        t1));


            });

            if (count < 1) {
                addCartBnVisible();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.closeConnection(connection, ps, rs);
        }
    }

    private void getExitsItem(int stockId, String currentQtyUnit) {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            connection = dbConnection.getConnection();
            String query = "select * from tbl_cart where stock_id = ? and  quantity_unit = ?";

            ps = connection.prepareStatement(query);
            ps.setInt(1, stockId);
            ps.setString(2, currentQtyUnit);


            rs = ps.executeQuery();
            if (rs.next()) {

                errorL.setVisible(true);
                errorL.setText("( This item is already in the cart )");
                quantityTf.setText(String.valueOf(rs.getLong("quantity")));
                sellingPriceTf.setText(String.valueOf(rs.getDouble("sellprice")));
                double sellingPrice = rs.getDouble("sellprice");
                BigDecimal sell_price = BigDecimal.valueOf(sellingPrice);
                sellingPriceTf.setText(sell_price.stripTrailingZeros().toPlainString());

                bnAddCart.setText(UPDATE_QUANTITY);

            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            DBConnection.closeConnection(connection, ps, rs);
        }
    }

    private void addCartBnVisible() {

        errorL.setText("");
        errorL.setVisible(false);
        errorL.managedProperty().bind(errorL.visibleProperty());
        BigDecimal mrp = BigDecimal.valueOf(stock.getProductMRP());
        sellingPriceTf.setText(mrp.stripTrailingZeros().toPlainString());
        quantityUnit.setItems(new StaticData().getSizeQuantityUnit());

        if (avlQty < 6) {
            quantityUnit.getSelectionModel().select(0);
        } else {
            quantityUnit.getSelectionModel().select(1);
        }

        bnAddCart.setText(ADD_CART);
    }

    private void setDefaultValue(int qty) {


        String inr = " INR";
        size.setText(stock.getFullSize());
        purchasePrice.setText(stock.getPurchasePrice() + inr);
        productMrp.setText(stock.getProductMRP() + inr);
        minSellingPrice.setText(stock.getMinSellingPrice() + inr);
        requiredQty.setText(requiredQuantity + " PCS");

        String qtyUnit = stock.getQuantityUnit();

        if (qtyUnit.equals("PKT")) {

            int p = stock.getQuantity() * Method.PER_PACKET_PCS;
            avlQty = p - requiredQuantity;
            qtyUnit = "PCS";
        } else {
            avlQty = stock.getQuantity() - requiredQuantity;
        }

        avlQty = avlQty - qty;

        avl_in_pcs.setText(avlQty + " - PCS");

        String fullQuantity;

        if (qtyUnit.equals("PCS")) {

            int pkt = avlQty / Method.PER_PACKET_PCS;
            int pcs = avlQty % Method.PER_PACKET_PCS;

            fullQuantity = pkt + " - PKT , " + pcs + " - PCS";
        } else {
            fullQuantity = avlQty + " - " + qtyUnit;
        }


        quantity_L.setText(fullQuantity);
    }

    public void bnAddToCart(ActionEvent event) {

        String quan = quantityTf.getText();
        String sellPrice = sellingPriceTf.getText();
        String quantity_Unit = quantityUnit.getSelectionModel().getSelectedItem();

        long quantity = 0;
        double sellingPrice = 0;

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
            method.show_popup("ENTER QUANTITY MORE THEN 0", quantityUnit);
            return;

        } else if (sellPrice.isEmpty()) {

            method.show_popup("ENTER SELLING PRICE", sellingPriceTf);
            return;
        } else if (quantity_Unit.isEmpty()) {
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

        if (quantity_Unit.equals("PKT")) {
            qty = quantity * Method.PER_PACKET_PCS;
        } else {
            qty = Long.parseLong(quan.replaceAll("[^0-9.]", ""));
        }

        if (qty <= avlQty) {

            if (sellingPrice >= stock.getMinSellingPrice()) {

                if (sellingPrice <= stock.getProductMRP()) {

                    switch (bnAddCart.getText()) {
                        case UPDATE_QUANTITY -> {
                            updateQuantity(quantity, quantity_Unit, sellingPrice, event);
                        }
                        case ADD_CART -> {

                            addToCart(quantity, quantity_Unit, sellingPrice, event);
                        }
                    }
                } else {
                    method.show_popup("PLEASE ENTER LESS THEN " + stock.getProductMRP() + " OR " + stock.getProductMRP() + " RS.", sellingPriceTf);
                }
            } else {
                method.show_popup("PLEASE ENTER MORE THAN " + stock.getMinSellingPrice() + " RS.", sellingPriceTf);
            }

        } else {
            String msg;

            if (quantity_Unit.equals("PKT")) {
                msg = quantity_L.getText();
            } else {
                msg = avl_in_pcs.getText();
            }

            method.show_popup("QUANTITY NOT AVAILABLE! Tot Avl : " + msg, quantityTf);
        }
    }

    private void getStockSetting() {
        requiredQuantity = 0;

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = dbConnection.getConnection();
            if (null == connection) {
                return;
            }
            ps = connection.prepareStatement(propRead.getProperty("READ_STOCK_CONTROL"));
            rs = ps.executeQuery();
            if (rs.next()) {
                requiredQuantity = rs.getInt("REQUIRED");

                setDefaultValue( new CountCartQty().countQty(stock.getStockID(),
                        quantityUnit.getSelectionModel().getSelectedItem()));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.closeConnection(connection, ps, rs);
        }
    }

    private void updateQuantity(long quantity, String quantity_Unit, double sellingPrice, ActionEvent event) {

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = dbConnection.getConnection();
            if (null == connection) {
                return;
            }

            ps = connection.prepareStatement(propUpdate.getProperty("UPDATE_CART_IN_QTY_DIALOG"));
            ps.setLong(1, quantity);
            ps.setString(2, quantity_Unit);
            ps.setDouble(3, sellingPrice);
            ps.setInt(4, stock_id);
            ps.setString(5, quantity_Unit);
            int res = ps.executeUpdate();

            if (res > 0) {

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

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

    private void addToCart(long quantity, String quantity_Unit, double sellingPrice, ActionEvent event) {

        Quantity quantity1 = new Quantity(quantity, quantity_Unit, sellingPrice);

        Main.primaryStage.setUserData(quantity1);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        if (stage.isShowing()) {
            stage.close();
        }

    }

    public void enterPress(KeyEvent e) {

        if (e.getCode() == KeyCode.ENTER) {
            //do something
            bnAddToCart(null);
        }
    }

    public void cancel(ActionEvent event) {

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        if (stage.isShowing()) {
            stage.close();
        }
    }
}
