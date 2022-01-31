module com.shop.management {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;
    requires validatorfx;
    requires java.logging;
    requires java.sql;
    requires org.apache.commons.io;
    requires org.controlsfx.controls;


    opens com.shop.management to javafx.fxml;
    exports com.shop.management;

    opens com.shop.management.Model to javafx.fxml;
    exports com.shop.management.Model;
    exports com.shop.management.Controller ;
    opens com.shop.management.Controller to javafx.fxml;
    exports com.shop.management.Method ;
    opens com.shop.management.Method to javafx.fxml;
}