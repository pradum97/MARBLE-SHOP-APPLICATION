package com.shop.management.Method;

import com.shop.management.Model.Role;
import com.shop.management.PropertiesLoader;
import com.shop.management.util.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Method extends StaticData {
    public ObservableList<String> getProductColor() {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        ObservableList<String> colorList = FXCollections.observableArrayList();

        try {
            connection = new DBConnection().getConnection();
            String query = "select color_name from tbl_color";
            ps = connection.prepareStatement(query);

            rs = ps.executeQuery();
            while (rs.next()){
                String colorName = rs.getString("color_name");
                colorList.add(colorName);
            }
            return colorList;
        } catch (SQLException e) {
            return FXCollections.observableArrayList("Color Not Available!");
        }finally {
            DBConnection.closeConnection(connection , ps,rs);
        }
    }

    public ObservableList<Role> getRole(){

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        ObservableList<Role> role = FXCollections.observableArrayList();

        try {
            connection = new DBConnection().getConnection();
            if (null == connection) {
                System.out.println(" Signup ( 65 ) : Connection Failed");
                return null;
            }
            ps = connection.prepareStatement(new PropertiesLoader().getReadProp().getProperty("ROLE"));
            rs = ps.executeQuery();

            while (rs.next()) {
                int role_Id = rs.getInt("ROLE_ID");
                String roleName = rs.getString("ROLE");

              role.add(new Role(role_Id ,roleName));

            }
            return  role;

        } catch (SQLException e) {
            e.printStackTrace();
            return  null;
        } finally {

            DBConnection.closeConnection(connection, ps, rs);
        }
    }

    public ContextMenu show_popup(String message, Object textField){

       ContextMenu form_Validator = new ContextMenu();
        form_Validator.setAutoHide(true);
        form_Validator.getItems().add(new MenuItem(message));
        form_Validator.show((Node) textField, Side.RIGHT, 10, 0);

        return form_Validator;

    }

    public String get_mac_address() {

        InetAddress ip;
        StringBuilder sb;
        try {

            ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);

            byte[] mac = network.getHardwareAddress();

            if (null == mac){
                return "Not-Found";
            }else {
                sb = new StringBuilder();
                for (int i = 0; i < mac.length; i++) {
                    sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                }
                return sb.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        }
    }

}
