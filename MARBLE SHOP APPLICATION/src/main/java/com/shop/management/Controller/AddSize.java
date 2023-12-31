package com.shop.management.Controller;

import com.shop.management.CustomDialog;
import com.shop.management.Main;
import com.shop.management.Method.Method;
import com.shop.management.Method.StaticData;
import com.shop.management.Model.Products;
import com.shop.management.PropertiesLoader;
import com.shop.management.util.DBConnection;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AddSize implements Initializable {

    public TextField purchasePrice;
    public TextField productMrp;
    public TextField minSellPrice;
    public TextField productHeight;
    public TextField productWidth;
    public ComboBox<String> productSizeUnit;
    public TextField productQuantity;
    public ComboBox<String> productQuantityUnit;
    private static final String REGEX = "[^0-9.]";
    public Button bnAddSize;
    public ComboBox<String> priceTypeC;
    public ComboBox<Integer> pcsPerPacket;
    private Products products;
    private Method method;
    private CustomDialog customDialog;
    private DBConnection dbConnection;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        products = (Products) Main.primaryStage.getUserData();

        if (null == products) {
            return;
        }
        method = new Method();
        customDialog = new CustomDialog();
        dbConnection = new DBConnection();
        productSizeUnit.setItems(method.getSizeUnit());
        productQuantityUnit.setItems(method.getSizeQuantityUnit());

        StaticData sd = new StaticData();
        priceTypeC.setItems(sd.getSizeQuantityUnit());
        pcsPerPacket.setItems(sd.getPcsPerPacketList());
        pcsPerPacket.getSelectionModel().selectFirst();

        purchasePrice.textProperty().addListener((observableValue, old, newValue) -> {

            double purchasePrice_d = 0, minPrice = 0;

            try {
                purchasePrice_d = Double.parseDouble(purchasePrice.getText());
            } catch (NumberFormatException e) {
                method.show_popup("Enter valid Purchase Price", purchasePrice);
                return;
            }

            minPrice = purchasePrice_d + (AddProducts.profitPercentage * purchasePrice_d / 100);

            minSellPrice.setText(String.valueOf(minPrice));

        });
    }

    public void enterPress(KeyEvent key) {

        if (key.getCode() == KeyCode.ENTER){
            finalAddSize(key.getSource());
        }

    }

    private void finalAddSize(Object source) {

        String heightS = productHeight.getText();
        String widthS = productWidth.getText();
        String quantityS = productQuantity.getText();
        String purchasePrice_s = purchasePrice.getText();
        String prodMrp = productMrp.getText();
        String minSellPrice_s = minSellPrice.getText();
        double mrp = 0, min_Sell_Price = 0, purchase_price = 0;

        if (purchasePrice_s.isEmpty()) {
            method.show_popup("ENTER PURCHASE PRICE ", purchasePrice);
            return;
        }
        try {
            purchase_price = Double.parseDouble(purchasePrice_s.replaceAll(REGEX, ""));
        } catch (NumberFormatException e) {
            customDialog.showAlertBox("INVALID PURCHASE PRICE", "ENTER VALID PURCHASE PRICE");
            e.printStackTrace();
            return;
        }

        if (prodMrp.isEmpty()) {
            method.show_popup("ENTER PRODUCT MRP ", productMrp);
            return;
        }
        try {
            mrp = Double.parseDouble(prodMrp.replaceAll(REGEX, ""));
        } catch (NumberFormatException e) {
            customDialog.showAlertBox("INVALID MRP", "ENTER VALID MRP");
            e.printStackTrace();
            return;
        }
        if (purchase_price > mrp) {
            method.show_popup("ENTER MRP MORE THAN PURCHASE PRICE", productMrp);
            return;
        }
        if (minSellPrice_s.isEmpty()) {
            method.show_popup("ENTER MIN SELLING PRICE ", minSellPrice);
            return;
        }
        try {
            min_Sell_Price = Double.parseDouble(minSellPrice_s.replaceAll(REGEX, ""));

        } catch (NumberFormatException e) {
            customDialog.showAlertBox("INVALID MIN SELL PRICE", "ENTER VALID INVALID MIN SELL PRICE");
            e.printStackTrace();
            return;
        }
        if (purchase_price > min_Sell_Price) {
            method.show_popup("ENTER MINIMUM SELLING PRICE MORE THAN PURCHASE PRICE", minSellPrice);
            return;
        } else if (min_Sell_Price > mrp) {
            method.show_popup("ENTER MINIMUM SELLING PRICE LESS THAN MRP", minSellPrice);
            return;

        }else  if (priceTypeC.getSelectionModel().isEmpty()){
            method.show_popup("PLEASE SELECT PRICE TYPE", priceTypeC);
            return;
        } else if (pcsPerPacket.getSelectionModel().isEmpty()){
            method.show_popup("PLEASE SELECT PCS PER PACKET", pcsPerPacket);
            return;
        }
        if (heightS.isEmpty()) {
            method.show_popup("ENTER PRODUCT HEIGHT", productHeight);
            return;
        } else if (widthS.isEmpty()) {
            method.show_popup("ENTER PRODUCT WIDTH", productWidth);
            return;
        } else if (null == productSizeUnit.getValue()) {
            method.show_popup("CHOOSE SIZE UNIT", productSizeUnit);
            return;

        } else if (quantityS.isEmpty()) {

            method.show_popup("ENTER PRODUCT QUANTITY", productQuantity);
            return;
        } else if (null == productQuantityUnit.getValue()) {

            method.show_popup("CHOOSE QUANTITY UNIT", productQuantityUnit);
            return;
        }

        int height = 0;
        int width = 0;
        long quantity = 0;


        try {
            height = Integer.parseInt(heightS.replaceAll(REGEX, ""));
            width = Integer.parseInt(widthS.replaceAll(REGEX, ""));

        } catch (NumberFormatException e) {
            customDialog.showAlertBox("INVALID PRODUCT SIZE", "ENTER VALID HEIGHT AND WIDTH ");
            return;
        }
        try {
            quantity = Long.parseLong(quantityS.replaceAll(REGEX, ""));
        } catch (NumberFormatException e) {
            customDialog.showAlertBox("INVALID QUANTITY", "ENTER VALID QUANTITY");
            e.printStackTrace();
        }
        String sizeUnit = productSizeUnit.getValue();
        String quantityUnit = productQuantityUnit.getValue();

        String priceTypeS = priceTypeC.getSelectionModel().getSelectedItem();
        int pcsPerPkt = pcsPerPacket.getSelectionModel().getSelectedItem();

        Connection connection = null;
        PreparedStatement ps = null;

        try {
            connection = dbConnection.getConnection();

            if (null == connection) {
                customDialog.showAlertBox("Failed", "Connection Failed");
                return;
            }
            long qty ;

            if (quantityUnit.equals("PKT")){
                qty = (quantity*pcsPerPkt);
            }else {
                qty =quantity;
            }

            ps = connection.prepareStatement(new PropertiesLoader().getInsertProp().getProperty("ADD_SIZE"));
            ps.setDouble(1, purchase_price);
            ps.setDouble(2, mrp);
            ps.setDouble(3, min_Sell_Price);
            ps.setInt(4, height);
            ps.setInt(5, width);
            ps.setString(6, sizeUnit);
            ps.setLong(7, qty);
            ps.setString(8, "PCS");
            ps.setInt(9, products.getProductID());

            ps.setString(10, priceTypeS);
            ps.setInt(11, pcsPerPkt);

            int res = ps.executeUpdate();

            if (res > 0) {

                Stage stage = (Stage) ((Node) source).getScene().getWindow();

                if (null != stage && stage.isShowing()) {
                    stage.close();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {

            DBConnection.closeConnection(connection, ps, null);
        }
    }


    public void addSizeBn(ActionEvent event) {

        finalAddSize(event.getSource());

    }

    public void cancel(ActionEvent event) {

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        if (stage.isShowing()) {
            stage.close();
        }
    }
}
