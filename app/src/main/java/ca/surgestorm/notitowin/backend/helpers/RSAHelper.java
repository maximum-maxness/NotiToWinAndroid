package ca.surgestorm.notitowin.backend.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import ca.surgestorm.notitowin.backend.JSONConverter;
import org.json.JSONArray;
import org.json.JSONException;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static javax.crypto.Cipher.*;

public class RSAHelper {

    public static void initKeys(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        if (!settings.contains("publicKey") || !settings.contains("privateKey")) {
            System.out.println("Initing Keys...");
            KeyPair keyPair = generateKeyPair();
            if (keyPair == null) return;
            byte[] publicKey = keyPair.getPublic().getEncoded();
            byte[] privateKey = keyPair.getPrivate().getEncoded();
            SharedPreferences.Editor edit = settings.edit();
            edit.putString("publicKey", android.util.Base64.encodeToString(publicKey, 0).trim() + "\n");
            edit.putString("privateKey", android.util.Base64.encodeToString(privateKey, 0));
            edit.apply();
        } else {
            System.out.println("Already have keys!");
        }
    }

    public static PublicKey getPublicKey(Context context) throws GeneralSecurityException {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        byte[] publicKeyBytes = android.util.Base64.decode(settings.getString("publicKey", ""), 0);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
    }

    public static PrivateKey getPrivateKey(Context context) throws GeneralSecurityException {
        SharedPreferences globalSettings = PreferenceManager.getDefaultSharedPreferences(context);
        byte[] privateKeyBytes = android.util.Base64.decode(globalSettings.getString("privateKey", ""), 0);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
    }

    public static JSONConverter encrypt(JSONConverter json, PublicKey pubKey) throws GeneralSecurityException {
        String jsonStr = json.serialize();

        Cipher cipher = getInstance("RSA/ECB/PKCS1PADDING");
        cipher.init(ENCRYPT_MODE, pubKey);

        int fragmentSize = 128; //The size of each encrypted fragment in amount of characters

        JSONArray arr = new JSONArray();
        while (jsonStr.length() > 0) {
            if (jsonStr.length() < fragmentSize) {
                fragmentSize = jsonStr.length();
            }
            String fragment = jsonStr.substring(0, fragmentSize);
            jsonStr = jsonStr.substring(fragmentSize);
            byte[] fragmentBytes = fragment.getBytes();
            byte[] encryptedFragment = cipher.doFinal(fragmentBytes); //encrypt fragment
            arr.put(Base64.getEncoder().encodeToString(encryptedFragment)); //add to the array of encrypted fragments
        }

        JSONConverter encryptedJson = new JSONConverter(PacketType.ENCRYPTED_PACKET);
        encryptedJson.set("data", arr);
        return encryptedJson;
    }

    public static JSONConverter decrypt(JSONConverter json, PrivateKey prvKey) throws GeneralSecurityException, JSONException {
        JSONArray fragments = json.getJSONArray("data");

        Cipher cipher = getInstance("RSA/ECB/PKCS1PADDING");
        cipher.init(DECRYPT_MODE, prvKey);

        StringBuilder decryptedStr = new StringBuilder();
        for (int i = 0; i < fragments.length(); i++) {
            byte[] encryptedFragment = Base64.getDecoder().decode(fragments.getString(i));
            String decryptedFragment = new String(cipher.doFinal(encryptedFragment));
            decryptedStr.append(decryptedFragment);
        }

        return JSONConverter.unserialize(decryptedStr.toString());
    }

    private static KeyPair generateKeyPair() {
        KeyPair keyPair;
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            keyPair = kpg.generateKeyPair();
            return keyPair;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }




}
