package com.oath2.oath20.service;

import com.amazonaws.services.cloudfront.CloudFrontUrlSigner;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.Security;
import java.time.Instant;
import java.util.Date;

@Service
public class CloudFrontService {
    private static final Logger logger = LoggerFactory.getLogger(CloudFrontService.class);

    private final String distributionDomain;
    private final String keyPairId;
    private final String privateKeyPath;
    private PrivateKey privateKey;

    public CloudFrontService(@Value("${aws.cloudfront.distribution-domain}") String distributionDomain,
                             @Value("${aws.cloudfront.key-pair-id}") String keyPairId,
                             @Value("${aws.cloudfront.private-key-path}") String privateKeyPath) {
        this.distributionDomain = distributionDomain;
        this.keyPairId = keyPairId;
        this.privateKeyPath = privateKeyPath;
    }

    public String getSignedUrl(String s3ObjectKey) {
        try {
            String resourceUrl = "https://" + distributionDomain + "/" + s3ObjectKey;
            Date expirationDate = Date.from(Instant.now().plusSeconds(3600)); // URL valid for 1 hour

            String signedUrl = CloudFrontUrlSigner.getSignedURLWithCannedPolicy(
                    resourceUrl, keyPairId, privateKey, expirationDate);

            logger.info("Generated signed URL for object: {}", s3ObjectKey);
            return signedUrl;
        } catch (Exception e) {
            logger.error("Failed to generate signed URL for object: {}", s3ObjectKey, e);
            throw new RuntimeException("Failed to generate signed URL", e);
        }
    }

    private PrivateKey loadPrivateKey() {
        try {
            // Add Bouncy Castle as a security provider
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

            // Read the private key file
            try (FileReader keyReader = new FileReader(privateKeyPath);
                 PEMParser pemParser = new PEMParser(keyReader)) {

                Object object = pemParser.readObject();
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

                if (object instanceof PEMKeyPair) {
                    PEMKeyPair keyPair = (PEMKeyPair) object;
                    PrivateKey privateKey = converter.getPrivateKey(keyPair.getPrivateKeyInfo());
                    logger.info("Successfully loaded CloudFront private key");
                    return privateKey;
                } else {
                    throw new IllegalStateException("Private key file does not contain a valid PEM encoded private key");
                }
            }
        } catch (IOException e) {
            logger.error("Failed to load CloudFront private key", e);
            throw new RuntimeException("Failed to load CloudFront private key", e);
        }
    }
}