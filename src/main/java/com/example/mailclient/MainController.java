package com.example.mailclient;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.mail.MessagingException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainController {
    public Button attachmentButton;
    public HTMLEditor mailEditor;
    public TextField receiverText;
    public TextField CCText;
    public TextField BCText;
    public Button picButton;
    public Button sendButton;
    public ListView mailList;
    public MailClient mailClient;
    public WebView webView;
    public Label senderLabel;
    public Label sizeLabel;
    public Label receiverLabel;
    public Label timeLabel;
    public Label fileLabel;
    public TextField themeText;
    public ProgressIndicator processIndicator;
    public ProgressBar processBar;
    public Label downloadLabel;
    public Label loadMailLabel;
    ArrayList<File> fileArr = new ArrayList();
    String selectMail;

    @FXML
    public void initialize()
    {
        mailList.getSelectionModel().selectedItemProperty().addListener((ChangeListener<Object>) (observable, oldValue, newValue) -> {
            //这里写自己的代码
            //System.out.println(newValue);
            selectMail = ((String) newValue);
            if(selectMail != null) {
                webView.getEngine().loadContent(mailClient.mailMaps.get(selectMail).content);
                receiverLabel.setText(mailClient.mailMaps.get(selectMail).receiver);
                timeLabel.setText(mailClient.mailMaps.get(selectMail).sendTime);
                senderLabel.setText(mailClient.mailMaps.get(selectMail).from);
                sizeLabel.setText(mailClient.mailMaps.get(selectMail).size);
                System.out.println(mailClient.mailMaps.get(selectMail).content);
            }
        });
        processIndicator.setProgress(-1);
        processBar.setProgress(-1);
    }

    public void clickAttachmentButton() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("添加附件");
        Stage stage = (Stage) attachmentButton.getScene().getWindow();
        File f = fileChooser.showOpenDialog(stage);
        if(f != null)
        {
            fileArr.add(f);
            if(fileLabel.getText().equals(""))
                fileLabel.setText("当前选择的附件有: ");
            fileLabel.setText(fileLabel.getText()+"\n"+f);
            System.out.println(fileArr);
        }
    }

    public void clickPicButton() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("添加图片文件");
        Stage stage = (Stage) attachmentButton.getScene().getWindow();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("图片文件", "*.jpg;*.png;*.gif;*.jpeg")
                //new FileChooser.ExtensionFilter("All FILE", "*.*")
        );

        File f = fileChooser.showOpenDialog(stage);
        //String imagePath=f;
        if(f != null) {
            mailEditor.setHtmlText(mailEditor.getHtmlText() + "<img src='file:\\\\" + f + "'/>");
            System.out.println(f);
        }
    }

    public void clickSendButton() {
        Map<String, String> maps = new HashMap<>(){{
            put("receiver", receiverText.getText());
            put("cc", CCText.getText());
            put("bc", BCText.getText());
            put("subject", themeText.getText());
        }};
        System.out.println(maps);
        try {
            mailClient.sendMail(maps, fileArr, mailEditor.getHtmlText());
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("发送成功");
            alert.setHeaderText("发送成功");
            alert.setContentText("邮件已送达");
            alert.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("发送失败");
            alert.setHeaderText("发送发生错误");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            //throw new RuntimeException(e);
        }
        System.out.println(mailEditor.getHtmlText());
    }

    public void updateMailList() {
        ObservableList<String> mailArr =  FXCollections.observableArrayList();
        //System.out.println("Key = " + key);
        mailArr.addAll(mailClient.mailMaps.keySet());
        mailList.setItems(mailArr);
    }

    public void setMailClient(MailClient mailClient) {
        this.mailClient = mailClient;
    }

    public void clickClearFileButton() {
        fileArr.clear();
        fileLabel.setText("");
    }

    public void clickDeleteButton() {
        if(!selectMail.equals(""))
        {
            try {
                mailClient.deleteMail(selectMail);
            } catch (MessagingException e) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("删除错误");
                alert.setHeaderText("删除发生错误");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
            mailClient.mailMaps.remove(selectMail);
            updateMailList();
        }
    }

    public void clickReloadButton() {
        new Thread(mailClient).start();//mailClient.getMail();
    }
    public void showProcess()
    {
        loadMailLabel.setVisible(true);
        downloadLabel.setVisible(true);
        processIndicator.setVisible(true);
        processBar.setVisible(true);
        processIndicator.setProgress(-1);
        processBar.setProgress(-1);
    }
    public void hideProcess()
    {
        loadMailLabel.setVisible(false);
        downloadLabel.setVisible(false);
        processIndicator.setVisible(false);
        processBar.setVisible(false);
    }
    public void updateProcess(int i, int count)
    {
        processBar.setProgress((double)i/(double)count);
        downloadLabel.setText("下载第" + i +"/" + count + "封邮件");
    }
}

