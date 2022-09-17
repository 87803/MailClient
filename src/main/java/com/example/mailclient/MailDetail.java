package com.example.mailclient;

public class MailDetail {
    String subject;
    String from;
    String content;
    String sendTime;
    String receiver;
    String size;
    String messageID;
    MailDetail(String subject,String from,String content,
               String sendTime,String receiver,String size, String messageID)
    {
        this.content=content;
        this.from=from;
        this.sendTime=sendTime;
        this.receiver=receiver;
        this.size=size;
        this.subject=subject;
        this.messageID=messageID;
    }
}
