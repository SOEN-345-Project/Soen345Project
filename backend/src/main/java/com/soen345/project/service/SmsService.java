package com.soen345.project.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private final String fromPhoneNumber;

    public SmsService(
            @Value("${twilio.account-sid}") String accountSid,
            @Value("${twilio.auth-token}") String authToken,
            @Value("${twilio.phone-number}") String fromPhoneNumber) {
        this.fromPhoneNumber = fromPhoneNumber;
        Twilio.init(accountSid, authToken);
    }

    public void sendVerificationSms(String toPhoneNumber, String verificationCode) {
        String body = "Your Eventigo verification code is: " + verificationCode
                + ". It expires in 15 minutes.";
        Message.creator(
                new PhoneNumber(toPhoneNumber),
                new PhoneNumber(fromPhoneNumber),
                body
        ).create();
    }
}
