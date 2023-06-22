package com.shop.management.Controller;

import com.shop.management.CustomDialog;
import com.shop.management.Main;
import com.shop.management.Method.GetUserProfile;
import com.shop.management.Method.Method;
import com.shop.management.Model.UserDetails;
import com.shop.management.PropertiesLoader;
import com.shop.management.util.DBConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.ResourceBundle;

public class Login implements Initializable {
    public TextField email_f;
    public PasswordField password_f;
    public CheckBox rememberPasswordCb;
    private Main main;
    private Method method;
    private CustomDialog customDialog;
    private DBConnection dbConnection;
    public static int currentlyLogin_Id = 0;
    public static int currentRole_Id = 0;
    public static String currentRoleName;
    private Connection connection;
    private Properties propRead;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        main = new Main();
        method = new Method();
        customDialog = new CustomDialog();
        dbConnection = new DBConnection();
        PropertiesLoader propLoader = new PropertiesLoader();
        propRead = propLoader.getReadProp();
    }
    @FXML
    public void forget_password_bn(ActionEvent event) {
        customDialog.showFxmlDialog("dashboard/forgotPassword.fxml", "Forgot Password");
    }

    public void login_bn(ActionEvent event) {
       startLogin();
    }
    private void getProfileDetails(ResultSet rs , PreparedStatement ps){

        try {
            openDashboard(rs, ps);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        //  getLicenseData(rs , ps);
    }

    private void openDashboard(ResultSet rs , PreparedStatement ps) throws SQLException {

        int userID = rs.getInt("user_id");
        int userStatus = rs.getInt("account_status");

        if (userStatus == 0) {
            customDialog.showAlertBox("Login Failed", "Your Account Has Been Inactive Please Contact Administrator");
            return;
        }

        currentlyLogin_Id = userID;
        UserDetails userDetails = new GetUserProfile().getUser(userID);

        if (null == userDetails) {

            customDialog.showAlertBox("Failed", "User Not Found");
        } else {


            currentRole_Id = userDetails.getRole_id();
            currentRoleName = userDetails.getRole();
            Main.primaryStage.setUserData(userDetails);
            main.changeScene("dashboard.fxml", "DASHBOARD");
            Main.primaryStage.setMaximized(true);
        }
    }

    private void getLicenseData(ResultSet rsProfile, PreparedStatement psProfile) {

            Connection connection = null;
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                connection = new DBConnection().getConnection();
                if (null == connection) {
                    return;
                }

                String query = "select expires_on from tbl_license";
                ps = connection.prepareStatement(query);
                rs = ps.executeQuery();

                if (rs.next()) {

                    String pattern = "dd-MM-yyyy";
                    SimpleDateFormat sdformat = new SimpleDateFormat(pattern);


                    Date currentDate = sdformat.parse(sdformat.format(new Date()));
                    Date expiresDate = sdformat.parse(rs.getString("expires_on"));

                    int checkExpireDate = currentDate.compareTo(expiresDate);

                    if (checkExpireDate > 0) {
                        customDialog.showFxmlDialog2("license/licenseMain.fxml", "");
                    } else {
                        openDashboard(rsProfile, psProfile);
                    }
                } else {
                    customDialog.showFxmlDialog2("license/licenseMain.fxml", "");
                }
            } catch (SQLException | ParseException e) {
                throw new RuntimeException(e);
            } finally {
                DBConnection.closeConnection(connection, ps, rs);
            }

    }

    public void create_new_account(ActionEvent event) {
        Main.primaryStage.setUserData("newUser");
        main.changeScene("signup.fxml", "Signup Here");
    }

    public void enterPress(KeyEvent e) {

        if (e.getCode() == KeyCode.ENTER) {
            //do something

            startLogin();
        }
    }

    private void startLogin() {

        PreparedStatement ps = null;
        ResultSet rs = null;

        String inputValue = email_f.getText();
        String password = password_f.getText();

        if (inputValue.isEmpty()) {
            method.show_popup("Please enter valid username", email_f);
            return;
        } else if (password.isEmpty()) {
            method.show_popup("Please enter password", password_f);
            return;
        }
        try {
            connection = dbConnection.getConnection();
            // Email Login
            long phoneNum = 0;
            try {
                phoneNum = Long.parseLong(inputValue);
            } catch (NumberFormatException e) {
                // e.printStackTrace();
            }
            ps = connection.prepareStatement(propRead.getProperty("LOGIN_WITH_PHONE"));
            ps.setLong(1, phoneNum);
            ps.setString(2, password);
            rs = ps.executeQuery();
            if (rs.next()) {
                getProfileDetails(rs , ps);
            } else {
                // USERNAME LOGIN
                ps = connection.prepareStatement(propRead.getProperty("LOGIN_WITH_USERNAME"));
                ps.setString(1, inputValue);
                ps.setString(2, password);
                rs = ps.executeQuery();
                if (rs.next()) {
                    getProfileDetails(rs,ps);
                } else {

                    ps = connection.prepareStatement(propRead.getProperty("LOGIN_WITH_EMAIL"));
                    ps.setString(1, inputValue);
                    ps.setString(2, password);
                    rs = ps.executeQuery();

                    if (rs.next()) {
                        getProfileDetails(rs,ps);

                    } else {
                        customDialog.showAlertBox("Authentication Failed", "Invalid email or password !");
                    }
                }
            }

        } catch (Exception e) {
            customDialog.showAlertBox("Authentication Failed","USER NOT FOUND");
        } finally {

            DBConnection.closeConnection(connection,ps,rs);

        }

    }


}
