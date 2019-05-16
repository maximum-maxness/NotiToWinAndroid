package ca.surgestorm.notitowin.controller.networking.helpers;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

import ca.surgestorm.notitowin.backend.JSONConverter;

import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;
import static javax.crypto.Cipher.getInstance;

public class RSAHelper {

    private static String PUBLIC_KEY_FILE;
    private static String PRIVATE_KEY_FILE;

    public static void initKeys() {
        PUBLIC_KEY_FILE = FileHelper.getStorePath() + "pubKey" + ".pub";
        PRIVATE_KEY_FILE = FileHelper.getStorePath() + "privKey" + ".pfx";
        KeyPair keyPair = generateKeyPair();
        storeKeys(keyPair);
    }

    public static PublicKey getPublicKey() throws Exception {
        return readPublicKey();
    }

    public static PrivateKey getPrivateKey() throws Exception {
        return readPrivateKey();
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
        KeyPair keyPair = null;
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            keyPair = kpg.generateKeyPair();
            return keyPair;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return keyPair;
        }
    }

    private static void storeKeys(KeyPair keyPair) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKeySpec pub = keyFactory.getKeySpec(keyPair.getPublic(), RSAPublicKeySpec.class);
            RSAPrivateKeySpec priv = keyFactory.getKeySpec(keyPair.getPrivate(), RSAPrivateKeySpec.class);

            saveToFile(PUBLIC_KEY_FILE, pub.getModulus(), pub.getPublicExponent());
            saveToFile(PRIVATE_KEY_FILE, priv.getModulus(), priv.getPrivateExponent());

        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private static void saveToFile(String fileName, BigInteger mod, BigInteger exp) {
        try (
                ObjectOutputStream oout = new ObjectOutputStream(
                        new BufferedOutputStream(
                                new FileOutputStream(
                                        FileHelper.verifyFilePath(fileName).getAbsoluteFile()
                                )))
        ) {
            oout.writeObject(mod);
            oout.writeObject(exp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static PublicKey readPublicKey() throws Exception {
        InputStream in = new FileInputStream(FileHelper.verifyFilePath(PUBLIC_KEY_FILE).getAbsoluteFile());
        try (ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(in))) {
            BigInteger m = (BigInteger) oin.readObject();
            BigInteger e = (BigInteger) oin.readObject();
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(m, e);
            KeyFactory fact = KeyFactory.getInstance("RSA");
            return fact.generatePublic(keySpec);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static PrivateKey readPrivateKey() throws Exception {
        InputStream in = new FileInputStream(FileHelper.verifyFilePath(PRIVATE_KEY_FILE).getAbsoluteFile());
        try (ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(in))) {
            BigInteger m = (BigInteger) oin.readObject();
            BigInteger e = (BigInteger) oin.readObject();
            RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(m, e);
            KeyFactory fact = KeyFactory.getInstance("RSA");
            return fact.generatePrivate(keySpec);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
