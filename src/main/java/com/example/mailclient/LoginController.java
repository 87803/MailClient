package com.example.mailclient;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LoginController {
    public ChoiceBox recServerChoiceBox;
    public TextField emailAdd;
    public PasswordField emailPwd;
    public TextField pop3Server;
    public CheckBox recSSL;
    public TextField recPort;
    public TextField smtpServer;
    public CheckBox sendSSL;
    public TextField sendPort;
    public Button loginButton;


    @FXML
    public void initialize()
    {
        ObservableList<String> item = FXCollections.observableArrayList("POP3");
        recServerChoiceBox.setItems(item);
        recServerChoiceBox.setValue("POP3");
    }

    public void clickLoginButton() {
        Map<String, String> config = new HashMap<>(){{
            put("emailAdd",  emailAdd.getText());
            put("emailPwd", emailPwd.getText());
            put("pop3Server",  pop3Server.getText());
            put("recSSL", String.valueOf(recSSL.isSelected()));
            put("recPort",  recPort.getText());
            put("smtpServer", smtpServer.getText());
            put("sendSSL",  String.valueOf(sendSSL.isSelected()));
            put("sendPort", sendPort.getText());
        }};
        System.out.println(config);
        MailClient mc = new MailClient(config);
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1000, 600);
            MainController mainController = fxmlLoader.getController();
            mainController.setMailClient(mc);
            mc.setMainController(mainController);
            mc.login();
            Stage stage2 = (Stage) loginButton.getScene().getWindow();
            stage2.close();
            Stage stage = new Stage();
            stage.setTitle("邮件客户端");
            stage.setScene(scene);
            //mainController.updateMailList();
            stage.show();

        } catch (MessagingException | IOException e) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("登录错误");
            alert.setHeaderText("登录发生错误");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            System.out.println(e.getMessage());
            //throw new RuntimeException(e);
        }
    }
}
