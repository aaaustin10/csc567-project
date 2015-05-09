package com.example.austin.clipboardsync;

import android.util.Base64;

import java.security.Key;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by austin on 5/3/15.
 */
public class Encrypter {
    Key aes_key;
    Cipher cipher;

    Encrypter(String plain_key) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(plain_key.getBytes());
            aes_key = new SecretKeySpec(md.digest(), "AES");
            cipher = Cipher.getInstance("AES");
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // courtesy of 'pointer null' on stack overflow
    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    public static String hash(byte[] contents) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return byteArrayToHex(md.digest(contents));
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    public String encrypt(String contents) {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, aes_key);
            byte[] encrypted = cipher.doFinal(contents.getBytes());
            return new String(Base64.encode(encrypted, Base64.DEFAULT));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String decrypt(String encoded_data) {
        try {
            cipher.init(Cipher.DECRYPT_MODE, aes_key);
            byte[] encrypted_data = Base64.decode(encoded_data, Base64.DEFAULT);
            String data = new String(cipher.doFinal(encrypted_data));
            return data;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
