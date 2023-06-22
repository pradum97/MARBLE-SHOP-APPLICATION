package com.shop.management.Controller.Proposal;

import com.shop.management.Controller.Login;
import com.shop.management.CustomDialog;
import com.shop.management.ImageLoader;
import com.shop.management.Main;
import com.shop.management.Method.GenerateInvoice;
import com.shop.management.Model.ProposalMainModel;
import com.shop.management.PropertiesLoader;
import com.shop.management.util.AppConfig;
import com.shop.management.util.DBConnection;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

public class ProposalMain implements Initializable {
    public TableColumn<ProposalMainModel, String> colAction;
    private int rowsPerPage = 15;
    public TextField searchTf;
    public TableView<ProposalMainModel> tableView;
    public TableColumn<ProposalMainModel, Integer> colSrNo;
    public TableColumn<ProposalMainModel, String> colCname;
    public TableColumn<ProposalMainModel, String> colPhone;
    public TableColumn<ProposalMainModel, String> cAddress;
    public TableColumn<ProposalMainModel, String> cDate;
    public TableColumn<ProposalMainModel, String> colItems;
    public TableColumn<ProposalMainModel, String> cInvoice;
    public Pagination pagination;

    private DBConnection dbConnection;
    private CustomDialog customDialog;
    private ObservableList<ProposalMainModel> proposeList = FXCollections.observableArrayList();
    private FilteredList<ProposalMainModel> filteredData;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dbConnection = new DBConnection();
        customDialog = new CustomDialog();
        getProposalItem();

    }

    private void getProposalItem() {
        if (null != proposeList) {
            proposeList.clear();
        }

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = dbConnection.getConnection();
            if (null == connection) {
                return;
            }

            String query = "select pm.proposal_main_id ,tc.customer_name , tc.customer_phone , tc.customer_address, pm.invoice_num ,\n" +
                    "       (TO_CHAR(pm.proposal_date,'MM-DD-YYYY HH12:MI:SS AM') )as proposalDate\n" +
                    "     from proposal_main pm\n" +
                    "left join tbl_customer tc on pm.customer_id = tc.customer_id";

            ps = connection.prepareStatement(query);

            rs = ps.executeQuery();

            while (rs.next()) {

                int proposalMainId = rs.getInt("proposal_main_id");

                String cusName = rs.getString("customer_name");
                String cusPhone = rs.getString("customer_phone");
                String cusAddress = rs.getString("customer_address");
                String invoiceNum = rs.getString("invoice_num");
                String proposalDate = rs.getString("proposalDate");

                proposeList.add(new ProposalMainModel(proposalMainId, cusName, cusPhone, cusAddress, invoiceNum, proposalDate));
            }

            if (proposeList.size() > 0) {
                pagination.setVisible(true);
                search_Item();
            }


        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.closeConnection(connection, ps, rs);
        }
    }

    public void refresh(ActionEvent event) {
        getProposalItem();
        changeTableView(pagination.getCurrentPageIndex(), rowsPerPage);
    }

    private void search_Item() {

        filteredData = new FilteredList<>(proposeList, p -> true);

        searchTf.textProperty().addListener((observable, oldValue, newValue) -> {

            filteredData.setPredicate(proposal -> {

                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();

                if (proposal.getCustomerPhone().toLowerCase().contains(lowerCaseFilter)) {
                    return true;

                } else if (proposal.getInvoiceNumber().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else return proposal.getCustomerName().toLowerCase().contains(lowerCaseFilter);
            });

            changeTableView(pagination.getCurrentPageIndex(), rowsPerPage);
        });

        pagination.setCurrentPageIndex(0);
        changeTableView(0, rowsPerPage);
        pagination.currentPageIndexProperty().addListener(
                (observable1, oldValue1, newValue1) -> changeTableView(newValue1.intValue(), rowsPerPage));

    }

    private void changeTableView(int index, int limit) {
        if (null == filteredData) {
            return;
        }

        int totalPage = (int) (Math.ceil(filteredData.size() * 1.0 / rowsPerPage));
        pagination.setPageCount(totalPage);

        colSrNo.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(
                tableView.getItems().indexOf(cellData.getValue()) + 1));
        colCname.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("customerPhone"));
        cAddress.setCellValueFactory(new PropertyValueFactory<>("customerAddress"));
        cInvoice.setCellValueFactory(new PropertyValueFactory<>("invoiceNumber"));
        cDate.setCellValueFactory(new PropertyValueFactory<>("proposeDate"));

        setOptionalCell();

        int fromIndex = index * limit;
        int toIndex = Math.min(fromIndex + limit, proposeList.size());

        int minIndex = Math.min(toIndex, filteredData.size());
        SortedList<ProposalMainModel> sortedData = new SortedList<>(
                FXCollections.observableArrayList(filteredData.subList(Math.min(fromIndex, minIndex), minIndex)));
        sortedData.comparatorProperty().bind(tableView.comparatorProperty());

        tableView.setItems(sortedData);

    }

    private void setOptionalCell() {

        Callback<TableColumn<ProposalMainModel, String>, TableCell<ProposalMainModel, String>>
                cellBn = (TableColumn<ProposalMainModel, String> param) -> new TableCell<>() {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {


                    Label saleNow = new Label("ADD TO CART");
                    Label checkItem = new Label("CHECK ITEMS");
                    saleNow.setMinWidth(140);
                    checkItem.setMinWidth(140);

                    ImageView iv = new ImageView();
                    iv.setFitWidth(15);
                    iv.setFitHeight(15);
                    saleNow.setGraphicTextGap(7);
                    iv.setImage(new ImageLoader().load("img/menu_icon/sell_ic.png"));
                    saleNow.setGraphic(iv);
                    saleNow.setStyle("-fx-cursor: hand;-fx-padding: 5 10 5 10; -fx-background-color: #640303 ; " +

                            "-fx-font-family: 'Arial'; -fx-font-weight:bold;-fx-text-fill: white ;-fx-alignment: center; -fx-background-radius: 5");
                    checkItem.setStyle("-fx-cursor: hand;-fx-padding: 5 10 5 10; -fx-background-color: #0388fa ; " +
                            "-fx-font-family: 'Arial'; -fx-font-weight:bold;-fx-text-fill: white ;-fx-alignment: center; -fx-background-radius: 5");

                    saleNow.setOnMouseClicked(mouseEvent -> {

                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("Warning ");
                        alert.setHeaderText("ARE YOU SURE YOU WANT TO ADD TO CART ?");
                        alert.initModality(Modality.APPLICATION_MODAL);
                        alert.initOwner(Main.primaryStage);
                        Optional<ButtonType> result = alert.showAndWait();
                        ButtonType button = result.orElse(ButtonType.CANCEL);
                        if (button == ButtonType.OK) {
                            ProposalMainModel model = tableView.getSelectionModel().getSelectedItem();


                            Connection connection = null;
                            PreparedStatement ps = null;
                            ResultSet rs = null;

                            try {

                                connection = dbConnection.getConnection();

                                ps = connection.prepareStatement("select count(cart_id) from tbl_cart");
                                rs = ps.executeQuery();

                                while (rs.next()) {

                                    int count = rs.getInt(1);

                                    if (count < 1) {

                                        ps = null;
                                        rs = null;

                                        String query = "select pi.product_id , pi.stock_id , pi.price_type , pi.quantity_unit , pi.quantity , pi.sellprice  from proposal_items pi\n" +
                                                "left join proposal_main pm on pi.proposal_main_id = pm.proposal_main_id where pi.proposal_main_id = ?";

                                        ps = connection.prepareStatement(query);
                                        ps.setInt(1, model.getProposalMainId());

                                        rs = ps.executeQuery();

                                        int res = 0;

                                        while (rs.next()) {
                                            res = 0;

                                            int productId = rs.getInt("product_id");
                                            int stockId = rs.getInt("stock_id");
                                            int quantity = rs.getInt("quantity");
                                            String qtyUnit = rs.getString("quantity_unit");
                                            String priceType = rs.getString("price_type");
                                            double rate = rs.getDouble("sellprice");

                                            ps = connection.prepareStatement(new PropertiesLoader().getInsertProp().getProperty("INSERT_CART_DETAILS"));
                                            ps.setInt(1, productId);
                                            ps.setInt(2, Login.currentlyLogin_Id);
                                            ps.setDouble(3, rate);
                                            ps.setInt(4, stockId);
                                            ps.setDouble(5, quantity);
                                            ps.setString(6, qtyUnit);
                                            ps.setString(7, priceType);

                                            res = ps.executeUpdate();
                                        }

                                        if (res > 0) {
                                            Alert alert1 = new Alert(Alert.AlertType.INFORMATION);
                                            alert1.setTitle("success");
                                            alert1.setHeaderText("ITEMS SUCCESSFULLY ADDED IN CART");
                                            alert1.setContentText("PLEASE GO TO CART AND SALE THE ITEM");
                                            alert1.initOwner(Main.primaryStage);
                                            alert1.showAndWait();
                                            deleteProposalItem(model.getProposalMainId());
                                            viewCart();
                                        }

                                    } else {

                                        String msg = "YOU CAN'T ADD TO THE CART BECAUSE OTHER PRODUCTS ALREADY EXIST! \n " +
                                                "PLEASE GO ON THE CART.   AND SELL THE PRODUCT. OR CLEAR THE PRODUCT !";

                                        customDialog.showAlertBox("Failed!", msg);

                                    }
                                }


                            } catch (SQLException e) {
                                e.printStackTrace();
                            } finally {

                                DBConnection.closeConnection(connection, ps, rs);
                            }


                        } else {
                            alert.close();
                        }
                    });

                    checkItem.setOnMouseClicked(mouseEvent -> {
                        ProposalMainModel proposalMainModel = tableView.getSelectionModel().getSelectedItem();
                        Main.primaryStage.setUserData(proposalMainModel);
                        customDialog.showFxmlFullDialog("proposal/proposalItems.fxml", "ITEMS");
                    });


                    VBox managebtn = new VBox(saleNow, checkItem);

                    managebtn.setStyle("-fx-alignment:center");
                    VBox.setMargin(checkItem, new Insets(10, 10, 10, 15));
                    setGraphic(managebtn);

                    setText(null);

                }
            }

        };


        Callback<TableColumn<ProposalMainModel, String>, TableCell<ProposalMainModel, String>>
                cellAction = (TableColumn<ProposalMainModel, String> param) -> new TableCell<>() {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {

                    ImageView ivDelete = new ImageView();
                    ivDelete.setFitWidth(20);
                    ivDelete.setFitHeight(20);
                    ivDelete.setImage(new ImageLoader().load("img/icon/delete_ic.png"));


                    Label bnPrint = new Label("PRINT");


                    bnPrint.setMinWidth(60);
                    bnPrint.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);


                    bnPrint.setStyle("-fx-background-color: #670283; -fx-background-radius: 3 ;-fx-font-weight: bold ;-fx-font-family: Arial; " +
                            "-fx-padding: 4 11 4 11 ; -fx-text-fill: white; -fx-alignment: center;-fx-cursor: hand");
                    ImageView print_iv = new ImageView();

                    String path = "img/icon/";

                    print_iv.setFitHeight(18);
                    print_iv.setFitWidth(18);

                    ImageLoader loader = new ImageLoader();

                    print_iv.setImage(loader.load(path.concat("print_ic.png")));

                    bnPrint.setGraphic(print_iv);

                    bnPrint.setOnMouseClicked(mouseEvent -> {
                        new GenerateInvoice().proposalInvoice(tableView.getSelectionModel().getSelectedItem().getProposalMainId());
                    });


                    ivDelete.setStyle("-fx-cursor: hand");

                    ivDelete.setOnMouseClicked(mouseEvent -> {
                        ProposalMainModel pim = tableView.getSelectionModel().getSelectedItem();
                        Alert alertD = new Alert(Alert.AlertType.CONFIRMATION);
                        alertD.setTitle("Warning ");
                        alertD.setHeaderText("ARE YOU SURE YOU WANT TO DELETE ?");
                        alertD.initModality(Modality.APPLICATION_MODAL);
                        alertD.initOwner(Main.primaryStage);
                        Optional<ButtonType> resultD = alertD.showAndWait();
                        ButtonType buttonD = resultD.orElse(ButtonType.CANCEL);
                        if (buttonD == ButtonType.OK) {
                            deleteProposalItem(pim.getProposalMainId());
                        } else {
                            alertD.close();
                        }

                    });


                    HBox managebtn = new HBox(ivDelete, bnPrint);

                    managebtn.setStyle("-fx-alignment:center");
                    HBox.setMargin(bnPrint, new Insets(10, 10, 10, 15));
                    HBox.setMargin(ivDelete, new Insets(0, 0, 0, 30));
                    setGraphic(managebtn);

                    setText(null);

                }
            }

        };

        colItems.setCellFactory(cellBn);
        colAction.setCellFactory(cellAction);
        customColumn(cAddress);
        customColumn(cDate);
    }

    private void deleteProposalItem(int proposalMainId) {

        String proposalItemDeleteQuery = "DELETE FROM proposal_items WHERE proposal_main_id = " + proposalMainId;
        String proposalMainDeleteQuery = "DELETE FROM proposal_main WHERE proposal_main_id = " + proposalMainId;

        Connection connection = null;
        Statement statement = null;

        try {

            connection = dbConnection.getConnection();
            if (null == connection) {
                return;
            }

            statement = connection.createStatement();

            statement.addBatch(proposalItemDeleteQuery);
            statement.addBatch(proposalMainDeleteQuery);

            int[] res = statement.executeBatch();

            if (res[0] > 0) {
                refresh(null);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {

            DBConnection.closeConnection(connection, null, null);

            try {
                if (null != statement) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void customColumn(TableColumn<ProposalMainModel, String> columnName) {

        columnName.setCellFactory(tc -> {
            TableCell<ProposalMainModel, String> cell = new TableCell<>();
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

    public void viewCart(ActionEvent event) {

        viewCart();
    }

    private void viewCart() {

        try {
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
    }
}
