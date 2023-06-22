package com.shop.management.Controller;

import com.shop.management.CustomDialog;
import com.shop.management.Main;
import com.shop.management.Model.Shop;
import com.shop.management.util.DBConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.ResourceBundle;

public class ShopDetails implements Initializable {
    @FXML
    public Label shopName;
    public Label sPhone_1;
    public Label sPhone_2;
    public Label sEmail;
    public Label sGstNum;
    public Label sAddress;
    public Label propName;
    public Label updateBn;

    private DBConnection dbConnection;
    private CustomDialog customDialog;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dbConnection = new DBConnection();
        customDialog = new CustomDialog();

        setData();
        updateBn.managedProperty().bind(updateBn.visibleProperty());
        updateBn.setVisible(Objects.equals(Login.currentRoleName.toLowerCase(), "admin".toLowerCase()));

    }

    private void setData() {

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            connection = dbConnection.getConnection();

            String query = "select * from tbl_shop_details";

            ps = connection.prepareStatement(query);

            rs = ps.executeQuery();

            while (rs.next()) {

                String shop_name = rs.getString("shop_name");
                String phone_1 = rs.getString("shop_phone_1");
                String phone_2 = rs.getString("shop_phone_2");
                String email = rs.getString("shop_email");
                String gstNum = rs.getString("shop_gst_number");
                String address = rs.getString("shop_address");
                String prop = rs.getString("shop_prop");

                shopName.setText(shop_name);
                sPhone_1.setText(phone_1);
                sPhone_2.setText(phone_2);
                sEmail.setText(email);
                sGstNum.setText(gstNum);
                sAddress.setText(address);
                propName.setText(prop);
            }


        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.closeConnection(connection, ps, rs);
        }
    }

    public void bnUpdate(MouseEvent event) {

        String sName = shopName.getText();
        String phone_1 = sPhone_1.getText();
        String phone_2 = sPhone_2.getText();
        String email = sEmail.getText();
        String address = sAddress.getText();
        String gstNum = sGstNum.getText();
        String prop = propName.getText();

        Shop shop = new Shop(sName , phone_1 , phone_2 , email , address ,gstNum,prop );
        Main.primaryStage.setUserData(shop);

        customDialog.showFxmlDialog("update/shopDetailsUpdate.fxml", "UPDATE");
        setData();
    }

    public void cancel(ActionEvent event) {

        Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        if (stage.isShowing()){
            stage.close();
        }
    }
}
