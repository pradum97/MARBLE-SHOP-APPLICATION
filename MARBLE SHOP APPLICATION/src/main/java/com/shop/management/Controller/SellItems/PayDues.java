package com.shop.management.Controller.SellItems;

import com.shop.management.ImageLoader;
import com.shop.management.Main;
import com.shop.management.Method.Method;
import com.shop.management.Method.StaticData;
import com.shop.management.Model.Sale_Main;
import com.shop.management.PropertiesLoader;
import com.shop.management.util.DBConnection;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;

public class PayDues implements Initializable {
    public ComboBox<String> paymentModeC;
    public Label duesAmount;
    public TextField receivedAmountTF;
    public TextField duesAmountTF;
    private Sale_Main saleMain;
    private DecimalFormat df = new DecimalFormat("0.##");
    private Method method;
    private Properties propInsert , propUpdate ;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        method = new Method();
        PropertiesLoader propLoader = new PropertiesLoader();
        propUpdate = propLoader.getUpdateProp();
        propInsert = propLoader.getInsertProp();

        saleMain = (Sale_Main) Main.primaryStage.getUserData();

        setValue();
        comboBoxConfig();

        receivedAmountTF.textProperty().addListener((observableValue, s, t1) -> {
            double totalDues = saleMain.getDuesAmount();
            double receivedAmount = 0;
            try {
                receivedAmount = Double.parseDouble(t1);
            } catch (NumberFormatException ignored) {
            }

            if (receivedAmount <= totalDues) {

                double avlDues = totalDues - receivedAmount;

                duesAmountTF.setText(String.valueOf(Double.valueOf(df.format(avlDues))));
            } else {
                receivedAmountTF.setText("");
                method.show_popup("YOUR DUES AMOUNT IS : " + totalDues, receivedAmountTF);
            }

        });

    }

    private void comboBoxConfig() {

        paymentModeC.setItems(new StaticData().getPaymentMode());
        paymentModeC.getSelectionModel().selectFirst();

        paymentModeC.setButtonCell(new ListCell<>() {
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

        paymentModeC.setCellFactory(new Callback<>() {
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
    }

    private void setValue() {
        duesAmount.setText(String.valueOf(saleMain.getDuesAmount()));
        duesAmountTF.setEditable(false);
        duesAmountTF.setText(String.valueOf(saleMain.getDuesAmount()));
    }

    public void PAY_100(ActionEvent actionEvent) {
        receivedAmountTF.setText("");
        receivedAmountTF.setText(String.valueOf(saleMain.getDuesAmount()));
    }

    public void payDues(ActionEvent event) {

      pay(event.getSource());
    }

    public void cancel(ActionEvent event) {

        Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        if (stage.isShowing()){
            stage.close();
        }
    }

    public void enterClick(KeyEvent e) {

        if (e.getCode() == KeyCode.ENTER) {
            pay(e.getSource());
        }
    }

    private void pay(Object source){

        String paidAmount = receivedAmountTF.getText();
        String avlDues = duesAmountTF.getText();
        String paymentMode = paymentModeC.getSelectionModel().getSelectedItem();

        if (paidAmount.isEmpty()) {
            method.show_popup("Please Enter Received Amount", receivedAmountTF);
            return;
        }
        double paidAmountD = 0, avlDuesD = 0;
        try {
            paidAmountD = Double.parseDouble(paidAmount);
        } catch (NumberFormatException e) {
            method.show_popup("Enter Valid Amount", receivedAmountTF);
            return;
        }

        try {
            avlDuesD = Double.parseDouble(avlDues);
        } catch (NumberFormatException ignored) {

        }

        if (paidAmountD < 1) {
            method.show_popup("Enter more than 0", receivedAmountTF);
            return;
        }

        ImageView image = new ImageView(new ImageLoader().load("img/icon/warning_ic.png"));
        image.setFitWidth(45);
        image.setFitHeight(45);
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setAlertType(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Warning ");
        alert.setGraphic(image);
        alert.setHeaderText("Are you sure you want to pay the dues?");
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.initOwner(Main.primaryStage);
        Optional<ButtonType> result = alert.showAndWait();
        ButtonType button = result.orElse(ButtonType.CANCEL);
        if (button == ButtonType.OK) {

            Connection connection = null;
            PreparedStatement ps = null, psH = null;

            try {
                connection = new DBConnection().getConnection();
                if (null == connection) {
                    return;
                }
                ps = connection.prepareStatement(propUpdate.getProperty("UPDATE_DUES_IN_PAYDUES"));
                ps.setDouble(1, paidAmountD);
                ps.setInt(2, saleMain.getDuesId());

                int res = ps.executeUpdate();

                if (res > 0) {

                    ps = null;

                    ps = connection.prepareStatement(propUpdate.getProperty("UPDATE_SALE_MAIN_IN_PAYDUES"));
                    ps.setDouble(1,paidAmountD);
                    ps.setInt(2,saleMain.getSale_main_id());
                    ps.executeUpdate();

                    Stage stage = (Stage) ((Node)source).getScene().getWindow();

                    String query = propInsert.getProperty("INSERT_DUES_HISTORY_IN_PAYDUES");
                    psH = connection.prepareStatement(query);
                    psH.setInt(1, saleMain.getDuesId());
                    psH.setInt(2, saleMain.getCustomerId());
                    psH.setInt(3, saleMain.getSale_main_id());
                    psH.setDouble(4, saleMain.getDuesAmount());
                    psH.setDouble(5, paidAmountD);
                    psH.setDouble(6, avlDuesD);
                    psH.setString(7, paymentMode);
                    int resH = psH.executeUpdate();

                    if (resH > 0) {

                        if (avlDuesD == 0){

                            ps  = null;
                            String duesQuery = "DELETE FROM TBL_DUES where dues_id ="+saleMain.getDuesId();
                            ps = connection.prepareStatement(duesQuery);
                            ps.executeUpdate();

                        }

                        Main.primaryStage.setUserData((boolean)true);
                        stage.close();
                    }

                }


            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                DBConnection.closeConnection(connection, ps, null);
                try {
                    if (null != psH) {
                        psH.close();
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
