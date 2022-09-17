package com.example.mailclient;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 580, 400);
        stage.setTitle("邮件客户端");
        stage.setScene(scene);
        //new MailClient();
        stage.show();

    }

    public static void main(String[] args) {
        launch();
    }
}