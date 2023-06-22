package com.shop.management.Controller.SellItems;

import com.shop.management.Main;
import com.shop.management.Model.SaleItems;
import com.shop.management.PropertiesLoader;
import com.shop.management.util.DBConnection;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.ResourceBundle;

public class ViewSellItems implements Initializable {
    int rowsPerPage = 8;
    public TableColumn<SaleItems, Integer> col_sno;
    public TableColumn<SaleItems, String> colProductName;
    public TableColumn<SaleItems, String> colColor;
    public TableColumn<SaleItems, String> colSize;
    public TableColumn<SaleItems, String> colProductType;
    public TableColumn<SaleItems, String> colCategory;
    public TableColumn<SaleItems, String> colPurchasePrice;
    public TableColumn<SaleItems, String> colMrp;
    public TableColumn<SaleItems, String> colRate;
    public TableColumn<SaleItems, String> colQuantity;
    public TableColumn<SaleItems, String> colDiscName;
    public TableColumn<SaleItems, String> colDiscount;
    public TableColumn<SaleItems, String> colHsnSac;
    public TableColumn<SaleItems, String> colTax;
    public TableColumn<SaleItems, String> colTaxAmount;
    public TableColumn<SaleItems, String> colNetAmount;
    public TableColumn<SaleItems, String> colDate;
    public TableView<SaleItems> saleTableView;
    public Pagination pagination;
    private DBConnection dbConnection;
    private int sale_main_id = 0;
    private Properties propRead;
    private ObservableList<SaleItems> reportList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dbConnection = new DBConnection();
        PropertiesLoader propLoader = new PropertiesLoader();
        propRead = propLoader.getReadProp();
        sale_main_id = (int) Main.primaryStage.getUserData();
        getSaleItem();
    }

    private void getSaleItem() {
        if (null != reportList) {
            reportList.clear();
        }
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = dbConnection.getConnection();
            if (null == connection) {
                return;
            }
            ps = connection.prepareStatement(propRead.getProperty("READ_SALE_ITEMS"));
            ps.setInt(1, sale_main_id);
            rs = ps.executeQuery();
            while (rs.next()) {

                int saleItemId = rs.getInt("sale_item_id");
                int productId = rs.getInt("product_id");
                int stockId = rs.getInt("stock_id");

                String productName = rs.getString("product_name");
                String productColor = rs.getString("product_color");
                String productCategory = rs.getString("product_category");
                String productType = rs.getString("product_type");
                String productSize = rs.getString("product_size");

                double purchasePrice = rs.getDouble("purchase_price");
                double productMrp = rs.getDouble("product_mrp");
                double sellPrice = rs.getDouble("sell_price");

                String quantity = rs.getString("product_quantity");
                String discName = rs.getString("discount_name");

                double discountAmount = rs.getDouble("discount_amount");
                double taxAmount = rs.getDouble("tax_amount");
                double netAmount = rs.getDouble("net_amount");
                int hsn = rs.getInt("hsn_sac");
                int pcsPerPkt = rs.getInt("PCS_PER_PACKET");

                int igst = rs.getInt("igst");
                int sgst = rs.getInt("sgst");
                int cgst = rs.getInt("cgst");
                String saleDate = rs.getString("sale_date");

                String discountName;
                if (null == discName || discName.isEmpty()) {
                    discountName = "-";
                } else {
                    discountName = discName;
                }

                String priceType = rs.getString("price_type");
                String fullDiscount = discountAmount + " ( " + rs.getString("discountPer") + " % )";
                int tax = igst + cgst + sgst;
                String sellRate = sellPrice + " / " + priceType;

                reportList.add(new SaleItems(saleItemId, productId, stockId, productName, productColor, productSize, productType, productCategory, purchasePrice, productMrp,
                        discountAmount, taxAmount, netAmount, discountName, quantity, hsn, tax, igst, cgst, sgst, saleDate, fullDiscount, sellRate));

            }
            if (null != reportList) {
                if (reportList.size() > 0) {

                    pagination.setVisible(true);
                    pagination.setCurrentPageIndex(0);
                    changeTableView(0, rowsPerPage);
                    pagination.currentPageIndexProperty().addListener((observable1, oldValue1, newValue1) -> changeTableView(newValue1.intValue(), rowsPerPage));
                }

            }


        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.closeConnection(connection, ps, rs);
        }
    }

    private void changeTableView(int index, int limit) {

        int totalPage = (int) (Math.ceil(reportList.size() * 1.0 / rowsPerPage));
        pagination.setPageCount(totalPage);

        col_sno.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(saleTableView.getItems().indexOf(cellData.getValue()) + 1));
        colProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colColor.setCellValueFactory(new PropertyValueFactory<>("productColor"));
        colSize.setCellValueFactory(new PropertyValueFactory<>("productSize"));
        colProductType.setCellValueFactory(new PropertyValueFactory<>("productType"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("productCategory"));
        colPurchasePrice.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));
        colMrp.setCellValueFactory(new PropertyValueFactory<>("mrp"));
        colRate.setCellValueFactory(new PropertyValueFactory<>("sellPrice"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colDiscName.setCellValueFactory(new PropertyValueFactory<>("discountName"));
        colDiscount.setCellValueFactory(new PropertyValueFactory<>("discountPer"));
        colHsnSac.setCellValueFactory(new PropertyValueFactory<>("hsnSac"));
        colTax.setCellValueFactory(new PropertyValueFactory<>("tax"));
        colTaxAmount.setCellValueFactory(new PropertyValueFactory<>("taxAmount"));
        colNetAmount.setCellValueFactory(new PropertyValueFactory<>("netAmount"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("sellingDate"));

        int fromIndex = index * limit;
        int toIndex = Math.min(fromIndex + limit, reportList.size());

        int minIndex = Math.min(toIndex, reportList.size());
        SortedList<SaleItems> sortedData = new SortedList<>(FXCollections.observableArrayList(reportList.subList(Math.min(fromIndex, minIndex), minIndex)));
        sortedData.comparatorProperty().bind(saleTableView.comparatorProperty());

        saleTableView.setItems(sortedData);
        customColumn(colProductName);
        customColumn(colDiscName);
        customColumn(colDate);

    }

    private void customColumn(TableColumn<SaleItems, String> columnName) {

        columnName.setCellFactory(tc -> {
            TableCell<SaleItems, String> cell = new TableCell<>();
            Text text = new Text();
            text.setStyle("-fx-font-size: 14");
            cell.setGraphic(text);
            text.setStyle("-fx-text-alignment: CENTER ; -fx-padding: 10");
            cell.setPrefHeight(Control.USE_COMPUTED_SIZE);
            text.wrappingWidthProperty().bind(columnName.widthProperty());
            text.textProperty().bind(cell.itemProperty());
            return cell;
        });
    }
}
