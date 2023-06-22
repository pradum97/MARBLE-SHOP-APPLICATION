package com.shop.management.Controller.SettingController;

import com.shop.management.CustomDialog;
import com.shop.management.Method.Method;
import com.shop.management.PropertiesLoader;
import com.shop.management.util.DBConnection;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.util.Properties;
import java.util.ResourceBundle;

public class AddCustomer implements Initializable {
    public TextField nameTf;
    public TextField phoneTf;
    public TextField addressTf;
    public TextArea descriptionTf;
    private Properties propInsert;
    private Method method;
    private DBConnection dbConnection;
    private CustomDialog customDialog;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        method = new Method();
        dbConnection = new DBConnection();
        customDialog = new CustomDialog();
        PropertiesLoader propLoader = new PropertiesLoader();
        propInsert = propLoader.getInsertProp();
    }

    public void submitBn(ActionEvent event) {

        add(event.getSource());
    }

    private boolean isExistPhone(long enterPhoneNum) {

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {

            connection = dbConnection.getConnection();
            String query = "select CUSTOMER_PHONE from TBL_CUSTOMER where CUSTOMER_PHONE = ?";

            ps = connection.prepareStatement(query);
            ps.setLong(1, enterPhoneNum);

            rs = ps.executeQuery();

            return rs.next();

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
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

    public void enterPress(KeyEvent event) {

        if (event.getCode() == KeyCode.ENTER) {
            add(event.getSource());
        }
    }

    private void add(Object source) {

        String sName = nameTf.getText();
        String sPhone = phoneTf.getText();
        String address = addressTf.getText();
        String description = descriptionTf.getText();

        if (sName.isEmpty()) {
            method.show_popup("Enter  Full Name", nameTf);
            return;
        } else if (sPhone.isEmpty()) {
            method.show_popup("Enter  Phone Number", phoneTf);
            return;
        }

        long phoneNum = 0;

        try {
            phoneNum = Long.parseLong(sPhone);
        } catch (NumberFormatException e) {
            customDialog.showAlertBox("Invalid", "Phone Number Not Valid! Please Enter Valid Phone Number");
            return;
        }

        if (isExistPhone(phoneNum)) {
            method.show_popup("PHONE NUMBER ALREADY EXIST", phoneTf);
            return;
        } else if (address.isEmpty()) {
            method.show_popup("Enter Full Address", addressTf);
            return;
        }

        Connection connection = null;
        PreparedStatement ps = null;

        try {

            connection = dbConnection.getConnection();
            if (null == connection) {
                return;
            }

            String query = propInsert.getProperty("INSERT_CUSTOMER_DETAILS_ADD_CUSTOMER");
            ps = connection.prepareStatement(query);
            ps.setString(1, sName);
            ps.setLong(2, phoneNum);
            ps.setString(3, address);

            if (description.isEmpty()) {
                ps.setNull(4, Types.NULL);
            } else {
                ps.setString(4, description);
            }
            int res = ps.executeUpdate();

            if (res > 0) {
                Stage stage = (Stage) ((Node) source).getScene().getWindow();
                if (stage.isShowing()) {
                    stage.close();
                }
                customDialog.showAlertBox("success", "Customer Successfully Added");
            }

        } catch (SQLException e) {
            customDialog.showAlertBox("error", e.getMessage());
            e.printStackTrace();
        } finally {
            DBConnection.closeConnection(connection, ps, null);
        }
    }
}
