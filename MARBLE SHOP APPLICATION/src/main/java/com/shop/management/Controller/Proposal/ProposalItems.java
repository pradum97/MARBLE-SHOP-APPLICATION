package com.shop.management.Controller.Proposal;

import com.shop.management.CustomDialog;
import com.shop.management.ImageLoader;
import com.shop.management.Main;
import com.shop.management.Model.ProposalItemModel;
import com.shop.management.Model.ProposalMainModel;
import com.shop.management.util.DBConnection;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.util.Callback;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

public class ProposalItems implements Initializable {
    public Label cusNameL;
    public Label cusPhoneL;
    public Label dateL;
    public Label cusAddressL;
    public Label invoiceNumL;
    public TableView<ProposalItemModel> tableView;
    public TableColumn<ProposalItemModel,String> colAction;
    public TableColumn <ProposalItemModel , Integer> colSrNo;
    public TableColumn <ProposalItemModel , String> colProductName;
    public TableColumn <ProposalItemModel , String> colProductType;
    public TableColumn <ProposalItemModel , String> colProductCategory;
    public TableColumn <ProposalItemModel , String> colProductSize;
    public TableColumn <ProposalItemModel , String> colProductQuantity;
    public TableColumn <ProposalItemModel , String> colRate;
    public TableColumn <ProposalItemModel , String> colDiscAmount;
    private ProposalMainModel proposalModel;

    private DBConnection dbConnection;
    private CustomDialog customDialog;
    private ObservableList<ProposalItemModel> proposeList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dbConnection = new DBConnection();
        customDialog = new CustomDialog();
        proposalModel = (ProposalMainModel) Main.primaryStage.getUserData();

        if (null == proposalModel){
            customDialog.showAlertBox("...","Item Not Available");
            return;
        }

        getProposalItem(proposalModel.getProposalMainId());
        setDefaultValue();
    }

    private void setDefaultValue() {
        cusNameL.setText(proposalModel.getCustomerName());
        cusPhoneL.setText(proposalModel.getCustomerPhone());
        cusAddressL.setText(proposalModel.getCustomerAddress());
        dateL.setText(proposalModel.getProposeDate());
        invoiceNumL.setText(proposalModel.getInvoiceNumber());
    }

    private void getProposalItem(int proposalMainId) {
        if (null != proposeList){
            proposeList.clear();
        }

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = dbConnection.getConnection();
            if (null == connection){
                return;
            }

            String query = "select tp.product_name , tp.product_type ,t.category_name, pi.proposal_items_id , pi.sellprice , pi.quantity , (concat(pi.quantity ,' -',pi.quantity_unit))as fullQuantity,\n" +
                    "       ((pi.sellprice * pi.quantity)*td.discount/100)as discountAmount ,\n" +
                    "       (concat(tps.height,'x',tps.width,' ',tps.size_unit)) as product_size\n" +
                    "      from proposal_items pi\n" +
                    "left join proposal_main pm on pm.proposal_main_id = pi.proposal_main_id\n" +
                    " left join tbl_product_stock tps on pi.stock_id = tps.stock_id\n" +
                    " left join tbl_products tp on tps.product_id = tp.product_id\n" +
                    "left outer join tbl_category t on tp.category_id = t.category_id\n" +
                    "  left join tbl_discount td on tp.discount_id = td.discount_id where pm.proposal_main_id = ?";

            ps = connection.prepareStatement(query);
            ps.setInt(1,proposalMainId);

            rs = ps.executeQuery();

            while (rs.next()){

               int proposalItemId = rs.getInt("proposal_items_id");

                String pName = rs.getString("product_name");
                String pType = rs.getString("product_type");
                String pCategory = rs.getString("category_name");
                String pQuantity = rs.getString("fullQuantity");
                String rate = rs.getString("sellprice");
                String pSize = rs.getString("product_size");
                double discAmount = rs.getDouble("discountAmount");

                proposeList.add(new ProposalItemModel(proposalItemId , pName , pType , pCategory , pSize , pQuantity , rate , Math.round(discAmount)));
            }
            colSrNo.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(
                    tableView.getItems().indexOf(cellData.getValue()) + 1));
            colProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
            colProductType.setCellValueFactory(new PropertyValueFactory<>("productType"));
            colProductCategory.setCellValueFactory(new PropertyValueFactory<>("productCategory"));
            colProductSize.setCellValueFactory(new PropertyValueFactory<>("productSize"));
            colProductQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
            colRate.setCellValueFactory(new PropertyValueFactory<>("rate"));
            colDiscAmount.setCellValueFactory(new PropertyValueFactory<>("discountAmount"));

            setOptionalCell();
            if (null != proposeList){
                tableView.setItems(proposeList);
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            DBConnection.closeConnection(connection , ps , rs);
        }
    }

    private void setOptionalCell() {

        Callback<TableColumn<ProposalItemModel, String>, TableCell<ProposalItemModel, String>>
                cellBn = (TableColumn<ProposalItemModel, String> param) -> new TableCell<>() {
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

                    ivDelete.setStyle("-fx-cursor: hand");

                    ivDelete.setOnMouseClicked(mouseEvent -> deleteProduct(tableView.getSelectionModel().getSelectedItem()));

                    HBox managebtn = new HBox(ivDelete);
                    managebtn.setStyle("-fx-alignment:center");
                    HBox.setMargin(ivDelete, new Insets(0, 0, 0, 20));
                    setGraphic(managebtn);
                    setText(null);

                }
            }

        };

        colAction.setCellFactory(cellBn);
        customColumn(colProductName);
    }

    private void customColumn(TableColumn<ProposalItemModel, String> columnName) {

        columnName.setCellFactory(tc -> {
            TableCell<ProposalItemModel, String> cell = new TableCell<>();
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
    private void deleteProduct(ProposalItemModel pim) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Warning ");
        alert.setHeaderText("ARE YOU SURE YOU WANT TO DELETE THIS PRODUCT ( " + pim.getProductName() + " )  ?");
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

                ps = con.prepareStatement("DELETE FROM proposal_items WHERE proposal_items_id = ?");
                ps.setInt(1, pim.getProposalItemId());

                int res = ps.executeUpdate();

                if (res > 0) {

                    customDialog.showAlertBox("", "Successfully Deleted");
                    alert.close();
                   getProposalItem(proposalModel.getProposalMainId());

                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {

                DBConnection.closeConnection(con, ps, null);
            }

        } else {
            alert.close();
        }
    }

}
