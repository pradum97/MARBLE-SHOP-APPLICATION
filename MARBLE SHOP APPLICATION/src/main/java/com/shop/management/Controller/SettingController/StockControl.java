package com.shop.management.Controller.SettingController;

import com.shop.management.CustomDialog;
import com.shop.management.Method.Method;
import com.shop.management.util.DBConnection;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.ResourceBundle;

public class StockControl implements Initializable {
    public TextField requiredTF;
    public TextField lowTF;
    public TextField mediumTF;
    public Button submitBn;
    public VBox container;

    private Method method;
    private DBConnection dbConnection;
    private CustomDialog customDialog;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        method = new Method();
        dbConnection = new DBConnection();
        customDialog = new CustomDialog();

        getData();
    }

    private void getData() {

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = dbConnection.getConnection();

            if (null == connection) {
                return;
            }

            String query = "SELECT * FROM STOCK_CONTROL";

            ps = connection.prepareStatement(query);
            rs = ps.executeQuery();

            if (rs.next()) {

                int required = rs.getInt("REQUIRED");
                int low = rs.getInt("low");
                int medium = rs.getInt("medium");

                requiredTF.setText(String.valueOf(required));
                lowTF.setText(String.valueOf(low));
                mediumTF.setText(String.valueOf(medium));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.closeConnection(connection, ps, rs);
        }
    }

    public void submitBn(ActionEvent event) {

        switch (submitBn.getText()) {

            case "UPDATE" -> {
                container.setDisable(false);
                submitBn.setText("SUBMIT");
            }
            case "SUBMIT" -> submit();

        }

    }

    public void cancel(ActionEvent event) {

        Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        if (stage.isShowing()){
            stage.close();
        }
    }

    public void enterPress(KeyEvent keyEvent) {

        if (keyEvent.getCode() == KeyCode.ENTER){

            if (Objects.equals(submitBn.getText(),"SUBMIT")) {
                submit();
            }
        }
    }

    private void submit (){

        int i_req = Integer.parseInt(requiredTF.getText());
        int i_low = Integer.parseInt(lowTF.getText());
        int i_med = Integer.parseInt(mediumTF.getText());

        if (requiredTF.getText().isEmpty()) {
            method.show_popup("ENTER REQUIRED QUANTITY", requiredTF);
            return;
        } else if (i_req < 0) {
            method.show_popup("NEGATIVE VALUE NOT ACCEPTED", requiredTF);
            return;
        } else if (lowTF.getText().isEmpty()) {
            method.show_popup("ENTER LOW QUANTITY", lowTF);
            return;
        }
        else if (i_low < 0){
            method.show_popup("NEGATIVE VALUE NOT ACCEPTED", lowTF);
            return;
        }else if (mediumTF.getText().isEmpty()) {
            method.show_popup("ENTER MEDIUM QUANTITY", mediumTF);
            return;
        }else if (i_med < 0){
            method.show_popup("NEGATIVE VALUE NOT ACCEPTED", mediumTF);
            return;
        }

        int required = Integer.parseInt(requiredTF.getText().replaceAll("[^0-9.]", ""));
        int low = Integer.parseInt(lowTF.getText().replaceAll("[^0-9.]", ""));
        int medium = Integer.parseInt(mediumTF.getText().replaceAll("[^0-9.]", ""));

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = dbConnection.getConnection();

            if (null == connection) {
                return;
            }

            String query = "UPDATE STOCK_CONTROL SET REQUIRED = ?, LOW = ?, MEDIUM = ?";

            ps = connection.prepareStatement(query);

            ps.setInt(1, required);
            ps.setInt(2, low);
            ps.setInt(3, medium);

            int res = ps.executeUpdate();

            if (res > 0) {


                customDialog.showAlertBox("", "Successfully Updated");

                getData();
                container.setDisable(true);
                submitBn.setText("UPDATE");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.closeConnection(connection, ps, rs);
        }
    }
}
