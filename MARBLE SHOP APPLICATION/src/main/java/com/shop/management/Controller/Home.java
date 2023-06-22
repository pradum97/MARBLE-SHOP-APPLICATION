package com.shop.management.Controller;

import com.shop.management.Model.DailySaleReport;
import com.shop.management.util.DBConnection;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ResourceBundle;

public class Home implements Initializable {
    public Label pL;
    int rowsPerPage = 15;
    public TableColumn<DailySaleReport, String> colCategory;
    public TableColumn<DailySaleReport, String> colTotalItem;
    public TableColumn<DailySaleReport, String> colPurAmount;
    public TableColumn<DailySaleReport, String> colNetAmount;
    public TableColumn<DailySaleReport, String> colProfit;
    public Label totalPurchaseAmtL;
    public Label totalNetAmountL;
    public BorderPane mainContainer;
    public Label totalProfitL;
    public TableView<DailySaleReport> tableViewHome;
    public TableColumn<DailySaleReport, Integer> col_sno;

    public HBox refresh_bn;
    public Pagination pagination;
    private DBConnection dbConnection;
    private DecimalFormat df = new DecimalFormat("0.##");

    private ObservableList<DailySaleReport> reportList = FXCollections.observableArrayList();


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dbConnection = new DBConnection();
        getSaleItems();
    }

    private void getSaleItems() {


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

            String query = "select  product_category , count(*)as total_Item  , sum(net_amount) as total_Net_Amount , sum(purchase_price*(cast(split_part(product_quantity::TEXT,' -', 1) as double precision))) as total_Pur_Amount,\n" +
                    "        sum(net_amount-tbl_saleitems.purchase_price) as profit\n" +
                    "from tbl_saleItems  where TO_CHAR(sale_date, 'yyyy-MM-dd' ) =\n" +
                    "                          TO_CHAR(CURRENT_DATE, 'yyyy-MM-dd')   group by product_category";

            ps = connection.prepareStatement(query);

            rs = ps.executeQuery();

            double totalPurchaseAmount = 0, totalProfit = 0, totalNetAmount = 0;
            while (rs.next()) {
                String category = rs.getString("product_category");
                int totalItem = rs.getInt("total_Item");

                double totalNet_Amount = rs.getDouble("total_Net_Amount");
                double total_PurAmount = rs.getDouble("total_Pur_Amount");
                double profit = rs.getDouble("profit");

                reportList.add(new DailySaleReport(totalItem, category, totalNet_Amount, total_PurAmount, profit));

                //  totalProfit = totalProfit+profit;
                totalPurchaseAmount = totalPurchaseAmount + total_PurAmount;
                totalNetAmount = totalNetAmount + totalNet_Amount;

            }

            if (totalNetAmount > totalPurchaseAmount) {
                totalProfit = Double.parseDouble(df.format(totalNetAmount - totalPurchaseAmount));
                double percentage = Double.parseDouble(df.format((totalProfit / totalPurchaseAmount) * 100));
                totalProfitL.setText("+ " + totalProfit + " ( " + percentage + " ) %");
                pL.setStyle("-fx-text-fill: green");
                totalProfitL.setStyle("-fx-text-fill: green");


            } else if (totalPurchaseAmount > totalNetAmount) {
                totalProfit = Double.parseDouble(df.format(totalPurchaseAmount - totalNetAmount));
                double percentage = Double.parseDouble(df.format((totalProfit / totalPurchaseAmount) * 100));

                totalProfitL.setText("- " + totalProfit + " ( " + percentage + " ) %");
                pL.setStyle("-fx-text-fill: red");
                totalProfitL.setStyle("-fx-text-fill: red");

            } else {
                totalProfitL.setText("0");
            }

            totalNetAmountL.setText(String.valueOf(Double.parseDouble(df.format(totalNetAmount))));
            totalPurchaseAmtL.setText(String.valueOf(Double.parseDouble(df.format(totalPurchaseAmount))));


        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.closeConnection(connection, ps, rs);
        }

        if (reportList.size() > 0) {
            pagination.setVisible(true);
            pagination.setCurrentPageIndex(0);
            changeTableView(0, rowsPerPage);
            pagination.currentPageIndexProperty().addListener(
                    (observable1, oldValue1, newValue1) -> changeTableView(newValue1.intValue(), rowsPerPage));
        }


    }

    public void bnRefresh(MouseEvent event) {
        if (null == reportList) {
            return;
        }
        getSaleItems();

        changeTableView(pagination.getCurrentPageIndex(), rowsPerPage);
    }

    private void changeTableView(int index, int limit) {

        int totalPage = (int) (Math.ceil(reportList.size() * 1.0 / rowsPerPage));
        pagination.setPageCount(totalPage);
        col_sno.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(
                tableViewHome.getItems().indexOf(cellData.getValue()) + 1));
        colTotalItem.setCellValueFactory(new PropertyValueFactory<>("totalItems"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("productCategory"));
        colNetAmount.setCellValueFactory(new PropertyValueFactory<>("totalNetAmount"));
        colPurAmount.setCellValueFactory(new PropertyValueFactory<>("totalPurChaseAmount"));
        colProfit.setCellValueFactory(new PropertyValueFactory<>("profit"));


        int fromIndex = index * limit;
        int toIndex = Math.min(fromIndex + limit, reportList.size());

        int minIndex = Math.min(toIndex, reportList.size());
        SortedList<DailySaleReport> sortedData = new SortedList<>(
                FXCollections.observableArrayList(reportList.subList(Math.min(fromIndex, minIndex), minIndex)));
        sortedData.comparatorProperty().bind(tableViewHome.comparatorProperty());

        tableViewHome.setItems(sortedData);

    }
}
