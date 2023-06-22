package com.shop.management.Controller;

import com.shop.management.CustomDialog;
import com.shop.management.ImageLoader;
import com.shop.management.Main;
import com.shop.management.Method.StaticData;
import com.shop.management.Model.StockMainModel;
import com.shop.management.util.DBConnection;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class StockReport implements Initializable {


    public ComboBox<String> filterCom;
    private int rowsPerPage = 15;
    public TableView<StockMainModel> tableView;
    public TableColumn<StockMainModel, Integer> colSrNo;
    public TableColumn<StockMainModel, String> colProductID;
    public TableColumn<StockMainModel, String> colMrp;
    public TableColumn<StockMainModel, String> colProductType;
    public TableColumn<StockMainModel, String> colProductCategory;
    public TableColumn<StockMainModel, String> colProductSize;
    public TableColumn<StockMainModel, String> colProductQuantity;
    public TableColumn<StockMainModel, String> colStockStatus;
    public TableColumn<StockMainModel, String> colProductColor;
    public Label totalItemL;
    public Pagination pagination;
    public TextField searchTf;

    private DBConnection dbConnection;
    private CustomDialog customDialog;

    private int requiredQuantity;
    private int lowQuantity;
    private int mediumQuantity;

    private FilteredList<StockMainModel> filteredData;
    private ObservableList<StockMainModel> stockList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dbConnection = new DBConnection();
        customDialog = new CustomDialog();
        comboBoxConfig();
    }

    private void comboBoxConfig() {

        filterCom.setItems(new StaticData().stockFilter());
        filterCom.getSelectionModel().selectFirst();

        filterCom.setStyle("-fx-font-size: 12 ; -fx-focus-traversable: false");

        filterCom.setButtonCell(new ListCell<>() {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    setText(item);
                    setAlignment(Pos.CENTER);
                    Insets old = getPadding();
                    setPadding(new Insets(old.getTop(), 0, old.getBottom(), 0));
                }
            }
        });

        filterCom.setCellFactory(new Callback<>() {
            @Override
            public ListCell<String> call(ListView<String> list) {
                return new ListCell<String>() {
                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null) {
                            setText(item);
                            setAlignment(Pos.CENTER);
                            setPadding(new Insets(3, 3, 3, 0));
                        }
                    }
                };
            }
        });

        filterCom.valueProperty().addListener((observableValue, s, t1) -> {

            filterBy();
        });

        getStockSetting();

    }

    private void getStockItem(String filterBy) {
        if (null != stockList) {
            stockList.clear();
        }

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = dbConnection.getConnection();
            if (null == connection) {
                return;
            }

            String query = "select (select  count(stock_id) as totalItem from tbl_product_stock ), tp.product_id , tps.stock_id , tp.product_code , tps.purchase_price , tps.product_mrp , tps.min_sellingprice,\n" +
                    "       tps.quantity , tps.quantity_unit,tps.pcs_per_packet , tp.product_type , tc.category_name ,concat(tps.quantity,' -',tps.quantity_unit) as fullQuantity ,\n" +
                    "       concat(tps.height,'x' , tps.width ,' ' ,tps.size_unit) as size  , tp.product_color from tbl_product_stock tps\n" +
                    " left join tbl_products tp on tps.product_id = tp.product_id\n" +
                    " left join tbl_category tc on tp.category_id = tc.category_id order by tps.stock_id asc ";


            ps = connection.prepareStatement(query);

            rs = ps.executeQuery();

            while (rs.next()) {

                int productId = rs.getInt("product_id");
                int stockId = rs.getInt("stock_id");
                int pcaPerPkt = rs.getInt("pcs_per_packet");

                String productCode = rs.getString("product_code");

                double purchasePrice = rs.getDouble("purchase_price");
                double mrp = rs.getDouble("product_mrp");
                double minSellPrice = rs.getDouble("min_sellingprice");

                int quantity = rs.getInt("quantity");

                String category = rs.getString("category_name");
                String type = rs.getString("product_type");
              //  String fullQuantity = rs.getString("fullQuantity");
                String size = rs.getString("size");
                String color = rs.getString("product_color");
                String quantityUnit = rs.getString("quantity_unit");

                String fullQuantity;

                if (quantityUnit.equals("PCS")){

                    int pkt = quantity / pcaPerPkt;
                    int pcs = quantity % pcaPerPkt;

                    fullQuantity = pkt+" - PKT , " + pcs+" - PCS";
                }else {
                    fullQuantity = quantity+" - "+quantityUnit;
                }


                switch (filterBy) {

                    case "ALL" ->  stockList.add(new StockMainModel(productId, stockId, quantity, productCode, type, category, size, color, fullQuantity, purchasePrice, mrp, minSellPrice));

                    case "Out Of Stock" -> {
                        if (quantity <= requiredQuantity) {
                            stockList.add(new StockMainModel(productId, stockId, quantity, productCode, type, category, size, color, fullQuantity, purchasePrice, mrp, minSellPrice));
                        } else {
                            if (null != stockList) {
                                tableView.refresh();
                                changeTableView(pagination.getCurrentPageIndex(), rowsPerPage);
                            }
                        }
                    }
                    case "LOW" -> {

                        if (quantity > requiredQuantity && quantity <= lowQuantity) {
                            stockList.add(new StockMainModel(productId, stockId, quantity, productCode, type, category, size, color, fullQuantity, purchasePrice, mrp, minSellPrice));
                        } else {
                            if (null != stockList) {
                                tableView.refresh();
                                changeTableView(pagination.getCurrentPageIndex(), rowsPerPage);
                            }
                        }
                    }
                    case "MEDIUM" -> {
                        if (quantity > lowQuantity && quantity <= mediumQuantity) {

                            stockList.add(new StockMainModel(productId, stockId, quantity, productCode, type, category, size, color, fullQuantity, purchasePrice, mrp, minSellPrice));
                        } else {
                            if (null != stockList) {
                                tableView.refresh();
                                changeTableView(pagination.getCurrentPageIndex(), rowsPerPage);
                            }
                        }
                    }
                    case "HIGH" -> {
                        if (quantity > mediumQuantity) {
                            stockList.add(new StockMainModel(productId, stockId, quantity, productCode, type, category, size, color, fullQuantity, purchasePrice, mrp, minSellPrice));
                        } else {
                            if (null != stockList) {
                                tableView.refresh();
                                changeTableView(pagination.getCurrentPageIndex(), rowsPerPage);
                            }
                        }
                    }
                }

                if (rs.isLast()) {
                    totalItemL.setText(rs.getString("totalItem"));
                }
            }

            if (stockList.size() > 0) {
                pagination.setVisible(true);
                search_Item();
            }


        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.closeConnection(connection, ps, rs);
        }
    }

    private void filterBy() {

        String filter = filterCom.getSelectionModel().getSelectedItem();
        getStockItem(filter);
    }

    private void getStockSetting() {
        requiredQuantity = 0;
        mediumQuantity = 0;
        lowQuantity = 0;

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

                requiredQuantity = rs.getInt("REQUIRED");
                lowQuantity = rs.getInt("low");
                mediumQuantity = rs.getInt("medium");
            }


        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.closeConnection(connection, ps, rs);
        }

        filterBy();
    }


    private void search_Item() {

        filteredData = new FilteredList<>(stockList, p -> true);

        searchTf.textProperty().addListener((observable, oldValue, newValue) -> {

            filteredData.setPredicate(products -> {

                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();

                if (products.getProductCode().toLowerCase().contains(lowerCaseFilter)) {
                    return true;

                } else if (products.getProductCategory().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (products.getProductColor().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (String.valueOf(products.getQuantity()).toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (products.getProductType().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });

            changeTableView(pagination.getCurrentPageIndex(), rowsPerPage);
        });

        pagination.setCurrentPageIndex(0);
        changeTableView(0, rowsPerPage);
        pagination.currentPageIndexProperty().addListener(
                (observable1, oldValue1, newValue1) -> {
                    changeTableView(newValue1.intValue(), rowsPerPage);
                });

    }

    private void changeTableView(int index, int limit) {

        int totalPage = (int) (Math.ceil(filteredData.size() * 1.0 / rowsPerPage));
        pagination.setPageCount(totalPage);

        colSrNo.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(
                tableView.getItems().indexOf(cellData.getValue()) + 1));
        colProductID.setCellValueFactory(new PropertyValueFactory<>("productCode"));
        colMrp.setCellValueFactory(new PropertyValueFactory<>("mrp"));
        colProductType.setCellValueFactory(new PropertyValueFactory<>("productType"));
        colProductCategory.setCellValueFactory(new PropertyValueFactory<>("productCategory"));
        colProductColor.setCellValueFactory(new PropertyValueFactory<>("productColor"));
        colProductSize.setCellValueFactory(new PropertyValueFactory<>("productSize"));
        colProductQuantity.setCellValueFactory(new PropertyValueFactory<>("productFullQuantity"));

        setOptionalCells();

        int fromIndex = index * limit;
        int toIndex = Math.min(fromIndex + limit, stockList.size());

        int minIndex = Math.min(toIndex, filteredData.size());
        SortedList<StockMainModel> sortedData = new SortedList<>(
                FXCollections.observableArrayList(filteredData.subList(Math.min(fromIndex, minIndex), minIndex)));
        sortedData.comparatorProperty().bind(tableView.comparatorProperty());

        tableView.setItems(sortedData);


    }

    private void setOptionalCells() {

        Callback<TableColumn<StockMainModel, String>, TableCell<StockMainModel, String>>
                cellFactory = (TableColumn<StockMainModel, String> param) -> new TableCell<>() {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);

                } else {
                    int quantity = tableView.getItems().get(getIndex()).getQuantity();
                    Label status = new Label();
                    Label reStock = new Label("Add Stock");

                    ImageView iv = new ImageView(new ImageLoader().load("img/menu_icon/reStock_ic.png"));
                    iv.setFitHeight(18);
                    iv.setFitWidth(18);
                    reStock.setGraphic(iv);
                    reStock.setGraphicTextGap(6);

                    double minWidth = 150;

                    status.setMinWidth(minWidth);
                    reStock.setMinWidth(115);

                    reStock.setStyle("-fx-alignment: center;-fx-padding: 5 6 5 6 ; -fx-background-color: #030c3d ; " +
                            "-fx-text-fill: white;-fx-background-radius: 5 ; -fx-cursor: hand");
                    if (quantity <= requiredQuantity ) {
                        status.setText("Out Of Stock");

                        setStatusStyle(status, "#a90606");

                    } else if (quantity >= requiredQuantity && quantity <= lowQuantity) {
                        status.setText("LOW");
                        setStatusStyle(status, "#f33737");
                    } else if (quantity >= lowQuantity && quantity <= mediumQuantity) {
                        status.setText("MEDIUM");
                        setStatusStyle(status, " #0388fa");

                    } else if (quantity >= mediumQuantity) {
                        status.setText("HIGH");
                        setStatusStyle(status, "#44ee0c");
                    }


                    reStock.setOnMouseClicked(mouseEvent -> {
                        StockMainModel smm = tableView.getSelectionModel().getSelectedItem();
                        Main.primaryStage.setUserData(smm);
                        customDialog.showFxmlDialog2("stock/reStock.fxml", "");
                        filterBy();
                    });
                    HBox managebtn = new HBox(status, reStock);
                    managebtn.setStyle("-fx-alignment:center ; -fx-padding: 0 10 0 0");
                    HBox.setMargin(reStock, new Insets(5, 20, 0, 0));

                    setGraphic(managebtn);

                    setText(null);

                }
            }
        };
        colStockStatus.setCellFactory(cellFactory);
    }

    private void setStatusStyle(Label status, String color) {

        status.setStyle(";-fx-background-radius: 5 ; -fx-text-fill: " + color + ";" +
                " -fx-padding: 3 7 3 7;-fx-alignment: center ; -fx-font-family: 'Arial Black'");
    }

    public void refreshBn(ActionEvent event) {

        getStockSetting();
        filterCom.getSelectionModel().selectFirst();
        tableView.refresh();
    }
}
