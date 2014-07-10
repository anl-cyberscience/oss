package com.bcmcgroup.flare.crypto;

/*
Copyright 2014 BCMC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

/**
 * This class is used as a cryptographic utility.
 * 
 * @author		David Du <ddu@bcmcgroup.com>
 * @version		1.1
 *
 */
public class CryptoUtil {

    /**
     * Method used to retrieve the cert in the form byte array
     *
     * @param cert A File object containing the certificate
     * @return byte array of the cert
     */
    private byte[] getKeyData(File cert) {
        byte[] buffer = new byte[(int) cert.length()];
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(cert);
            fis.read(buffer);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return buffer;
    }

    /**
     * Method used to get the generated Public Key
     *
     * @param cert A File object containing the certificate
     * @return A PublicKey object containing the public key
     */
    public PublicKey getStoredPublicKey(File cert) {
        PublicKey publicKey = null;
        byte[] keydata = getKeyData(cert);
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        X509EncodedKeySpec encodedPublicKey = new X509EncodedKeySpec(keydata);
        try {
            publicKey = keyFactory.generatePublic(encodedPublicKey);
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return publicKey;
    }
}
