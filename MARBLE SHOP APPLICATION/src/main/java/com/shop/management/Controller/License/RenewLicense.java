package com.shop.management.Controller.License;

import com.shop.management.CustomDialog;
import com.shop.management.Main;
import com.shop.management.Method.Method;
import com.shop.management.Method.TimeFromOnline;
import com.shop.management.util.DBConnection;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class RenewLicense implements Initializable {
    private final String RENEW_URL = "http://techwhizer.in/licenseApi/renewLicense.php";
    private final String UPDATE_DATE_URL = "http://techwhizer.in/licenseApi/update/updateSerialKey.php";
    private final String RENEW = "renew";
    private final String APP_LINK = "appLink";
    private final String pattern = "dd-MM-yyyy";
    public TextField applicationIdTf;
    public TextField serialKeyTf;
    public VBox textFieldContainer;
    public Button activateBn;
    public Label msgL;
    private Method method;
    private CustomDialog customDialog;
    private String type = null;
    private DBConnection dbConnection;
    private Map<String, Object> map;
    private int availableDays;
    private String licenseType;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        map = (Map<String, Object>) Main.primaryStage.getUserData();
        if (null == map) {
            return;
        }
        availableDays = (int) map.get("avlDays");
        licenseType = (String) map.get("licenseType");
        method = new Method();
        customDialog = new CustomDialog();
        dbConnection = new DBConnection();
        getApplicationIdFromDb();
    }

    private void getApplicationIdFromDb() {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            connection = dbConnection.getConnection();
            if (null == connection) {
                return;
            }
            String query = "select application_id from TBL_LICENSE";
            ps = connection.prepareStatement(query);
            rs = ps.executeQuery();
            if (rs.next()) {
                type = RENEW;
                String appId = rs.getString("application_id");
                applicationIdTf.setText(appId);
                applicationIdTf.setEditable(false);
                applicationIdTf.setFocusTraversable(false);
            } else {
                applicationIdTf.setEditable(true);
                type = APP_LINK;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            DBConnection.closeConnection(connection, ps, rs);
        }
    }

    public void activateBn(ActionEvent actionEvent) {

        String applicationId = applicationIdTf.getText().replaceAll(" ", "");
        String serialKey = serialKeyTf.getText().replaceAll(" ", "");

        if (applicationId.isEmpty()) {

            method.show_popup("Please Enter Application Id", applicationIdTf);
            return;
        } else if (serialKey.isEmpty()) {
            method.show_popup("Please Enter Serial Key", serialKeyTf);
            return;

        }

        msgL.setText("Please Wait...");
        activateBn.setDisable(true);

        boolean isDateValid = new TimeFromOnline().getCurrentDate();

        if (!isDateValid) {
            msgL.setText("Please Connect Internet!");
            activateBn.setDisable(false);
            customDialog.showAlertBox("Failed", "Invalid date or internet not connected. please connect to internet");
            return;
        }

        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(RENEW_URL);
        List<NameValuePair> params = new ArrayList<>();

        params.add(new BasicNameValuePair("appId", applicationId));
        params.add(new BasicNameValuePair("serialKey", serialKey));
        params.add(new BasicNameValuePair("type", type));

        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = dbConnection.getConnection();
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity respEntity = response.getEntity();

            if (respEntity != null) {
                String content = EntityUtils.toString(respEntity);
                JSONObject jb = new JSONObject(content);
                int statusCode = jb.getInt("statusCode");
                String message = jb.getString("message");

                if (statusCode == 200) {

                    JSONObject js = jb.getJSONObject("data");

                    int companyId = js.getInt("company_id");
                    int licence_period_days = js.getInt("licence_period_days");
                    String application_id = js.getString("application_id");
                    String serial_key = js.getString("serial_key");
                    String license_type = js.getString("license_type");
                    String registeredEmail = js.getString("email");

                    DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(pattern);
                    SimpleDateFormat sdformat = new SimpleDateFormat(pattern);
                    LocalDate currentD = LocalDate.now();
                    Period p = Period.ofDays((licence_period_days + availableDays));
                    LocalDate ld = currentD.plus(p);
                    String expiresDate = sdformat.format(sdformat.parse(dateFormat.format(ld)));
                    String currentDate = dateFormat.format(currentD);
                    if (null == connection) {
                        return;
                    }
                    if (Objects.equals(type, APP_LINK)) {

                        String query = "INSERT INTO tbl_license (COMPANY_ID, APPLICATION_ID, SERIAL_KEY, START_ON,\n" +
                                "                         EXPIRES_ON, LICENSE_PERIOD_DAYS, REGISTERED_EMAIL,LICENSE_TYPE) VALUES (?,?,?,?,?,?,?,?)";

                        ps = connection.prepareStatement(query);
                        ps.setInt(1, companyId);
                        ps.setString(2, application_id);
                        ps.setString(3, serial_key);
                        ps.setString(4, currentDate);
                        ps.setString(5, expiresDate);
                        ps.setInt(6, licence_period_days);
                        ps.setString(7, registeredEmail);
                        ps.setString(8, license_type);
                        int res = ps.executeUpdate();
                        if (res > 0) {
                            updateDateInOnlineServer(serial_key, expiresDate, actionEvent.getSource());
                            getApplicationIdFromDb();
                        }
                    } else if (Objects.equals(type, RENEW)) {

                        String query = "UPDATE tbl_license SET COMPANY_ID = ?, APPLICATION_ID = ?, SERIAL_KEY= ?, START_ON= ?,\n" +
                                "                         EXPIRES_ON= ?, LICENSE_PERIOD_DAYS= ?, REGISTERED_EMAIL= ?,LICENSE_TYPE= ?";

                        ps = connection.prepareStatement(query);
                        ps.setInt(1, companyId);
                        ps.setString(2, application_id);
                        ps.setString(3, serial_key);
                        ps.setString(4, currentDate);
                        ps.setString(5, expiresDate);
                        ps.setInt(6, licence_period_days);
                        ps.setString(7, registeredEmail);
                        ps.setString(8, license_type);
                        int res = ps.executeUpdate();
                        if (res > 0) {
                            updateDateInOnlineServer(serial_key, expiresDate, actionEvent.getSource());
                            getApplicationIdFromDb();
                        }
                    }
                } else {
                    msgL.setText("");
                    activateBn.setDisable(false);
                    customDialog.showAlertBox("Failed", message);
                }
            }
        } catch (Exception e) {
            msgL.setText("");
            activateBn.setDisable(false);
            e.printStackTrace();
        } finally {
            activateBn.setDisable(false);
            DBConnection.closeConnection(connection, ps, null);
        }
    }

    private void updateDateInOnlineServer(String serial_key, String expiresDate, Object source) {

        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(UPDATE_DATE_URL);
        List<NameValuePair> params = new ArrayList<NameValuePair>();

        params.add(new BasicNameValuePair("serialKey", serial_key));
        params.add(new BasicNameValuePair("expiresDate", expiresDate));
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity respEntity = response.getEntity();
            if (respEntity != null) {
                String content = EntityUtils.toString(respEntity);

                System.out.println(content);

                JSONObject jb = new JSONObject(content);
                int statusCode = jb.getInt("statusCode");
                String message = jb.getString("message");

                if (statusCode == 200) {

                    serialKeyTf.setText("");
                    customDialog.showAlertBox("Successful", "Licence Successfully Activated");

                    Stage stage = (Stage) ((Node) source).getScene().getWindow();
                    if (null != stage && stage.isShowing()) {
                        stage.close();
                    }

                } else {
                    msgL.setText("");
                    activateBn.setDisable(false);
                    customDialog.showAlertBox("Failed", message);
                }
            }

        } catch (Exception e) {
            msgL.setText("");
            activateBn.setDisable(false);
            e.printStackTrace();
        }


    }
}
