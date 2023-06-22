package com.shop.management.Controller;

import com.shop.management.CustomDialog;
import com.shop.management.Method.Method;
import com.shop.management.PropertiesLoader;
import com.shop.management.util.AppConfig;
import com.shop.management.util.DBConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.controlsfx.control.Rating;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeedbackDialog implements Initializable {
    @FXML
    public TextField fullName;
    public TextField email;
    public TextField phone;
    public TextArea comments;
    public Rating rate;
    public Button bn_feedback_submit;
    public Label titleL;
    private DBConnection dbConnection;
    private CustomDialog customDialog;
    private Method method;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        method = new Method();
        dbConnection = new DBConnection();
        customDialog = new CustomDialog();
        rate.setRating(4);

        titleL.setText("we want to know what you thought of your experience at " + AppConfig.COMPANY_NAME + " so we'd love to here you feedback");


    }

    public void enterPress(KeyEvent e) {

        if (e.getCode() == KeyCode.ENTER) {
            //do something

            submitFeedback(null);
        }
    }

    public void submitFeedback(ActionEvent event) {

        String fullname = fullName.getText();
        String emailID = email.getText();
        String phoneNum = phone.getText();
        String comment = comments.getText();
        double rating = rate.getRating();

        rate.setRating(4);

        Pattern pattern = Pattern.compile(method.emailRegex);
        Matcher matcher = pattern.matcher(emailID);

        if (fullname.isEmpty()) {

            method.show_popup("Enter Full Name", fullName);
            return;
        } else if (!emailID.isEmpty()) {

            if (!matcher.matches()) {
                method.show_popup("Enter Valid Email", email);
                return;

            }

        } else if (!phoneNum.isEmpty()) {

            Pattern phone_pattern = Pattern.compile("^\\d{10}$");
            Matcher phone_matcher = phone_pattern.matcher(phoneNum);

            if (!phone_matcher.matches()) {

                customDialog.showAlertBox("Failed ",
                        "Enter 10-digit Phone Number Without Country Code");
                return;
            }
        } else if (comment.isEmpty()) {

            method.show_popup("Enter Comments", comments);
            return;
        }

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = dbConnection.getConnection();

            if (null == con) {
                return;
            }
            ps = con.prepareStatement(new PropertiesLoader().getReadProp().getProperty("FEEDBACK"));
            ps.setString(1, fullname);
            ps.setString(2, emailID);
            ps.setString(3, phoneNum);
            ps.setString(4, String.valueOf(rating));
            ps.setString(5, comment);

            int res = ps.executeUpdate();

            if (res > 0) {

                customDialog.showAlertBox("Successfully Submit Your Feedback",
                        " Thank you for your feedback");
                email.setText("");
                phone.setText("");
                comments.setText("");
                fullName.setText("");

            }

        } catch (SQLException | NumberFormatException e) {
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


    }
}
