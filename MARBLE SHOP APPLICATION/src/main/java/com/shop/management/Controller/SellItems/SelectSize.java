package com.shop.management.Controller.SellItems;

import com.shop.management.Controller.Login;
import com.shop.management.CustomDialog;
import com.shop.management.Main;
import com.shop.management.Method.GetStockData;
import com.shop.management.Method.Method;
import com.shop.management.Model.Products;
import com.shop.management.Model.Quantity;
import com.shop.management.Model.Stock;
import com.shop.management.PropertiesLoader;
import com.shop.management.util.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.net.URL;
import java.sql.*;
import java.util.Properties;
import java.util.ResourceBundle;

public class SelectSize implements Initializable {

    public TableColumn<Stock, String> colSize;
    public TableColumn<Stock, String> colPurchasePrice;
    public TableColumn<Stock, String> colMrp;
    public TableColumn<Stock, String> colMinSellPrice;
    public TableColumn<Stock, String> colQuantity;
    public TableColumn<Stock, String> colAction;
    public TableColumn<Stock, String> colPriceType;
    public TableView<Stock> tableView;
    public TableColumn<Stock, String> colPcsPerPkt;

    private CustomDialog customDialog;
    private DBConnection dbConnection;
    private Products products;
    private Properties propInsert, propRead;

    private int requiredQuantity;
    private ObservableList<Stock> stockList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        customDialog = new CustomDialog();
        dbConnection = new DBConnection();
        PropertiesLoader propLoader = new PropertiesLoader();
        propRead = propLoader.getReadProp();
        propInsert = propLoader.getInsertProp();

        getStockSetting();

        products = (Products) Main.primaryStage.getUserData();
        if (null != products) {
            setTableData(products.getProductID());
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
            }


        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.closeConnection(connection, ps, rs);
        }
    }

    private void setTableData(int productID) {

        stockList = new GetStockData().getStockList(productID);

        if (null == stockList) {

            customDialog.showAlertBox("Failed", "Data Not Found");
            return;
        }
        tableView.setItems(stockList);
        colPurchasePrice.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));
        colMrp.setCellValueFactory(new PropertyValueFactory<>("productMRP"));
        colPriceType.setCellValueFactory(new PropertyValueFactory<>("priceType"));
        colPcsPerPkt.setCellValueFactory(new PropertyValueFactory<>("pcsPerPacket"));
        colMinSellPrice.setCellValueFactory(new PropertyValueFactory<>("minSellingPrice"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("fullQuantity"));
        colSize.setCellValueFactory(new PropertyValueFactory<>("fullSize"));

        Callback<TableColumn<Stock, String>, TableCell<Stock, String>> cellFactory = (TableColumn<Stock, String> param) -> new TableCell<>() {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);

                } else {

                    Label selectBn = new Label("SELECT");
                    selectBn.setStyle("-fx-background-color: #0881ea;" + "-fx-padding: 5 15 5 15 ; -fx-background-radius: 30; " +
                            "-fx-text-fill: white; " + "-fx-alignment: center;-fx-cursor: hand");

                    selectBn.setOnMouseClicked(event -> {
                        Stock stock = tableView.getSelectionModel().getSelectedItem();

                        if (null != stock) {
                            if (requiredQuantity >= stock.getQuantity()) {

                                customDialog.showAlertBox("Error", "You Can't Sell This Item Because The Quantity Is Very Low. \nYour Required Quantity Is " + requiredQuantity);
                                return;
                            }
                            Main.primaryStage.setUserData(stock);
                            customDialog.showFxmlDialog2("sellItems/quantityDialog.fxml", "");

                            Quantity quantity = null;
                            try {
                                quantity = (Quantity) Main.primaryStage.getUserData();
                            } catch (ClassCastException e) {
                                // TODO
                            }

                            if (null == quantity) {
                                return;
                            }

                            addCart_Stock(products, stock, quantity);
                        }

                    });

                    HBox managebtn = new HBox(selectBn);
                    managebtn.setStyle("-fx-alignment:center");
                    HBox.setMargin(selectBn, new Insets(5, 8, 8, 5));
                    setGraphic(managebtn);
                    setText(null);
                }
            }

        };

        colAction.setCellFactory(cellFactory);

    }

    private void addCart_Stock(Products products, Stock stock, Quantity quantity) {

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = dbConnection.getConnection();
            if (null == connection) {
                return;
            }

            ps = connection.prepareStatement(propInsert.getProperty("INSERT_CART_DETAILS"));
            ps.setInt(1, products.getProductID());
            ps.setInt(2, Login.currentlyLogin_Id);
            ps.setDouble(3, quantity.getSellingPrice());
            ps.setInt(4, stock.getStockID());
            ps.setLong(5, quantity.getQuantity());
            ps.setString(6, quantity.getQuantityUnit());
            ps.setString(7, quantity.getPriceType());

            int res = ps.executeUpdate();

            if (res > 0) {
                refreshCartItemCount();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {

            DBConnection.closeConnection(connection, ps, rs);
        }
    }

    private void refreshCartItemCount() {
    }

    public void cancel(ActionEvent event) {

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        if (stage.isShowing()) {
            stage.close();
        }
    }
}
