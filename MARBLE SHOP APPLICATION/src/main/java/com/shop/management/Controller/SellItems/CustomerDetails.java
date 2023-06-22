package com.shop.management.Controller.SellItems;

import com.shop.management.CustomDialog;
import com.shop.management.ImageLoader;
import com.shop.management.Main;
import com.shop.management.Method.Method;
import com.shop.management.PropertiesLoader;
import com.shop.management.util.DBConnection;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.net.URL;
import java.sql.*;
import java.util.*;

public class CustomerDetails implements Initializable {
    public TextField c_name;
    public TextField c_phone;
    public TextField c_address;
    public Button bnCheckOut;
    public VBox detailsContainer;
    public Label messageL;
    public RadioButton radioYes;
    public RadioButton radioNo;
    public TextField gstNumTf;
    public Button bnCheckGstNum;
    public HBox gstClaimContainer;
    public VBox gstContainer;
    public Label billTypeL;
    public Label gstClaimedL;
    public Label totalPayableL;
    public VBox gstClaimLabel;
    public Label payableAmount;
    public HBox gstVerifyContainer;
    public TextField paidAmountTF;
    public TextField duesAmountTF;
    public BorderPane main_container;
    public ScrollPane scrollView;
    public HBox defaultPayable;
    private Method method;
    private int customerId;
    private Properties propInsert, propRead;
    private boolean isGstClaimed = false;
    private CustomDialog customDialog;
    private Map<String, Object> map;
    private double totalPayable;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        method = new Method();
        customDialog = new CustomDialog();
        PropertiesLoader propLoader = new PropertiesLoader();
        propRead = propLoader.getReadProp();
        propInsert = propLoader.getInsertProp();

        map = (Map) Main.primaryStage.getUserData();

        if (null == map) {
            customDialog.showAlertBox("Failed", "Error");
            return;
        }

        radioNo.setSelected(true);
        gstClaimLabel.managedProperty().bind(gstClaimLabel.visibleProperty());
        gstClaimLabel.setVisible(false);

        detailsContainer.setDisable(true);
        c_phone.textProperty().addListener((observableValue, s, t1) -> {
            String newValue = c_phone.getText();
            if (newValue.length() != 10) {
                method.show_popup("ENTER 10-DIGIT PHONE NUMBER", c_phone);
                detailsContainer.setDisable(true);
                bnCheckOut.setVisible(true);
                messageL.setVisible(false);
                c_name.setText("");
                c_address.setText("");
                gstNumTf.setText("");
                isGstClaimed = false;
            }
        });

        radioYes.setOnMouseClicked(mouseEvent -> {
            
            radioYes.setSelected(true);
            radioNo.setSelected(false);
            isGstClaimed = true;
            gstClaimLabel.setVisible(true);

            totalPayable = ((Double) map.get("totalPayable")) - ((Double) map.get("gstAmount"));
            gstClaimedL.setText(String.valueOf(map.get("gstAmount")));
            totalPayableL.setText(String.valueOf(totalPayable));
            duesAmountTF.setText(String.valueOf(totalPayable));
            defaultPayable.managedProperty().bind(defaultPayable.visibleProperty());
            defaultPayable.setVisible(false);


        });

        radioNo.setOnMouseClicked(mouseEvent -> {
            gstClaimLabel.managedProperty().bind(gstClaimLabel.visibleProperty());
            gstClaimLabel.setVisible(false);
            radioYes.setSelected(false);
            radioNo.setSelected(true);
            isGstClaimed = false;
            defaultPayable.setVisible(true);
        });

        paidAmountTF.textProperty().addListener((observableValue, s, t1) -> {
            double receivedAmount = 0;
            try {
                receivedAmount = Double.parseDouble(t1);
            } catch (NumberFormatException ignored) {
            }

            if (receivedAmount <= totalPayable) {

                double totDues = totalPayable - receivedAmount;

                duesAmountTF.setText(String.valueOf(Math.round(totDues)));
            } else {
                paidAmountTF.setText("");
                duesAmountTF.setText("");
                method.show_popup("YOUR PAYABLE AMOUNT IS : " + totalPayable, paidAmountTF);
            }
        });

