package com.shop.management.Controller.License;

import com.shop.management.CustomDialog;
import com.shop.management.ImageLoader;
import com.shop.management.Main;
import com.shop.management.util.DBConnection;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.*;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class LicenseMain implements Initializable {
    public VBox calenderContainer;
    public Label dayCountL;
    public Label appIdL;
    public Label emailL;
    public Label subscriptionStatus;
    public Label startOnL;
    public Label expireL;
    public Label licenseType;
    public HBox detailsContainer;
    public HBox activateLicenseContainer;
    private CustomDialog customDialog;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        customDialog = new CustomDialog();
        setCalenderImage();
        activateLicenseContainer.managedProperty().bind(activateLicenseContainer.visibleProperty());
        activateLicenseContainer.setVisible(false);
        detailsContainer.setDisable(true);
        getLicenseData();
    }
    private void setCalenderImage() {
        Image image = new ImageLoader().loadWithSize("img/icon/calender.png");
        BackgroundImage myBI = new BackgroundImage(image,
                BackgroundRepeat.REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT,
                BackgroundSize.DEFAULT);
        calenderContainer.setStyle("-fx-border-color: transparent");
        calenderContainer.setBackground(new Background(myBI));
    }

    public void renewBn(ActionEvent actionEvent) {
        openRenewPage("Renew Your License");
    }
    private void getLicenseData() {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            connection = new DBConnection().getConnection();
            if (null == connection){
                return;
            }

            String query = "select application_id ,start_on ,expires_on ,license_type , registered_email from tbl_license";
            ps = connection.prepareStatement(query);
            rs = ps.executeQuery();

            if (rs.next()){
                activateLicenseContainer.managedProperty().bind(activateLicenseContainer.visibleProperty());
                activateLicenseContainer.setVisible(false);
                detailsContainer.setDisable(false);

                String appId = rs.getString("application_id");
                String startOn = rs.getString("start_on");
                String expiresDate = rs.getString("expires_on");
                String type = rs.getString("license_type");
                String registered_email = rs.getString("registered_email");
                appIdL.setText(appId);
                emailL.setText(registered_email);
                licenseType.setText(type);

                startOnL.setText(startOn);
                expireL.setText(expiresDate);

                String pattern = "dd-MM-yyyy";
                SimpleDateFormat sdformat = new SimpleDateFormat(pattern);
                DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(pattern);

                Date currentDate = sdformat.parse(sdformat.format(new Date()));
                Date expires_Date = sdformat.parse(expiresDate);

                long days = ChronoUnit.DAYS.between(LocalDate.parse(dateFormat.format(LocalDate.now()),dateFormat),
                        LocalDate.parse(expiresDate,dateFormat));

                if (days < 0){
                    days = 0;
                }
                dayCountL.setText(String.valueOf(days));

                int checkExpireDate = currentDate.compareTo(expires_Date);

                if (checkExpireDate > 0) {
                    // The Plan Has Expired. Please Renew The Plan!
                    statusStyle("RED","Your license has expired. please renew the license !");
                } else if (checkExpireDate == 0) {
                    //Today Is The Last Date Of The Plan!
                    statusStyle("green","Today is the last day of the license");
                } else if (checkExpireDate < 0) {
                   //Plan is Running Now 😊
                    statusStyle("green","ACTIVE");
                }
            }else {
                detailsContainer.setDisable(true);
                activateLicenseContainer.setVisible(true);

            }
        } catch (SQLException | ParseException e) {
            throw new RuntimeException(e);
        } finally {
            DBConnection.closeConnection(connection , ps,rs);
        }
    }
    private void statusStyle(String borderColor , String text) {
        subscriptionStatus.setText(text.toUpperCase());
        subscriptionStatus.setStyle("-fx-text-fill:"+borderColor+" ;" +
                "-fx-background-color: white; -fx-border-radius: 7;-fx-padding: 0 , 10 , 0 , 10 ; -fx-font-weight: bold;-fx-font-family: 'Arial Black'");
    }

    public void activateLicenseBn(ActionEvent actionEvent) {

        openRenewPage("Activate Your Subscription");
    }

    private void openRenewPage(String title){

        int avlDays = Integer.parseInt(dayCountL.getText());
        Map<String , Object> map = new HashMap<>();
        map.put("avlDays" , avlDays);
        map.put("licenseType" , licenseType.getText());

        Main.primaryStage.setUserData(map);

        customDialog.showFxmlDialog2("license/renewLicense.fxml",title);

        getLicenseData();
    }
}
