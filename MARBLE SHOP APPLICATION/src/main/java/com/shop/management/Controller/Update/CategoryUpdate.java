package com.shop.management.Controller.Update;

import com.shop.management.Main;
import com.shop.management.Method.Method;
import com.shop.management.Model.CategoryModel;
import com.shop.management.util.DBConnection;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class CategoryUpdate implements Initializable {
    public TextField categoryNameTF;
    private  CategoryModel categoryModel;
    private Method method;
    private DBConnection dbConnection;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

         categoryModel = (CategoryModel) Main.primaryStage.getUserData();
        method = new Method();
        dbConnection = new DBConnection();

         if (null == categoryModel){
             return;
         }
        setData(categoryModel);
    }

    private void setData(CategoryModel categoryModel) {

        categoryNameTF.setText(categoryModel.getCategoryName());
    }

    public void updateCategory(ActionEvent event) {

       update(event.getSource());

    }

    private void update(Object source) {

        String cName = categoryNameTF.getText();

        if (cName.isEmpty()){
            method.show_popup("ENTER CATEGORY NAME",categoryNameTF);
            return;
        }

        Connection connection = null;
        PreparedStatement ps = null;

        try {
            connection = dbConnection.getConnection();;
            if (null == dbConnection){
                return;
            }

            ps = connection.prepareStatement("UPDATE tbl_category SET CATEGORY_NAME = ? where category_id = ?");
            ps.setString(1,cName);
            ps.setInt(2,categoryModel.getCategoryId());

            int res = ps.executeUpdate();

            if (res >0){
                categoryNameTF.setText("");

                Stage stage = (Stage) ((Node) source).getScene().getWindow();

                if (stage.isShowing()){
                    stage.close();
                }
            }

        } catch (SQLException e) {
        }finally {
            DBConnection.closeConnection(connection,ps,null);
        }
    }

    public void cancel(ActionEvent event) {

        Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        if (stage.isShowing()){
            stage.close();
        }
    }

    public void enterPress(KeyEvent event) {

        if (event.getCode() == KeyCode.ENTER){
            update(event.getSource());
        }
    }
}