        other();
    }

    private void other() {

        billTypeL.setText((String) map.get("billType"));
        payableAmount.setText(String.valueOf(map.get("totalPayable")));

        if (Objects.equals(map.get("billType"), "GST")) {
            gstContainer.setVisible(true);

            gstContainerClaim();
            gstNumTf.textProperty().addListener((observableValue, s, t1) -> {
                gstContainerClaim();

            });
        } else {

            gstContainer.managedProperty().bind(gstContainer.visibleProperty());
            gstContainer.setVisible(false);

        }
    }

    private void gstContainerClaim() {
        if (gstNumTf.getText().isEmpty()) {
            gstClaimContainer.managedProperty().bind(gstClaimContainer.visibleProperty());
            gstClaimContainer.setVisible(false);
            gstClaimLabel.managedProperty().bind(gstClaimLabel.visibleProperty());
            gstClaimLabel.setVisible(false);
        } else {
            gstClaimContainer.setVisible(true);
            if (isGstClaimed) {
                gstClaimLabel.setVisible(true);
            }
        }
    }

    public void cancel_Bn(ActionEvent event) {

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        if (stage.isShowing()) {
            stage.close();
        }
    }

    public void submit_bn(ActionEvent event) {

        String fullName = c_name.getText();
        String cPhone = c_phone.getText();
        String address = c_address.getText();
        String gstNumber = gstNumTf.getText();

        long phoneNum;

        if (cPhone.isEmpty()) {
            method.show_popup("ENTER CUSTOMER PHONE NUMBER", c_phone);
            return;
        } else if (fullName.isEmpty()) {
            method.show_popup("ENTER CUSTOMER FULL-NAME", c_name);
            return;
        }
        try {

            phoneNum = Long.parseLong(cPhone.replaceAll("[^0-9.]", ""));

        } catch (NumberFormatException e) {
            c_phone.setText("");
            return;
        }
        if (address.isEmpty()) {
            method.show_popup("ENTER CUSTOMER ADDRESS", c_address);
            return;
        }

        ImageView image = new ImageView(new ImageLoader().load("img/icon/warning_ic.png"));
        image.setFitWidth(45);
        image.setFitHeight(45);
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setAlertType(Alert.AlertType.CONFIRMATION);
        alert.setTitle("WARNING ");
        alert.setGraphic(image);
        alert.setHeaderText("ARE YOU SURE YOU WANT TO SELL ?");
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.initOwner(Main.primaryStage);
        Optional<ButtonType> result = alert.showAndWait();
        ButtonType button = result.orElse(ButtonType.CANCEL);
        if (button == ButtonType.OK) {

            if (customerId > 0) {

                sellNow(customerId, event);

            } else {


                addNewCustomer(fullName, phoneNum, address, event, gstNumber);
            }
        } else {
            alert.close();
        }

    }


    private void sellNow(int customerId, ActionEvent event) {

        Map<String, Object> mapS = new HashMap<>();

        if (isGstClaimed) {
            double paidAmount = 0 , avlDues = 0;
            try {
                paidAmount = Double.parseDouble(paidAmountTF.getText());
                avlDues = Double.parseDouble(duesAmountTF.getText());
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            mapS.put("isGstClaimed", isGstClaimed);
            mapS.put("gstClaimedAmount", map.get("gstAmount"));
            mapS.put("billType", map.get("billType"));
            mapS.put("totalPayable", totalPayable);
            mapS.put("paidAmount", paidAmount);
            mapS.put("avlDues", avlDues);
        }
        mapS.put("customerId", customerId);
        Main.primaryStage.setUserData(mapS);

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        if (stage.isShowing()) {
            stage.close();
        }

    }

    private void addNewCustomer(String fullName, long phoneNum, String address, ActionEvent event, String gstNumber) {

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = new DBConnection().getConnection();

            if (null == connection) {
                return;
            }
            ps = connection.prepareStatement(propInsert.getProperty("INSERT_CUSTOMER_DETAILS"), new String[]{"customer_id"});

            ps.setString(1, fullName);
            ps.setLong(2, phoneNum);
            ps.setString(3, address);

            if (gstNumber.isEmpty()) {
                ps.setNull(4, Types.NULL);
            } else {
                ps.setString(4, gstNumber);
            }

            int res = ps.executeUpdate();


            if (res > 0) {

                rs = ps.getGeneratedKeys();

                if (rs.next()) {

                    int cId = rs.getInt(1);

                    sellNow(cId, event);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.closeConnection(connection, ps, rs);
        }
    }

    public void bnCheckOut(ActionEvent event)   {
        String newValue = c_phone.getText();

        if (newValue.length() == 10) {

            long phoneNum;

            try {
                phoneNum = Long.parseLong(newValue.replaceAll("[^0-9.]", ""));
                checkCustomerExits(phoneNum);

            } catch (NumberFormatException e) {
                c_phone.setText("");
            }

            detailsContainer.setDisable(false);
        } else {
            method.show_popup("ENTER 10-DIGIT PHONE NUMBER", c_phone);
            detailsContainer.setDisable(true);

        }
    }

    private void checkCustomerExits(long phoneNumber) {

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            connection = new DBConnection().getConnection();
            if (null == connection) {
                return;
            }

            ps = connection.prepareStatement(propRead.getProperty("READ_CUSTOMER_DETAILS"));
            ps.setLong(1, phoneNumber);

            rs = ps.executeQuery();
            messageL.setVisible(true);

            if (rs.next()) {
                customerId = 0;
                messageL.setText("Customer Already Exists");
                messageL.setStyle("-fx-text-fill: green");
                bnCheckOut.setVisible(false);
                bnCheckOut.managedProperty().bind(bnCheckOut.visibleProperty());

                String name = rs.getString("customer_name");
                String address = rs.getString("customer_address");
                String gstNum = rs.getString("gst_number");
                customerId = rs.getInt("customer_id");
                c_name.setText(name);
                c_address.setText(address);

                if (null == gstNum) {

                    gstNumTf.setPromptText("GST NUM NOT AVAILABLE");
                    gstClaimContainer.setVisible(false);
                } else {
                    gstNumTf.setText(gstNum);
                }
                c_name.setEditable(false);
                c_address.setEditable(false);
                gstNumTf.setEditable(false);
                detailsContainer.setDisable(false);
                gstVerifyContainer.managedProperty().bind(gstVerifyContainer.visibleProperty());
                gstVerifyContainer.setVisible(false);
                gstNumTf.setFocusTraversable(false);

            } else {
                customerId = 0;
                detailsContainer.setDisable(true);
                messageL.setText("Add New Customer".toUpperCase());
                messageL.setStyle("-fx-text-fill: #f10707");
                c_name.setEditable(true);
                c_address.setEditable(true);
                gstNumTf.setEditable(true);
                gstNumTf.setPromptText("Enter GST Number :");
                detailsContainer.setVisible(true);
                gstVerifyContainer.setVisible(true);
                bnCheckOut.setVisible(false);
                gstNumTf.setFocusTraversable(true);
                bnCheckOut.managedProperty().bind(bnCheckOut.visibleProperty());
                gstClaimContainer.managedProperty().bind(gstClaimContainer.visibleProperty());
                gstClaimContainer.setVisible(false);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.closeConnection(connection, ps, rs);
        }

    }

    public void gstVerification(ActionEvent event) {

        new CustomDialog().showFxmlFullDialog("sellItems/gstVerification.fxml", "GST VERIFICATION");
    }

    public void enterPress(KeyEvent event) {

        if (event.getCode() == KeyCode.ENTER) {
            bnCheckOut(null);
        }
    }
}
