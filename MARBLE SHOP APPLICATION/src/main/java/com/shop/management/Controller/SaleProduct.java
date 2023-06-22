package com.shop.management.Controller;

import com.shop.management.CustomDialog;
import com.shop.management.ImageLoader;
import com.shop.management.Main;
import com.shop.management.Model.Products;
import com.shop.management.util.AppConfig;
import com.shop.management.util.DBConnection;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.ResourceBundle;

public class SaleProduct implements Initializable {
    int rowsPerPage = 15;
    public TableColumn<Products, String> colColor;
    public TableColumn<Products, Integer> colSrNo;
    public TableColumn<Products, String> colProductName;
    public TableColumn<Products, String> colProductCode;
    public TableColumn<Products, String> colType;
    public TableColumn<Products, String> colCategory;
    public TableColumn<Products, String> colDiscount;
    public TableColumn<Products, String> colTax;
    public TableColumn<Products, String> colPrice;
    public TableView<Products> tableView;
    public TableColumn<Products, String> colSize;
    public Label cartCountL;
    public BorderPane mainContainer;
    public TextField searchTf;
    public TableColumn<Products, String> colHsnSac;
    public Pagination pagination;
    private DBConnection dbconnection;
    private CustomDialog customDialog;
    private ObservableList<Products> productsList = FXCollections.observableArrayList();
    private FilteredList<Products> filteredData;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        dbconnection = new DBConnection();
        customDialog = new CustomDialog();
        getProduct();
    }

    private void getProduct() {

        if (null != productsList) {
            productsList.clear();
        }

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            String query = "SELECT tp.product_id,(TO_CHAR(tp.added_date, 'DD-MM-YYYY HH12:MI:SS AM')) as added_date, tp.product_name ,tp.product_code, tp.product_description\n" +
                    "        ,tp.product_color,tp.product_type,tc.category_id, tc.category_name,\n" +
                    "       tp.discount_id ,tp.tax_id ,\n" +
                    "       td.discount_id ,td.discount,tpt.tax_id ,\n" +
                    "       tpt.tax_id ,tpt.sgst,tpt.cgst,tpt.igst , tpt.hsn_sac,tpt.description,tpt.gstName,\n" +
                    "       (select string_agg(concat(tps.height , 'x' , tps.width ,' ', tps.size_unit ),', ' ) as height_width\n" +
                    "        from tbl_product_stock as tps where  tps.product_id = tp.product_id group by tp.product_id )\n" +
                    "\n" +
                    "FROM   tbl_products as tp\n" +
                    "    LEFT JOIN tbl_category as tc ON tp.category_id = tc.category_id\n" +
                    "           Left JOIN tbl_discount as td  ON ( tp.discount_id = td.discount_id )\n" +
                    "           Left Join tbl_product_tax as tpt  on ( tp.tax_id = tpt.tax_id ) ORDER BY tp.product_id DESC";


            connection = dbconnection.getConnection();

            if (null == connection) {
                return;
            }
            ps = connection.prepareStatement(query);
            rs = ps.executeQuery();

            while (rs.next()) {

                int productID = rs.getInt("product_id");
                String productName = rs.getString("product_name");
                String productDescription = rs.getString("product_description");
                String productColor = rs.getString("product_color");
                String productType = rs.getString("product_type");
                String productCategory = rs.getString("category_name");
                String addedDate = rs.getString("added_date");
                String productCode = " " + rs.getString("product_code");

                // discount
                int discountID = rs.getInt("discount_id");
                double totalDiscount = rs.getInt("discount");


                // tax
                int hsnSac = rs.getInt("hsn_sac");
                int taxId = rs.getInt("tax_id");
                double sgst = rs.getInt("sgst");
                double cgst = rs.getInt("cgst");
                double igst = rs.getInt("igst");

                String size = rs.getString("height_width");

                if (null == size) {
                    size = "-";
                }

                double totalTaxPer = sgst + cgst + igst;

                productsList.add(new Products(0, productID, 0, 0,
                        0, 0, 0, size,
                        null, 0, productID, productName, productDescription, productColor,
                        productType, productCategory, discountID, taxId, null, addedDate,
                        totalDiscount, totalTaxPer, hsnSac, productCode));
            }

            if (productsList.size() > 0) {
                pagination.setVisible(true);
                search_Item();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {

            DBConnection.closeConnection(connection, ps, rs);

        }

    }

    private void customColumn(TableColumn<Products, String> columnName) {

        columnName.setCellFactory(tc -> {
            TableCell<Products, String> cell = new TableCell<>();
            Text text = new Text();
            text.setStyle("-fx-font-size: 14");
            cell.setGraphic(text);
            text.setStyle("-fx-text-alignment: CENTER ;");
            cell.setPrefHeight(Control.USE_COMPUTED_SIZE);
            text.wrappingWidthProperty().bind(columnName.widthProperty());
            text.textProperty().bind(cell.itemProperty());
            return cell;
        });
    }

    public void bnRefresh(MouseEvent event) {

        getProduct();
        tableView.refresh();
        changeTableView(pagination.getCurrentPageIndex(), rowsPerPage);
    }

    public void bnViewCart(MouseEvent event) {
        try {
            Main.primaryStage.setUserData(false);
            Stage stage = new Stage();
            Parent parent = FXMLLoader.load(Objects.requireNonNull(CustomDialog.class.getResource("dashboard/cart.fxml")));
            stage.getIcons().add(new ImageLoader().load(AppConfig.APPLICATION_ICON));

            stage.setTitle("YOUR CART");
            stage.setMaximized(false);
            Scene scene = new Scene(parent);
            stage.setScene(scene);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(Main.primaryStage);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
        countCart();

        if (Main.primaryStage.getUserData() instanceof Boolean) {
            boolean isSuccess = (boolean) Main.primaryStage.getUserData();
            if (isSuccess) {
                searchTf.setText("");
                bnRefresh(null);
            }
        }
    }

    private void countCart() {

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {

            connection = dbconnection.getConnection();

            if (null == connection) {

                return;
            }

            ps = connection.prepareStatement("select count(*) from tbl_cart");

            rs = ps.executeQuery();

            while (rs.next()) {
                int count = rs.getInt("count");
                cartCountL.setText(String.valueOf(count));
            }


        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.closeConnection(connection, ps, rs);
        }
    }

    private void search_Item() {

        filteredData = new FilteredList<>(productsList, p -> true);

        searchTf.textProperty().addListener((observable, oldValue, newValue) -> {

            filteredData.setPredicate(products -> {

                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();

                if (products.getProductName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;

                } else if (products.getProductCode().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (products.getProductType().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (products.getCategory().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (products.getProductColor().toLowerCase().contains(lowerCaseFilter)) {

                    return true;
                } else if (products.getAdded_date().toLowerCase().contains(lowerCaseFilter)) {

                    return true;
                } else return String.valueOf(products.getHsn_sac()).toLowerCase().contains(lowerCaseFilter);
            });

            changeTableView(pagination.getCurrentPageIndex(), rowsPerPage);
        });

        pagination.setCurrentPageIndex(0);
        changeTableView(0, rowsPerPage);
        pagination.currentPageIndexProperty().addListener(
                (observable1, oldValue1, newValue1) -> changeTableView(newValue1.intValue(), rowsPerPage));

    }

    private void changeTableView(int index, int limit) {

        int totalPage = (int) (Math.ceil(filteredData.size() * 1.0 / rowsPerPage));
        pagination.setPageCount(totalPage);

        colSrNo.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(
                tableView.getItems().indexOf(cellData.getValue()) + 1));
        colColor.setCellValueFactory(new PropertyValueFactory<>("productColor"));
        colProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colProductCode.setCellValueFactory(new PropertyValueFactory<>("productCode"));
        colType.setCellValueFactory(new PropertyValueFactory<>("productType"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colDiscount.setCellValueFactory(new PropertyValueFactory<>("totalDiscount"));
        colTax.setCellValueFactory(new PropertyValueFactory<>("totalTax"));
        colSize.setCellValueFactory(new PropertyValueFactory<>("sizeUnit"));
        colHsnSac.setCellValueFactory(new PropertyValueFactory<>("hsn_sac"));

        int fromIndex = index * limit;
        int toIndex = Math.min(fromIndex + limit, productsList.size());

        int minIndex = Math.min(toIndex, filteredData.size());
        SortedList<Products> sortedData = new SortedList<>(
                FXCollections.observableArrayList(filteredData.subList(Math.min(fromIndex, minIndex), minIndex)));
        sortedData.comparatorProperty().bind(tableView.comparatorProperty());

        tableView.setItems(sortedData);


        setOptionalCell();


        customColumn(colProductName);
        customColumn(colSize);
        customColumn(colProductCode);
        countCart();

    }

    private void setOptionalCell() {

        Callback<TableColumn<Products, String>, TableCell<Products, String>>
                cellSize = (TableColumn<Products, String> param) -> new TableCell<>() {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);

                } else {

                    Label bnCheckPrice = new Label("CHECK PRICE");
                    Label bnAddCart = new Label("➕ ADD TO CART");

                    bnCheckPrice.getStyleClass().add("checkItem");
                    bnAddCart.setStyle("-fx-background-color: #625603;" +
                            "-fx-padding: 5 15 5 15 ; -fx-background-radius: 30; -fx-text-fill: white; " +
                            "-fx-alignment: center;-fx-cursor: hand");
                    bnAddCart.setOnMouseClicked(event -> {

                        Products products = tableView.getSelectionModel().getSelectedItem();

                        if (null != products) {
                            Main.primaryStage.setUserData(products);
                            customDialog.showFxmlDialog("sellItems/selectSize.fxml", "Select Size");
                            countCart();
                        }
                    });

                    bnCheckPrice.setOnMouseClicked(event -> {
                        Products products = tableView.getSelectionModel().getSelectedItem();
                        if (null == tableView) {
                            {
                                return;
                            }
                        }
                        Main.primaryStage.setUserData(products);
                        customDialog.showFxmlDialog("ViewSizeAndPrice.fxml", "SIZE AND PRICE CHART");
                    });


                    HBox container = new HBox(bnCheckPrice, bnAddCart);
                    container.setStyle("-fx-alignment:center");
                    HBox.setMargin(bnCheckPrice, new Insets(2, 20, 2, 20));
                    HBox.setMargin(bnAddCart, new Insets(2, 20, 2, 0));
                    setGraphic(container);
                    setText(null);

                }
            }

        };
        colPrice.setCellFactory(cellSize);
    }
}
