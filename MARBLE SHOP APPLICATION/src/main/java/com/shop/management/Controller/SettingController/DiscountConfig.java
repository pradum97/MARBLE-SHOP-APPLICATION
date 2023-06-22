package com.shop.management.Controller.SettingController;

import com.shop.management.Controller.Login;
import com.shop.management.CustomDialog;
import com.shop.management.ImageLoader;
import com.shop.management.Main;
import com.shop.management.Method.GetDiscount;
import com.shop.management.Method.Method;
import com.shop.management.Model.Discount;
import com.shop.management.util.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.util.Callback;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

public class DiscountConfig implements Initializable {

    int rowsPerPage = 10;

    public TableView<Discount> tableViewDiscount;
    public TableColumn<Discount, String> colDiscount;
    public TableColumn<Discount, String> colDisAction;
    public TableColumn<Discount, String> colDiscountDes;
    public TableColumn<Discount, String> colDiscountName;
    public Pagination pagination;
    private Method method;
    private DBConnection dbConnection;
    private CustomDialog customDialog;
    private ObservableList<Discount> discountList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        method = new Method();
        dbConnection = new DBConnection();
        customDialog = new CustomDialog();
        setDiscountData();

    }

    private void changeTableView(int index, int limit) {

        int totalPage = (int) (Math.ceil(discountList.size() * 1.0 / rowsPerPage));
        pagination.setPageCount(totalPage);


        colDiscountName.setCellValueFactory(new PropertyValueFactory<>("discountName"));
        colDiscount.setCellValueFactory(new PropertyValueFactory<>("discount"));
        colDiscountDes.setCellValueFactory(new PropertyValueFactory<>("description"));

        Callback<TableColumn<Discount, String>, TableCell<Discount, String>>
                cellFactory = (TableColumn<Discount, String> param) -> new TableCell<>() {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);

                } else {
                    ImageLoader imageLoader = new ImageLoader();
                    ImageView iv_edit = new ImageView(imageLoader.load("img/icon/edit_ic.png"));
                    iv_edit.setFitHeight(22);
                    iv_edit.setFitHeight(22);
                    iv_edit.setPreserveRatio(true);

                    ImageView iv_delete = new ImageView(imageLoader.load("img/icon/delete_ic.png"));
                    iv_delete.setFitHeight(17);
                    iv_delete.setFitWidth(17);
                    iv_delete.setPreserveRatio(true);
                    iv_delete.managedProperty().bind(iv_delete.visibleProperty());
                    iv_delete.setVisible(Objects.equals(Login.currentRoleName.toLowerCase(), "admin".toLowerCase()));

                    iv_edit.setStyle(
                            " -fx-cursor: hand ;"
                                    + "-glyph-size:28px;"
                                    + "-fx-fill:#c506fa;"
                    );

                    iv_delete.setStyle(
                            " -fx-cursor: hand ;"
                                    + "-glyph-size:28px;"
                                    + "-fx-fill:#ff0000;"
                    );
                    iv_edit.setOnMouseClicked((MouseEvent event) -> {

                        Discount edit_selection = tableViewDiscount.
                                getSelectionModel().getSelectedItem();

                        if (null == edit_selection) {
                            method.show_popup("Please Select", tableViewDiscount);
                            return;
                        }

                        Main.primaryStage.setUserData(edit_selection);

                        customDialog.showFxmlDialog("setting/update/discountUpdate.fxml", "UPDATE DISCOUNT");
                        setDiscountData();

                    });

                    iv_delete.setOnMouseClicked((MouseEvent event) -> {


                        Discount delete_selection = tableViewDiscount.
                                getSelectionModel().getSelectedItem();

                        if (null == delete_selection) {
                            method.show_popup("Please Select ", tableViewDiscount);
                            return;
                        }

                        deleteDiscount(delete_selection);

                    });

                    HBox managebtn = new HBox(iv_edit, iv_delete);

                    managebtn.setStyle("-fx-alignment:center");
                    HBox.setMargin(iv_edit, new Insets(2, 2, 0, 3));
                    HBox.setMargin(iv_delete, new Insets(2, 3, 0, 20));

                    setGraphic(managebtn);

                    setText(null);

                }
            }

        };


        colDisAction.setCellFactory(cellFactory);
        customColumn(colDiscountName);

        int fromIndex = index * limit;
        int toIndex = Math.min(fromIndex + limit, discountList.size());

        int minIndex = Math.min(toIndex, discountList.size());
        SortedList<Discount> sortedData = new SortedList<>(
                FXCollections.observableArrayList(discountList.subList(Math.min(fromIndex, minIndex), minIndex)));
        sortedData.comparatorProperty().bind(tableViewDiscount.comparatorProperty());

        tableViewDiscount.setItems(sortedData);

    }

    private void customColumn(TableColumn<Discount, String> columnName) {

        columnName.setCellFactory(tc -> {
            TableCell<Discount, String> cell = new TableCell<>();
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

    public void addDiscountBN(ActionEvent event) {

        customDialog.showFxmlDialog("setting/addDiscount.fxml", "ADD NEW DISCOUNT");
        setDiscountData();
    }

    private void setDiscountData() {
        if (null != discountList) {
            discountList.clear();
        }
        discountList = new GetDiscount().get();

        if (null == discountList) {
            return;
        }
        double tableMinHeight = 70;

        for (int i = 0; i < discountList.size(); i++) {

            tableMinHeight = tableMinHeight + 20 + i;

            tableViewDiscount.setMinHeight(tableMinHeight);

        }

        if (discountList.size() > 0) {
            pagination.setVisible(true);
        }

        changeTableView(pagination.getCurrentPageIndex(), rowsPerPage);

        pagination.setCurrentPageIndex(0);
        changeTableView(0, rowsPerPage);
        pagination.currentPageIndexProperty().addListener(
                (observable1, oldValue1, newValue1) -> changeTableView(newValue1.intValue(), rowsPerPage));

    }

    private void deleteDiscount(Discount discount) {

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setAlertType(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Warning ");
        alert.setHeaderText("Are You Sure You Want to Delete This DISCOUNT ( " + discount.getDiscount() + " )");
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.initOwner(Main.primaryStage);
        Optional<ButtonType> result = alert.showAndWait();
        ButtonType button = result.orElse(ButtonType.CANCEL);
        if (button == ButtonType.OK) {
            Connection con = null;
            PreparedStatement ps = null;

            try {

                con = dbConnection.getConnection();

                if (null == con) {
                    return;
                }

                ps = con.prepareStatement("DELETE FROM TBL_DISCOUNT WHERE DISCOUNT_ID = ?");
                ps.setInt(1, discount.getDiscount_id());

                int res = ps.executeUpdate();

                if (res > 0) {
                    setDiscountData();
                    alert.close();
                }

            } catch (SQLException e) {
                customDialog.showAlertBox("ERROR", "You cannot remove this Discount because this Discount has been used in your products.");
                e.printStackTrace();
            } finally {

                try {

                    if (null != con) {
                        con.close();
                    }
                    if (null != ps) {
                        ps.close();
                    }


                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

        } else {
            alert.close();
        }
    }
}
