package com.example.presentationservice.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(HashUtils.class);

    private static final String CHARACTERSET_NAME_UTF_8 = "UTF-8";
    private static final String TYPE_OF_MESSAGE_DIGIT = "MD5";

    public static String hashPassword(String password) {
        String result = null;
        try {
            byte[] byteMessage = password.getBytes(CHARACTERSET_NAME_UTF_8);
            MessageDigest md = MessageDigest.getInstance(TYPE_OF_MESSAGE_DIGIT);
            byte[] resByte = md.digest(byteMessage);

            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < resByte.length; i++) {
                sb.append(Integer.toString((resByte[i] & 0xff) + 0x100, 16).substring(1));
            }
            result = sb.toString();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            LOGGER.error("Error while hash calculating due to exception {}", ex);
        }
        return result;
    }
}
