package com.shop.management.Controller;

import com.shop.management.CustomDialog;
import com.shop.management.Dashboard;
import com.shop.management.ImageLoader;
import com.shop.management.Method.Method;
import com.shop.management.Method.StaticData;
import com.shop.management.Model.CategoryModel;
import com.shop.management.Model.Discount;
import com.shop.management.Model.ProductSize;
import com.shop.management.Model.TAX;
import com.shop.management.PropertiesLoader;
import com.shop.management.util.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class AddProducts implements Initializable {
    public TableView<ProductSize> sizeTableView;
    public TextField productName;
    public TextField productPurchasePrice;
    public TextField productMrp;
    public TextField productMinSellPrice;
    public ComboBox<Discount> productDiscount;
    public ComboBox<String> productColor;
    public ComboBox<String> productType;
    public ComboBox<CategoryModel> productCategory;
    public TextField productHeight;
    public TextField productWidth;
    public ComboBox<String> productSizeUnit;
    public TextField productQuantity;
    public ComboBox<String> productQuantityUnit;
    public Button bnAddSize;
    public TableColumn<ProductSize, String> col_Height;
    public TableColumn<ProductSize, String> col_Width;
    public TableColumn<ProductSize, String> col_Quantity;
    public TableColumn<ProductSize, String> col_action;
    public TableColumn<ProductSize, String> col_SizeUit;
    public TableColumn<ProductSize, String> col_QuantityUnit;
    public TextArea productDescription;
    public Button bnSubmit;
    public ComboBox<TAX> productTax;
    public TableColumn<ProductSize, String> col_PurchasePrice;
    public TableColumn<ProductSize, String> col_mrp;
    public TableColumn<ProductSize, String> col_minSelPrice;
    public TableColumn<ProductSize, String> colPriceType;
    public TableColumn<ProductSize, String> colPcsPerPkt;
    public TextField productCodeTF;
    private static final  String REGEX = "[^0-9.]";
    public ComboBox<String> priceTypeC;
    public ComboBox<Integer> pcsPerPacket;


    double tableViewSize = 70;
    private Method method;
    private CustomDialog customDialog;
    private DBConnection dbConnection;

    ObservableList<ProductSize> sizeList = FXCollections.observableArrayList();
    ObservableList<Discount> discountList = FXCollections.observableArrayList();
    ObservableList<TAX> taxList = FXCollections.observableArrayList();
    private ObservableList<CategoryModel> categoryList = FXCollections.observableArrayList();

    public static double profitPercentage = 20; // in %

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        sizeTableView.setPlaceholder(new Label("Size Not Available"));
        method = new Method();
        customDialog = new CustomDialog();
        dbConnection = new DBConnection();

        StaticData sd = new StaticData();
        pcsPerPacket.setItems(sd.getPcsPerPacketList());
        pcsPerPacket.getSelectionModel().selectFirst();
        priceTypeC.setItems(sd.getSizeQuantityUnit());

        setComboBoxData();
        textChangeListener();

        productDiscount.setOnMouseClicked(mouseEvent -> {
           if (discountList.isEmpty()){
               setDiscount();
           }
        });
        productTax.setOnMouseClicked(mouseEvent -> {
           if (taxList.isEmpty()){
               setTax();
           }
        });

        productCategory.setOnMouseClicked(mouseEvent -> {
            if (categoryList.isEmpty()){
                getCategory();
            }
        });
    }
    private void textChangeListener() {

        productDiscount.setOnMouseClicked(event -> {

            productDiscount.getSelectionModel().clearSelection();
            productDiscount.setPromptText("Not Applicable");

        });

        productTax.setOnMouseClicked(event -> {
                    productTax.getSelectionModel().clearSelection();
                    productTax.setPromptText("Not Applicable");
                }

        );

        productPurchasePrice.textProperty().addListener((observableValue, old, newValue) -> {

            double purchasePrice, minPrice = 0;

            try {
                purchasePrice = Double.parseDouble(productPurchasePrice.getText().replaceAll(REGEX, ""));

            } catch (NumberFormatException e) {
                productMinSellPrice.setText("");
                return;
            }

            minPrice = purchasePrice + (profitPercentage * purchasePrice / 100);

            BigDecimal minSell_price = BigDecimal.valueOf(minPrice);

            productMinSellPrice.setText(minSell_price.stripTrailingZeros().toPlainString());

        });
    }
    private void setComboBoxData() {

        productColor.setOnMouseClicked(mouseEvent -> {
            productColor.setItems(method.getProductColor());
        });

        productType.setItems(method.getProductType());
        productSizeUnit.setItems(method.getSizeUnit());
        productQuantityUnit.setItems(method.getSizeQuantityUnit());


    }
    public void submit_bn(ActionEvent event) {
        submit();
    }

    public void bnAddSize(ActionEvent event) {

        String heightS = productHeight.getText();
        String widthS = productWidth.getText();
        String quantityS = productQuantity.getText();
        String purchasePrice = productPurchasePrice.getText();
        String prodMrp = productMrp.getText();
        String minSellPrice = productMinSellPrice.getText();
        double mrp = 0, min_Sell_Price = 0, purchase_price = 0;

        if (purchasePrice.isEmpty()) {
            method.show_popup("ENTER PURCHASE PRICE ", productPurchasePrice);
            return;
        }
        try {
            purchase_price = Double.parseDouble(purchasePrice.replaceAll(REGEX, ""));
        } catch (NumberFormatException e) {
            customDialog.showAlertBox("INVALID PURCHASE PRICE", "ENTER VALID PURCHASE PRICE");
            e.printStackTrace();
            return;
        }

        if (prodMrp.isEmpty()) {
            method.show_popup("ENTER PRODUCT MRP ", productMrp);
            return;
        }
        try {
            mrp = Double.parseDouble(prodMrp.replaceAll(REGEX, ""));
        } catch (NumberFormatException e) {
            customDialog.showAlertBox("INVALID MRP", "ENTER VALID MRP");
            e.printStackTrace();
            return;
        }
        if (purchase_price > mrp) {
            method.show_popup("ENTER MRP MORE THAN PURCHASE PRICE", productMrp);
            return;
        } else if (minSellPrice.isEmpty()) {
            method.show_popup("ENTER MIN SELLING PRICE ", productMinSellPrice);
            return;
        }
        try {
            min_Sell_Price = Double.parseDouble(minSellPrice.replaceAll(REGEX, ""));

        } catch (NumberFormatException e) {
            customDialog.showAlertBox("INVALID MIN SELL PRICE", "ENTER VALID INVALID MIN SELL PRICE");
            e.printStackTrace();
            return;
        }

        if (priceTypeC.getSelectionModel().isEmpty()){
            method.show_popup("PLEASE SELECT PRICE TYPE", priceTypeC);
            return;
        } else if (pcsPerPacket.getSelectionModel().isEmpty()){
            method.show_popup("PLEASE SELECT PCS PER PACKET", pcsPerPacket);
            return;
        }
        if (purchase_price > min_Sell_Price) {
            method.show_popup("ENTER MINIMUM SELLING PRICE MORE THAN PURCHASE PRICE", productMinSellPrice);
            return;
        } else if (min_Sell_Price > mrp) {
            method.show_popup("ENTER MINIMUM SELLING PRICE LESS THAN MRP", productMinSellPrice);
            return;

        }
        else if (heightS.isEmpty()) {
            method.show_popup("ENTER PRODUCT HEIGHT", productHeight);
            return;
        } else if (widthS.isEmpty()) {
            method.show_popup("ENTER PRODUCT WIDTH", productWidth);
            return;
        } else if (null == productSizeUnit.getValue()) {
            method.show_popup("CHOOSE SIZE UNIT", productSizeUnit);
            return;

        } else if (quantityS.isEmpty()) {

            method.show_popup("ENTER PRODUCT QUANTITY", productQuantity);
            return;
        } else if (null == productQuantityUnit.getValue()) {

            method.show_popup("CHOOSE QUANTITY UNIT", productQuantityUnit);
            return;
        }

        double height = 0, width = 0;
        long quantity = 0;

        try {
            height = Double.parseDouble(heightS.replaceAll(REGEX, ""));
            width = Double.parseDouble(widthS.replaceAll(REGEX, ""));

        } catch (NumberFormatException e) {
            customDialog.showAlertBox("INVALID PRODUCT SIZE", "ENTER VALID HEIGHT AND WIDTH ");
            return;
        }

        try {
            quantity = Long.parseLong(quantityS.replaceAll(REGEX, ""));
        } catch (NumberFormatException e) {
            customDialog.showAlertBox("INVALID QUANTITY", "ENTER VALID QUANTITY");
            e.printStackTrace();
        }
        String sizeUnit = productSizeUnit.getValue();
        String quantityUnit = productQuantityUnit.getValue();

        String priceTypeS = priceTypeC.getSelectionModel().getSelectedItem();
        int pcsPerPkt = pcsPerPacket.getSelectionModel().getSelectedItem();

        ProductSize productSize = new ProductSize(purchase_price, mrp, min_Sell_Price, width,
                height, quantity, sizeUnit, quantityUnit , priceTypeS , pcsPerPkt);
        sizeList.add(productSize);

        sizeTableView.setItems(sizeList);

        tableViewSize = tableViewSize + 20;
        sizeTableView.setMinHeight(tableViewSize);

        col_PurchasePrice.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));
        col_mrp.setCellValueFactory(new PropertyValueFactory<>("productMRP"));
        col_minSelPrice.setCellValueFactory(new PropertyValueFactory<>("MinSellPrice"));
        col_Height.setCellValueFactory(new PropertyValueFactory<>("height"));
        col_Width.setCellValueFactory(new PropertyValueFactory<>("width"));
        col_Quantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        col_SizeUit.setCellValueFactory(new PropertyValueFactory<>("sizeUnit"));
        col_QuantityUnit.setCellValueFactory(new PropertyValueFactory<>("quantityUnit"));
        colPriceType.setCellValueFactory(new PropertyValueFactory<>("priceType"));
        colPcsPerPkt.setCellValueFactory(new PropertyValueFactory<>("pcsPerPkt"));


        Callback<TableColumn<ProductSize, String>, TableCell<ProductSize, String>>
                cellFactory = (TableColumn<ProductSize, String> param) -> new TableCell<>() {
                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                            setText(null);

                        } else {

                            String path = "img/icon/delete_ic.png";
                            ImageView iv_delete = new ImageView(new ImageLoader().load(path));
                            iv_delete.setFitHeight(17);
                            iv_delete.setFitWidth(17);
                            iv_delete.setPreserveRatio(true);

                            iv_delete.setStyle(
                                    " -fx-cursor: hand ;"
                                            + "-glyph-size:28px;"
                                            + "-fx-fill:#ff0000;"
                            );

                            iv_delete.setOnMouseClicked((MouseEvent event) -> {


                                ProductSize ps1 = sizeTableView.getSelectionModel().getSelectedItem();

                                sizeTableView.getItems().remove(ps1);

                                tableViewSize = tableViewSize - 20;


                                sizeTableView.setMinHeight(tableViewSize);

                            });

                            HBox managebtn = new HBox(iv_delete);

                            managebtn.setStyle("-fx-alignment:center");
                            HBox.setMargin(iv_delete, new Insets(2, 3, 0, 20));

                            setGraphic(managebtn);

                            setText(null);

                        }
                    }

                };

        col_action.setCellFactory(cellFactory);

        productPurchasePrice.setText("");
        productMrp.setText("");
        productMinSellPrice.setText("");

        productHeight.setText("");
        productWidth.setText("");


        productSizeUnit.getSelectionModel().clearSelection();
        productQuantity.setText("");
        productQuantityUnit.getSelectionModel().clearSelection();
    }

    private void getCategory(){
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        if (null != categoryList){
            categoryList.clear();
        }

        try {
            connection = dbConnection.getConnection();

            if (null == connection) {
                return;
            }

            ps = connection.prepareStatement("SELECT * FROM tbl_category");

            rs = ps.executeQuery();

            while (rs.next()) {
                int categoryId = rs.getInt("category_id");
                String cName = rs.getString("category_name");

                categoryList.add(new CategoryModel(categoryId, cName));

            }
            productCategory.setItems(categoryList);

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.closeConnection(connection, ps, rs);
        }
    }
    private void setDiscount() {

        if (null != discountList) {
            discountList.clear();
        }

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = dbConnection.getConnection();
            ps = connection.prepareStatement(new PropertiesLoader().getReadProp().getProperty("GET_DISCOUNT"));
            rs = ps.executeQuery();

            while (rs.next()) {
                int discountID = rs.getInt("discount_id");
                int discount = rs.getInt("discount");
                String description = rs.getString("description");
                String discountName = rs.getString("discount_name");
                discountList.addAll(new Discount(discountID, discountName, discount, description));

            }
            productDiscount.setItems(discountList);

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnection.closeConnection(connection, ps, rs);
        }
    }

    private void setTax() {

        if (null != taxList) {
            taxList.clear();
        }

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = dbConnection.getConnection();

            ps = connection.prepareStatement(new PropertiesLoader().getReadProp().getProperty("GET_TAX"));
            rs = ps.executeQuery();

            while (rs.next()) {

                int hsn_sac = rs.getInt("hsn_sac");
                int taxID = rs.getInt("tax_id");
                int sgst = rs.getInt("sgst");
                int cgst = rs.getInt("cgst");
                int igst = rs.getInt("igst");
                String tax_description = rs.getString("description");
                String gstName = rs.getString("gstName");

                String gn;
                if (gstName == null) {
                    gn = "";
                } else {
                    gn = gstName;
                }

                taxList.add(new TAX(taxID, hsn_sac, sgst, cgst, igst, gn, tax_description));
            }

            productTax.setItems(taxList);

        } catch (SQLException e) {
            e.printStackTrace();

        } finally {

            DBConnection.closeConnection(connection, ps, rs);
        }
    }

    public void enterPress(KeyEvent e) {

        if (e.getCode() == KeyCode.ENTER) {

            submit();
        }
    }

    private boolean isExist(String value) {

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {

            connection = dbConnection.getConnection();
            String query = "select PRODUCT_CODE from TBL_PRODUCTS where product_code = ?";

            ps = connection.prepareStatement(query);
            ps.setString(1, value);

            rs = ps.executeQuery();

            return rs.next();

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            DBConnection.closeConnection(connection, ps, rs);
        }
    }

    private void submit() {
        // textField
        String prodName = productName.getText();
        String prodCode = productCodeTF.getText().replaceAll(" ", "");
        String prodDescription = productDescription.getText();

        Discount discount = productDiscount.getSelectionModel().getSelectedItem();
        TAX tax = productTax.getSelectionModel().getSelectedItem();

        ObservableList<ProductSize> tableData = sizeTableView.getItems();


        if (prodName.isEmpty()) {
            method.show_popup("ENTER PRODUCT NAME ", productName);
            return;
        } else if (prodCode.isEmpty()) {
            method.show_popup("Enter Product Code", productCodeTF);
            return;
        } else if (isExist(prodCode)) {
            method.show_popup("PRODUCT CODE ALREADY EXIST", productCodeTF);
            return;
        } else if (productTax.getSelectionModel().isEmpty()) {
            method.show_popup("SELECT HSN CODE", productTax);
            return;
        } else if (null == productCategory.getValue()) {
            method.show_popup("CHOOSE PRODUCT CATEGORY", productCategory);
            return;
        } else if (null == productColor.getValue()) {
            method.show_popup("CHOOSE PRODUCT COLOR ", productColor);
            return;
        } else if (null == productType.getValue()) {
            method.show_popup("CHOOSE PRODUCT TYPE ", productType);
            return;
        } else if (tableData.isEmpty()) {
            method.show_popup("ADD AT LEAST 1 SIZE", bnAddSize);
            return;
        }
        String prodColor = productColor.getValue();
        String prodType = productType.getValue();
        int categoryId = productCategory.getSelectionModel().getSelectedItem().getCategoryId();


        Connection connection;
        PreparedStatement ps;
        ResultSet rs;

        try {
            connection = dbConnection.getConnection();
            if (null == connection) {
                return;
            }

            String productQuery = "INSERT INTO TBL_PRODUCTS(PRODUCT_NAME, PRODUCT_DESCRIPTION, PRODUCT_COLOR, PRODUCT_TYPE,\n" +
                    "                         CATEGORY_ID, DISCOUNT_ID, TAX_ID,PRODUCT_CODE)\n" +
                    " VALUES(?,?,?,?,?,?,?,?)";

            ps = connection.prepareStatement(productQuery, new String[]{"product_id"});
            ps.setString(1, prodName);
            ps.setString(2, prodDescription);
            ps.setString(3, prodColor);
            ps.setString(4, prodType);
            ps.setInt(5, categoryId);

            if (null == discount) {
                ps.setNull(6, Types.NULL);
            } else {
                ps.setInt(6, discount.getDiscount_id()); // dis
            }
            if (null == tax) {
                ps.setNull(7, Types.NULL);
            } else {
                ps.setInt(7, tax.getTaxID()); //tax
            }
            ps.setString(8, prodCode.replaceAll(" ", ""));
            int productResult = ps.executeUpdate();

            if (productResult > 0) {

                rs = ps.getGeneratedKeys();

                if (rs.next()) {

                    int productID = rs.getInt(1);
                    ps = null;

                    String stockQuery = "INSERT INTO TBL_PRODUCT_STOCK (PURCHASE_PRICE, PRODUCT_MRP, min_sellingPrice, PRODUCT_ID, HEIGHT," +
                            " WIDTH, QUANTITY, SIZE_UNIT, QUANTITY_UNIT , PRICE_TYPE , PCS_PER_PACKET) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
                    ps = connection.prepareStatement(stockQuery);

                    for (ProductSize pz : tableData) {

                        long qty ;

                        if (pz.getQuantityUnit().equals("PKT")){
                            qty = (pz.getQuantity()*pz.getPcsPerPkt());
                        }else {
                            qty = pz.getQuantity();
                        }
                        ps.setDouble(1, pz.getPurchasePrice());
                        ps.setDouble(2, pz.getProductMRP());
                        ps.setDouble(3, pz.getMinSellPrice());
                        ps.setInt(4, productID);
                        ps.setDouble(5, pz.getHeight());
                        ps.setDouble(6, pz.getWidth());
                        ps.setDouble(7, qty);
                        ps.setString(8, pz.getSizeUnit());
                        ps.setString(9, "PCS");
                        ps.setString(10, pz.getPriceType());
                        ps.setInt(11, pz.getPcsPerPkt());
                        int stockRes = ps.executeUpdate();

                        if (stockRes < 0) {
                            return;
                        }
                    }
                    customDialog.showAlertBox("", "Product Successfully Added");
                    Stage stage = Dashboard.stage;

                    if (null != stage && stage.isShowing()) {
                        stage.close();
                    }
                    DBConnection.closeConnection(connection, ps, rs);
                }
            }
        } catch (Exception e) {

            customDialog.showAlertBox("Failed..", "");
        }
    }

    public void closeBn(ActionEvent event) {

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        if (stage.isShowing()) {
            stage.close();
        }
    }
}
