package io.github.getsixtyfour.openpyn.security;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings.Secure;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class SecurityManager {

    private static final int IV_LENGTH = 16;

    private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";

    private static volatile SecurityManager sInstance = null;

    private SecretKey mKey;

    @NonNull
    public static SecurityManager getInstance(@NonNull Context context) {
        if (sInstance == null) {
            synchronized (SecurityManager.class) {
                if (sInstance == null) {
                    sInstance = new SecurityManager(context);
                }
            }
        }
        return sInstance;
    }

    @SuppressWarnings("unused")
    private SecurityManager() {
        if (sInstance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    @SuppressLint("HardwareIds")
    private SecurityManager(Context context) {
        String androidId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
        try {
            byte[] key = androidId.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, IV_LENGTH);
            mKey = new SecretKeySpec(key, "AES");
        } catch (NoSuchAlgorithmException e) {
            Log.wtf(getClass().getSimpleName(), e);
        }
    }

    @NonNull
    @SuppressWarnings("WeakerAccess")
    public String decryptString(@NonNull String stringToDecrypt) {
        String output = stringToDecrypt;
        try {
            byte[] encryptedBytes = Base64.decode(stringToDecrypt, Base64.DEFAULT);
            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(encryptedBytes, 0, IV_LENGTH);
            cipher.init(Cipher.DECRYPT_MODE, mKey, ivSpec);
            byte[] cipherBytes = cipher.doFinal(encryptedBytes, IV_LENGTH, encryptedBytes.length - IV_LENGTH);
            output = new String(cipherBytes, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
            Log.wtf(getClass().getSimpleName(), e);
        }
        return output;
    }

    @NonNull
    @SuppressWarnings("WeakerAccess")
    public String encryptString(@NonNull String stringToEncrypt) {
        String output = stringToEncrypt;
        try {
            byte[] clearText = stringToEncrypt.getBytes(StandardCharsets.UTF_8);
            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, mKey, ivSpec);
            byte[] cipherBytes = cipher.doFinal(clearText);
            output = new String(Base64.encode(concat(iv, cipherBytes), Base64.NO_WRAP), StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
            Log.wtf(getClass().getSimpleName(), e);
        }
        return output;
    }

    private static byte[] concat(byte[]... arrays) {
        int length = 0;
        for (byte[] array : arrays) {
            length += array.length;
        }
        byte[] result = new byte[length];
        int pos = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, pos, array.length);
            pos += array.length;
        }
        return result;
    }
}
