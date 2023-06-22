package com.shop.management.Controller.ReturnItems;

import com.shop.management.Controller.Login;
import com.shop.management.CustomDialog;
import com.shop.management.ImageLoader;
import com.shop.management.Main;
import com.shop.management.Method.Method;
import com.shop.management.Model.ReturnProductModel;
import com.shop.management.PropertiesLoader;
import com.shop.management.util.DBConnection;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;

import java.net.URL;
import java.sql.*;
import java.util.*;

public class ReturnProduct implements Initializable {

    public TextField invoiceNumTF;
    public Label cusNameL;
    public Label cusPhoneL;
    public Label cusAddressL;
    public Label purchaseDateL;
    public Label netAmountL;
    public Label billTypeL;
    public Label duesL;
    public TableView<ReturnProductModel> tableView;
    public TableColumn<ReturnProductModel, Integer> colSrNo;
    public TableColumn<ReturnProductModel, String> colProductName;
    public TableColumn<ReturnProductModel, String> colProductSIze;
    public TableColumn<ReturnProductModel, String> colQuantity;
    public TableColumn<ReturnProductModel, String> colRate;
    public TableColumn<ReturnProductModel, String> colAction;
    public TableColumn<ReturnProductModel, String> colAlreadyReturned;
    public TableColumn<ReturnProductModel, String> colReturnable;
    public Button searchBn;
    public TextField invoicePrefetchTF;
    public Label refundL;
    public TextArea remarkTF;
    public Button bnSubmit;
    public TextField discountTF;
    public Label gstAmountL;
    public HBox gstContainer;
    public HBox addiDiscountContainer;
    public Label addiDiscountL;
    private DBConnection dbConnection;
    private Method method;
    private CustomDialog customDialog;
    double netReturnAmtTot = 0 , gstAmount;
    private double totalReturnDisctAmount = 0;
    private int saleMainID;
    private double discountPer;
    private String oldInvoiceNumber;
    private double netAmount;
    private ObservableList<ReturnProductModel> itemList = FXCollections.observableArrayList();
    private Properties propInsert;
    private Properties propUpdate;
    private Properties propRead;
    private double additional_discount;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        method = new Method();
        dbConnection = new DBConnection();
        customDialog = new CustomDialog();
        bnSubmit.setDisable(true);
        PropertiesLoader propLoader = new PropertiesLoader();
        propUpdate = propLoader.getUpdateProp();
        propRead = propLoader.getReadProp();
        propInsert = propLoader.getInsertProp();
        textChangeListener();
        gstContainer.managedProperty().bind(gstContainer.visibleProperty());
        gstContainer.setVisible(false);
        addiDiscountContainer.managedProperty().bind(addiDiscountContainer.visibleProperty());
        addiDiscountContainer.setVisible(false);
    }

    public void bnSearch(ActionEvent event) {
        String str = searchBn.getText();
        if ("RESET".equals(str)) {
            resetValue();
            return;
        }

        String invPrefix = invoicePrefetchTF.getText();
        String invoiceNumber = invoiceNumTF.getText();

        if (invoiceNumber.isEmpty()) {

            ContextMenu menu = new ContextMenu();
            menu.setAutoHide(true);
            menu.getItems().add(new MenuItem("Enter Invoice Number "));
            menu.show(invoiceNumTF, Side.BOTTOM, 10, 0);
            return;
        }
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            connection = dbConnection.getConnection();
            if (null == connection) {
                return;
            }
            String query = propRead.getProperty("SEARCH_SALE_ITEM");

            ps = connection.prepareStatement(query);
            ps.setString(1, invPrefix + invoiceNumber);

            rs = ps.executeQuery();
            Map<Integer, ReturnProductModel> map = new HashMap<>();

            int res = 0;
            while (rs.next()) {

                int saleMainId = rs.getInt("sale_main_id");
                int saleItemId = rs.getInt("sale_item_id");
                int productId = rs.getInt("product_id");
                int stockId = rs.getInt("stock_id");

                String invoiceNum = rs.getString("invoice_number");

                String customerName = rs.getString("customer_name");
                String customerPhone = rs.getString("customer_phone");
                String customerAddress = rs.getString("customer_address");
                String saleDate = rs.getString("sale_date");
                String billType = rs.getString("bill_type");
                String productName = rs.getString("product_name");
                String size = rs.getString("product_size");
                String productQuantity = rs.getString("product_quantity");

                double net_Amount = rs.getDouble("net_amount");
                double duesAmount = rs.getDouble("dues_amount");
                double rate = rs.getDouble("sell_price");
                double discountPer = rs.getDouble("discountPer");
                double gstClaimedAmt = rs.getDouble("gst_Claimed");
                double gstAmount = rs.getDouble("tax_amount");
                double totalGstPercentage = rs.getDouble("totalGstPercentage");
                int alreadyReturned = rs.getInt("alreadyReturned");

                int quantity = Integer.parseInt(rs.getString("product_quantity").split(" -")[0]);

                String qtyUnit = productQuantity.split(" -")[1];
                if (rs.isLast()) {
                    additional_discount = rs.getDouble("additional_discount");

                    addiDiscountContainer.setVisible(additional_discount > 0);
                    res++;
                    netAmount = net_Amount;
                    cusNameL.setText(customerName.toUpperCase());
                    cusPhoneL.setText(customerPhone);
                    cusAddressL.setText(customerAddress);
                    purchaseDateL.setText(saleDate);
                    netAmountL.setText(String.valueOf(netAmount));
                    billTypeL.setText(billType);
                    duesL.setText(String.valueOf(duesAmount));
                    searchBn.setText("RESET");
                    invoiceNumTF.setEditable(false);
                    saleMainID = saleMainId;
                    oldInvoiceNumber = invoiceNum;

                }

                int key = saleItemId;
                if (map.containsKey(key)) {

                    int totReturned = Integer.parseInt(map.get(key).getAlreadyReturned().split(" -")[0]) + alreadyReturned;

                    int totQty = Integer.parseInt(productQuantity.split(" -")[0]) - totReturned;

                    ReturnProductModel rpm = new ReturnProductModel(false, productId, stockId, saleMainId, saleItemId, productName,
                            size, productQuantity, rate, discountPer, totReturned + " -" + qtyUnit, "0",
                            totQty + " -" + qtyUnit, gstAmount, totalGstPercentage, gstClaimedAmt);

                    map.put(saleItemId, rpm);

                } else {
                    String returnable = (Integer.parseInt(productQuantity.split(" -")[0]) - alreadyReturned) + " -" + qtyUnit;
                    ReturnProductModel rpm = new ReturnProductModel(false, productId, stockId, saleMainId, saleItemId, productName,
                            size, productQuantity, rate, discountPer, alreadyReturned + " -" + qtyUnit, "0", returnable, gstAmount, totalGstPercentage, gstClaimedAmt);
                    map.put(saleItemId, rpm);
                }

                itemList = FXCollections.observableArrayList(map.values());
            }
            if (res < 1) {
                customDialog.showAlertBox("Failed", "Invoice Not Found");
            }

            colSrNo.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(
                    tableView.getItems().indexOf(cellData.getValue()) + 1));
            colProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
            colProductSIze.setCellValueFactory(new PropertyValueFactory<>("productSize"));
            colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
            colRate.setCellValueFactory(new PropertyValueFactory<>("rate"));
            colAction.setCellValueFactory(new PropertyValueFactory<>("returnQuantity"));
            colAlreadyReturned.setCellValueFactory(new PropertyValueFactory<>("alreadyReturned"));
            colReturnable.setCellValueFactory(new PropertyValueFactory<>("returnable"));

            colAction.setCellFactory(TextFieldTableCell.forTableColumn());
            colAction.setOnEditCommit(e -> {
                String qtyUnit = e.getTableView().getItems().get(e.getTablePosition().getRow()).getReturnable().split(" -")[1];
                int avlQuantity = Integer.parseInt(e.getTableView().getItems().get(e.getTablePosition().getRow()).getReturnable().split(" -")[0].replaceAll("[^0-9.]", ""));
                int inputQuantity = Integer.parseInt(e.getNewValue().replaceAll("[^0-9.]", ""));

                if (inputQuantity > avlQuantity) {
                    String msg = "AVAILABLE QUANTITY :  " + avlQuantity + " -" + qtyUnit
                            + "\n\nENTER QUANTITY :  " + inputQuantity + " -" + qtyUnit;

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("QUANTITY NOT AVAILABLE");
                    alert.setHeaderText(msg);
                    alert.setContentText("Please Enter Less Then :  " + avlQuantity + " -" + qtyUnit);
                    alert.initOwner(Main.primaryStage);
                    alert.showAndWait();
                    tableView.refresh();
                } else {

                    if (inputQuantity > 0) {
                        e.getTableView().getItems().get(e.getTablePosition().getRow()).setReturn(true);
                    }
                    e.getTableView().getItems().get(e.getTablePosition().getRow()).setReturnQuantity(e.getNewValue());
                    calculate();
                }
            });
            tableView.setItems(itemList);

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.closeConnection(connection, ps, rs);
        }
    }
    private void textChangeListener() {
        discountTF.textProperty().addListener((observableValue, s, t1) -> {

            double currentDiscount = 0;
            try {
                currentDiscount = Double.parseDouble(t1.replaceAll("[^0-9.]", ""));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            if (netReturnAmtTot < currentDiscount) {
                method.show_popup("Discount Amount Should Not Be Exceed Price !", discountTF);
                discountTF.setText("0");
                return;
            }

            if (netReturnAmtTot > 0) {
                double additionalDis = Double.parseDouble(addiDiscountL.getText());
                double total = (netReturnAmtTot - currentDiscount) - additionalDis;
                refundL.setText(String.valueOf(Math.round(total)));
            } else {
                refundL.setText("0");
                addiDiscountL.setText("0");
                bnSubmit.setDisable(true);
            }
        });
    }

    private void resetValue() {
        cusNameL.setText("");
        cusPhoneL.setText("");
        cusAddressL.setText("");
        purchaseDateL.setText("");
        netAmountL.setText("");
        billTypeL.setText("");
        duesL.setText("");
        invoiceNumTF.setText("");
        searchBn.setText("SEARCH");
        invoiceNumTF.setEditable(true);
        if (!itemList.isEmpty()) {
            itemList.clear();
        }


        calculate();

        gstContainer.managedProperty().bind(gstContainer.visibleProperty());
        gstContainer.setVisible(false);

        addiDiscountContainer.managedProperty().bind(addiDiscountContainer.visibleProperty());
        addiDiscountContainer.setVisible(false);

        gstAmountL.setText("0");
        addiDiscountL.setText("0");
        refundL.setText("0");

        tableView.refresh();
    }

    public void bnSubmit(ActionEvent event) {
        ImageView image = new ImageView(new ImageLoader().load("img/icon/warning_ic.png"));
        image.setFitWidth(45);
        image.setFitHeight(45);
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setAlertType(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Warning ");
        alert.setGraphic(image);
        alert.setHeaderText("Are you sure you want to Return This Item");
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.initOwner(Main.primaryStage);
        Optional<ButtonType> result = alert.showAndWait();
        ButtonType button = result.orElse(ButtonType.CANCEL);
        if (button == ButtonType.OK) {
            try {
                finalReturn();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            alert.close();
        }
    }

    private void finalReturn() throws SQLException {

        String remark = remarkTF.getText();

        double refundAmount = 0;
        try {
            refundAmount = Double.parseDouble(refundL.getText());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        Connection connection = null;
        PreparedStatement psReturnMain = null, psReturnItem = null, psUpdateStock = null , psAddiDis = null;
        ResultSet rs = null;

        try {
            connection = dbConnection.getConnection();
            connection.setAutoCommit(false);

            String returnMainQuery = propInsert.getProperty("INSERT_RETURN_MAIN");

            psReturnMain = connection.prepareStatement(returnMainQuery, new String[]{"return_main_id"});
            psReturnMain.setInt(1, saleMainID);
            psReturnMain.setInt(2, Login.currentlyLogin_Id);
            psReturnMain.setDouble(3, refundAmount);
            psReturnMain.setString(4, getInvoiceNumber()); // invoice Num
            if (remark.isEmpty()) {
                psReturnMain.setNull(5, Types.NULL);
            } else {
                psReturnMain.setString(5, remark);
            }
            psReturnMain.setString(6, oldInvoiceNumber);


            int res = psReturnMain.executeUpdate();
            if (res > 0) {
                int return_main_id = 0;
                rs = psReturnMain.getGeneratedKeys();

                if (rs.next()) {
                    return_main_id = rs.getInt(1);

                    String addiDisUpdateQuery = "update tbl_sale_main set additional_discount = additional_discount - ?," +
                            "net_amount = net_amount+ ? , tot_disc_amount = tot_disc_amount- ? where sale_main_id = ?";

                    psAddiDis = connection.prepareStatement(addiDisUpdateQuery);
                    psAddiDis.setDouble(1,additional_discount);
                    psAddiDis.setDouble(2,additional_discount);
                    psAddiDis.setDouble(3,additional_discount);
                    psAddiDis.setInt(4,saleMainID);
                    psAddiDis.executeUpdate();
                }

                for (ReturnProductModel rp : tableView.getItems()) {

                    if (rp.isReturn()) {
                        String quantityUnit = rp.getQuantity().split(" -")[1];
                        res = 0;
                        int returnQuantity = 0;
                        try {
                            returnQuantity = Integer.parseInt(rp.getReturnQuantity().replaceAll("[^0-9.]", ""));
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                        String returnItemsQuery = propInsert.getProperty("INSERT_RETURN_ITEM");
                        psReturnItem = connection.prepareStatement(returnItemsQuery);
                        psReturnItem.setInt(1, return_main_id);
                        psReturnItem.setInt(2, rp.getProductID());
                        psReturnItem.setInt(3, returnQuantity);
                        psReturnItem.setDouble(4, discountPer);
                        psReturnItem.setDouble(5, rp.getRate());
                        psReturnItem.setString(6, quantityUnit);
                        psReturnItem.setInt(7, rp.getSaleItemId());
                        res = psReturnItem.executeUpdate();
                        if (res > 0) {
                            res = 0;
                            String updateQuery = propUpdate.getProperty("UPDATE_STOCK_AFTER_RETURN");
                            PreparedStatement updatePstmt = connection.prepareStatement(updateQuery);
                            updatePstmt.setInt(1, returnQuantity);
                            updatePstmt.setInt(2, rp.getStockId());
                            res = updatePstmt.executeUpdate();

                        }
                    }


                }
            }

            if (res > 0) {
                connection.commit();
                customDialog.showAlertBox("Success", "Successfully Returned");
                resetValue();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            connection.rollback();
        } finally {
            DBConnection.closeConnection(connection, psReturnMain, rs);
            if (null != psReturnItem) {
                psReturnItem.close();
            }
            if (null != psUpdateStock) {
                psUpdateStock.close();
            }
        }
    }

    private void calculate() {

        netReturnAmtTot = 0;
        totalReturnDisctAmount = 0;
        discountPer = 0;
        gstAmount = 0;

        bnSubmit.setDisable(true);
        for (ReturnProductModel rp : tableView.getItems()) {

            if (rp.isReturn()) {
                bnSubmit.setDisable(false);
                int returnQuantity = 0;
                try {
                    returnQuantity = Integer.parseInt(rp.getReturnQuantity().replaceAll("[^0-9.]", ""));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                double rate = rp.getRate();

                double disPer = rp.getDiscountPercentage();
                double disAmt =( (rate * disPer) / 100)* returnQuantity;
                double reAmt = (rate * returnQuantity);

                double gstAmt = ((((reAmt-disAmt) * 100) / (100 + rp.getTotalGstPercentage()))*rp.getTotalGstPercentage())/100;


                if (rp.getGstClaimedAmount() > 0) {
                    gstContainer.setVisible(true);
                    netReturnAmtTot = netReturnAmtTot - gstAmt;
                    gstAmount += gstAmt;
                } else {
                    gstContainer.managedProperty().bind(gstContainer.visibleProperty());
                    gstContainer.setVisible(false);
                }

                totalReturnDisctAmount += disAmt;
                netReturnAmtTot += reAmt;
                addiDiscountL.setText(String.valueOf(additional_discount));
            }
        }

        gstAmountL.setText(String.valueOf(Math.round(gstAmount)));
        discountTF.setText(String.valueOf(Math.round(totalReturnDisctAmount)));

    }

    public void bnReturnHistory(ActionEvent event) {
        customDialog.showFxmlFullDialog("returnItems/returnHistory.fxml", "RETURN HISTORY");
    }

    public String getInvoiceNumber() {
        dbConnection = new DBConnection();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            connection = dbConnection.getConnection();
            ps = connection.prepareStatement("select max(RETURN_MAIN_ID) from TBL_RETURN_MAIN");
            rs = ps.executeQuery();
            String invoiceNum = null;
            if (rs.next()) {
                long id = rs.getInt(1) + 1;
                invoiceNum = String.format("%07d", id);
            }
            return "SUMA" + invoiceNum + "R";
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            DBConnection.closeConnection(connection, ps, rs);
        }

    }

    public void enterPress(KeyEvent event) {

        if (event.getCode() == KeyCode.ENTER) {
            bnSearch(null);
        }
    }
}




