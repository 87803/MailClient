package com.example.mailclient;

import javafx.application.Platform;
import javafx.scene.control.Alert;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MailClient implements Runnable{
    private final Session session;
    private final String emailAdd;
    private final String emailPwd;
    public Map<String, MailDetail> mailMaps;
    private MainController mainController;

    MailClient(Map<String, String> config) {
        // 创建参数配置, 用于连接邮件服务器的参数配置
        // SMTP
        Properties props = new Properties();                    // 参数配置
        props.setProperty("mail.transport.protocol", "smtp");   // 使用的协议（JavaMail规范要求）
        props.setProperty("mail.smtp.auth", "true");            // 需要请求认证
        props.setProperty("mail.smtp.host", config.get("smtpServer"));   // SMTP 服务器地址
        props.setProperty("mail.smtp.port", config.get("sendPort"));  //端口
        if(config.get("sendSSL").equals("true"))
        {
            props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.setProperty("mail.smtp.socketFactory.fallback", "false");
            props.setProperty("mail.smtp.socketFactory.port", config.get("sendPort"));
            props.setProperty("mail.smtp.starttls.Enable", "true");
        }

        // POP3
        props.setProperty("mail.store.protocol", "pop3");		// 协议
        props.setProperty("mail.pop3.host", config.get("pop3Server"));	// pop3服务器
        props.setProperty("mail.pop3.port", config.get("recPort"));	// 端口
        if(config.get("recSSL").equals("true"))
        {
            props.setProperty("mail.pop3.socketFactory.fallback","false");
            props.setProperty("mail.pop3.socketFactory.port",config.get("recPort"));
            props.setProperty("mail.pop3.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.setProperty("mail.pop3.starttls.Enable", "true");
        }

        // 根据配置创建会话对象
        session = Session.getInstance(props);
        //session.setDebug(true);
        emailAdd = config.get("emailAdd");
        emailPwd = config.get("emailPwd");

    }

    public void login() throws MessagingException {
        Transport transport = session.getTransport();
        transport.connect(emailAdd, emailPwd);
        transport.close();
        Store store = session.getStore("pop3");
        store.connect(emailAdd, emailPwd);
        store.close();

        new Thread(this).start();//getMail();  //登录成功后获取邮件

    }

    public void getMail() throws MessagingException {
        Store store = session.getStore("pop3");
        store.connect(emailAdd, emailPwd);

        Folder folder = store.getFolder("INBOX");
        folder.open(Folder.READ_ONLY);	//打开收件箱 Folder.READ_ONLY：只读权限 Folder.READ_WRITE：可读可写（可以修改邮件的状态）
        // 由于POP3协议无法获知邮件的状态,所以getUnreadMessageCount得到的是收件箱的邮件总数
        //System.out.println("未读邮件数: " + folder.getUnreadMessageCount());
        // 由于POP3协议无法获知邮件的状态,所以下面得到的结果始终都是为0
        //System.out.println("删除邮件数: " + folder.getDeletedMessageCount());
        //System.out.println("新邮件: " + folder.getNewMessageCount());

        // 获得收件箱中的邮件总数
        System.out.println("邮件总数: " + folder.getMessageCount());
        // 得到收件箱中的所有邮件,并解析
        Message[] messages = folder.getMessages();
        try {
            mailMaps = ParseMail.parseMessage(mainController, messages);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //解决Not on FX application thread; currentThread = Thread-3
        Platform.runLater(() -> {
            mainController.updateMailList();//更新JavaFX的主线程的代码放在此处
        });

        //释放资源
        folder.close(true);
        store.close();
    }

    public void setMainController(MainController mainController)
    {
        this.mainController = mainController;
    }


    public void sendMail(Map<String, String> maps, ArrayList<File> fileArr, String content) throws Exception {
        MimeMessage message = createMimeMessage(maps, fileArr, content);

        // 4. 根据 Session 获取邮件传输对象
        Transport transport = session.getTransport();
        transport.connect(emailAdd, emailPwd);

        // 6. 发送邮件, 发到所有的收件地址, message.getAllRecipients() 获取到的是在创建邮件对象时添加的所有收件人, 抄送人, 密送人
        transport.sendMessage(message, message.getAllRecipients());

        // 7. 关闭连接
        transport.close();
    }

    public MimeMessage createMimeMessage(Map<String, String> maps, ArrayList<File> fileArr, String content) throws Exception {
        // 1. 创建邮件对象
        MimeMessage message = new MimeMessage(session);
        // 2. From: 发件人
        message.setFrom(new InternetAddress(emailAdd, emailAdd, "UTF-8"));
        // 3. To: 收件人（可以增加多个收件人、抄送、密送）
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(maps.get("receiver"), maps.get("receiver"),"UTF-8"));
        if(!maps.get("cc").equals(""))
            message.addRecipient(Message.RecipientType.CC, new InternetAddress(maps.get("cc"), maps.get("cc"), "UTF-8"));
        if(!maps.get("bc").equals(""))
            message.addRecipient(Message.RecipientType.BCC, new InternetAddress(maps.get("bc"), maps.get("bc"), "UTF-8"));
        // 4. Subject: 邮件主题
        message.setSubject(maps.get("subject"), "UTF-8");


        //下面是邮件内容的创建:
        // 把图片的本地路径转为节点
        ArrayList<MimeBodyPart> imageNode = new ArrayList<>();
        int picNum=0;
        Pattern pattern = Pattern.compile("<img src=\"file:.*?>");
        Matcher matcher = pattern.matcher(content);
        while(matcher.find())
        {
            String picPath = content.substring(matcher.start()+17,matcher.end()-2);
            content = content.replaceFirst("<img src=\"file:.*?>","<img src='cid:imageIndex"+ picNum +"'/>");
            matcher = pattern.matcher(content);

            // 5. 创建图片“节点”
            MimeBodyPart image = new MimeBodyPart();
            DataHandler dh = new DataHandler(new FileDataSource(picPath)); // 读取本地文件
            image.setDataHandler(dh);		            // 将图片数据添加到“节点”
            image.setContentID("imageIndex"+picNum);	    // 为“节点”设置一个唯一编号（在文本“节点”将引用该ID）
            imageNode.add(image);
            picNum++;
        }



        // 6. 创建文本“节点”
        MimeBodyPart text = new MimeBodyPart();
        //    这里添加图片的方式是将整个图片包含到邮件内容中, 实际上也可以以 http 链接的形式添加网络图片
        text.setContent(content, "text/html;charset=UTF-8");

        // 7. （文本+图片）设置 文本 和 图片 “节点”的关系（将 文本 和 图片 “节点”合成一个混合“节点”）
        MimeMultipart mm_text_image = new MimeMultipart();
        mm_text_image.addBodyPart(text);
        for (MimeBodyPart mimeBodyPart : imageNode)
            mm_text_image.addBodyPart(mimeBodyPart);
        mm_text_image.setSubType("related");	// 关联关系

        // 8. 将 文本+图片 的混合“节点”封装成一个普通“节点”
        //    最终添加到邮件的 Content 是由多个 BodyPart 组成的 Multipart, 所以我们需要的是 BodyPart,
        //    上面的 mm_text_image 并非 BodyPart, 所有要把 mm_text_image 封装成一个 BodyPart
        MimeBodyPart text_image = new MimeBodyPart();
        text_image.setContent(mm_text_image);

        MimeMultipart mm = new MimeMultipart();
        mm.addBodyPart(text_image);
        // 9. 创建附件“节点”
        for(int i=0;i<fileArr.size();i++)
        {
            MimeBodyPart attachment = new MimeBodyPart();
            DataHandler dh2 = new DataHandler(new FileDataSource(fileArr.get(i)));  // 读取本地文件
            attachment.setDataHandler(dh2);                                                // 将附件数据添加到“节点”
            attachment.setFileName(MimeUtility.encodeText(dh2.getName()));                // 设置附件的文件名（需要编码）

            // 10. 设置（文本+图片）和 附件 的关系（合成一个大的混合“节点” / Multipart ）
            mm.addBodyPart(attachment);        // 如果有多个附件，可以创建多个多次添加

        }
        mm.setSubType("mixed");            // 混合关系
        // 11. 设置整个邮件的关系（将最终的混合“节点”作为邮件的内容添加到邮件对象）
        message.setContent(mm);

        // 12. 设置发件时间
        message.setSentDate(new Date());

        // 13. 保存上面的所有设置
        message.saveChanges();

        return message;
    }

    public void deleteMail(String deleteMailName) throws MessagingException {
        MailDetail delete = mailMaps.get(deleteMailName);

        Store store = session.getStore("pop3");
        store.connect(emailAdd, emailPwd);

        Folder folder = store.getFolder("INBOX");
        folder.open(Folder.READ_WRITE);
        Message[] messages = folder.getMessages();

        for(int i=0; i<messages.length; i++) {
            MimeMessage msg = (MimeMessage) messages[i];
            if(delete.messageID.equals(msg.getMessageID())) {
                //设置删除标记
                messages[i].setFlag(Flags.Flag.DELETED, true);
                break;
            }
        }
        //释放资源
        folder.close(true);
        store.close();
    }

    @Override
    public void run() {
        try {
            getMail();//更新JavaFX的主线程的代码放在此处
        } catch (MessagingException e) {
            //解决Not on FX application thread; currentThread = Thread-3
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("收取错误");
                alert.setHeaderText("收取发生错误");
                alert.setContentText(e.getMessage());
                alert.showAndWait();//更新JavaFX的主线程的代码放在此处
            });

            //throw new RuntimeException(e);
        }

    }
}
