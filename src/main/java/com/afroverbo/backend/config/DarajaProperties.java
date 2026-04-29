package com.afroverbo.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "daraja")
public class DarajaProperties {
    private boolean mockEnabled = true;
    private String baseUrl = "https://sandbox.safaricom.co.ke";
    private String consumerKey;
    private String consumerSecret;
    private String shortCode;
    private String passkey;
    private String callbackUrl;
    private String transactionType = "CustomerPayBillOnline";
}
