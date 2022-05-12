module client {
    requires javafx.controls;
    requires javafx.fxml;
    requires service;
    requires io.netty.transport;
    requires io.netty.codec;


    opens client to javafx.fxml;
    exports client;

}
