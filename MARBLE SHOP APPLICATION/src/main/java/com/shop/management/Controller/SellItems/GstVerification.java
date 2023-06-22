package com.shop.management.Controller.SellItems;

import com.shop.management.CustomDialog;
import javafx.beans.binding.Bindings;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ResourceBundle;

public class GstVerification implements Initializable {
    public WebView webView;

    private final static String URL = "https://services.gst.gov.in/services/searchtp";
    public ProgressBar progBar;
    public Label messageLabel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        try {
            URL url2 = new URL(URL);
            URLConnection connection = url2.openConnection();
            connection.connect();
            messageLabel.managedProperty().bind(messageLabel.visibleProperty());
            messageLabel.setVisible(false);
            webView.setVisible(true);

        } catch (IOException e) {
            messageLabel.setVisible(true);
            webView.managedProperty().bind(webView.visibleProperty());
            webView.setVisible(false);
            new CustomDialog().showAlertBox("Loading Failed...","The Internet connection is not available. Please connect to the Internet");
            Stage stage = CustomDialog.stage3;

            if (null != stage && stage.isShowing()){
                stage.close();
            }
        }

        WebEngine e = webView.getEngine();
        e.load(URL);
        webView.setFontScale(1.5f);
        // set zoom
        webView.setZoom(0.8);

        progBar.progressProperty().bind(webView.getEngine().getLoadWorker().progressProperty());
        progBar.visibleProperty().bind(
                Bindings.when(progBar.progressProperty().lessThan(0).or(progBar.progressProperty().isEqualTo(1)))
                        .then(false)
                        .otherwise(true)
        );
        progBar.managedProperty().bind(progBar.visibleProperty());
        progBar.setMaxWidth(Double.MAX_VALUE);

    }
}
