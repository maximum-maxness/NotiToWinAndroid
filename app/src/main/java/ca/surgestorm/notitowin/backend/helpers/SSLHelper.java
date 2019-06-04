package ca.surgestorm.notitowin.backend.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import org.spongycastle.asn1.x500.X500NameBuilder;
import org.spongycastle.asn1.x500.style.BCStyle;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.X509v3CertificateBuilder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.net.ssl.*;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Objects;

public class SSLHelper {
    private static final BouncyCastleProvider BOUNCY_CASTLE_PROVIDER = new BouncyCastleProvider();
    public static X509Certificate certificate;

    public static void initCertificate(Context context) {
        PrivateKey privKey;
        PublicKey pubKey;

        try {
            privKey = RSAHelper.getPrivateKey(context);
            pubKey = RSAHelper.getPublicKey(context);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        if (!settings.contains("certificate")) {
            try {
                X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
                InetAddress myHost = InetAddress.getLocalHost();
                String deviceName = myHost.getHostName();
                nameBuilder.addRDN(BCStyle.CN, deviceName);
                nameBuilder.addRDN(BCStyle.OU, "Network Test App");
                nameBuilder.addRDN(BCStyle.O, "Max");
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.YEAR, -1);
                Date notBefore = calendar.getTime();
                calendar.add(Calendar.YEAR, 10);
                Date notAfter = calendar.getTime();
                X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
                        nameBuilder.build(),
                        BigInteger.ONE,
                        notBefore,
                        notAfter,
                        nameBuilder.build(),
                        pubKey
                );
                ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSAEncryption").setProvider(BOUNCY_CASTLE_PROVIDER).build(privKey);
                certificate = new JcaX509CertificateConverter().setProvider(BOUNCY_CASTLE_PROVIDER).getCertificate(certificateBuilder.build(contentSigner));
                SharedPreferences.Editor edit = settings.edit();
                edit.putString("certificate", android.util.Base64.encodeToString(certificate.getEncoded(), 0));
                edit.apply();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Already Have Cert!");
            try {
                SharedPreferences globalSettings = PreferenceManager.getDefaultSharedPreferences(context);
                byte[] certificateBytes = android.util.Base64.decode(globalSettings.getString("certificate", ""), 0);
                X509CertificateHolder certificateHolder = new X509CertificateHolder(certificateBytes);
                certificate = new JcaX509CertificateConverter().setProvider(BOUNCY_CASTLE_PROVIDER).getCertificate(certificateHolder);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Cert Read Success!");
        }
    }

    public static boolean isCertificateStored(Context context, String deviceId) {
        SharedPreferences devicePreferences = context.getSharedPreferences(deviceId, Context.MODE_PRIVATE);
        String cert = devicePreferences.getString("certificate", "");
        assert cert != null;
        return !cert.isEmpty();
    }

    private static SSLContext getSSLContext(Context context, String clientID, boolean deviceHasBeenPaired) {
        try {
            PrivateKey privKey = RSAHelper.getPrivateKey(context);

            X509Certificate clientCert = null;
            if (deviceHasBeenPaired) {
                SharedPreferences devicePreferences = context.getSharedPreferences(clientID, Context.MODE_PRIVATE);
                byte[] certificateBytes = Base64.decode(devicePreferences.getString("certificate", ""), 0);
                X509CertificateHolder certificateHolder = new X509CertificateHolder(certificateBytes);
                clientCert = new JcaX509CertificateConverter().setProvider(BOUNCY_CASTLE_PROVIDER).getCertificate(certificateHolder);
            }

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setKeyEntry("key", privKey, "".toCharArray(), new Certificate[]{certificate});
            if (clientCert != null) {
                keyStore.setCertificateEntry(clientID, clientCert);
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, "".toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            TrustManager[] trustCerts = new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }};

            SSLContext tlsContext = SSLContext.getInstance("TLSv1");
            if (deviceHasBeenPaired) {
                tlsContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
            } else {
                tlsContext.init(kmf.getKeyManagers(), trustCerts, new SecureRandom());
            }
            return tlsContext;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void configureSSLSocket(SSLSocket socket, boolean deviceHasBeenPaired, boolean clientMode) throws SocketException {
        String[] supportedCiphers = {
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"
        };
        socket.setEnabledCipherSuites(supportedCiphers);
        socket.setSoTimeout(30000);

        if (clientMode) {
            socket.setUseClientMode(true);
        } else {
            socket.setUseClientMode(false);
            if (deviceHasBeenPaired) {
                socket.setNeedClientAuth(true);
            } else {
                socket.setWantClientAuth(true);
            }
        }
    }

    public static SSLSocket convertToSSLSocket(Context context, Socket socket, String clientID, boolean deviceHasBeenPaired, boolean clientMode) throws IOException {
        SSLSocketFactory ssf = Objects.requireNonNull(SSLHelper.getSSLContext(context, clientID, deviceHasBeenPaired)).getSocketFactory();
        SSLSocket sslSocket = (SSLSocket) ssf.createSocket(socket, socket.getInetAddress().getHostAddress(), socket.getPort(), true);
        SSLHelper.configureSSLSocket(sslSocket, deviceHasBeenPaired, clientMode);
        return sslSocket;
    }

    public static String getCertHash(Certificate certificate) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(certificate.getEncoded());
            Formatter formatter = new Formatter();
            int counter;
            for (counter = 0; counter < hash.length - 1; counter++) {
                formatter.format("%02x", hash[counter]);
            }
            formatter.format("%02x", hash[counter]);
            return formatter.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static Certificate parseCertificate(byte[] certBytes) throws IOException, CertificateException {
        X509CertificateHolder certificateHolder = new X509CertificateHolder(certBytes);
        return new JcaX509CertificateConverter().setProvider(BOUNCY_CASTLE_PROVIDER).getCertificate(certificateHolder);
    }
}
