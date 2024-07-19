package com.oath2.oath20.service;

import com.amazonaws.services.cloudfront.CloudFrontUrlSigner;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Collectors;

@Service
public class CloudFrontService {
    private static final Logger logger = LoggerFactory.getLogger(CloudFrontService.class);

    private final String distributionDomain;
    private final String keyPairId;
    private final PrivateKey privateKey;

    public CloudFrontService(@Value("${aws.cloudfront.distribution-domain}") String distributionDomain,
                             @Value("${aws.cloudfront.key-pair-id}") String keyPairId,
                             @Value("${aws.cloudfront.private-key-path}") Resource privateKeyResource) {
        this.distributionDomain = distributionDomain.startsWith("http") ? distributionDomain : "https://" + distributionDomain;
        this.keyPairId = keyPairId;
        this.privateKey = loadPrivateKey(privateKeyResource);
    }

    public String getSignedUrl(String s3ObjectKey) {
        try {
            String resourceUrl = distributionDomain + "/" + s3ObjectKey;
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

    private PrivateKey loadPrivateKey(Resource privateKeyResource) {
        try {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

            logger.info("Loading private key from path: {}", privateKeyResource.getURI());

            // Reading and logging the content of the PEM file
            String pemContent;
            try (BufferedReader keyReader = new BufferedReader(new InputStreamReader(privateKeyResource.getInputStream()))) {
                pemContent = keyReader.lines().collect(Collectors.joining("\n"));
                logger.debug("PEM file content:\n{}", pemContent);
            } catch (IOException e) {
                logger.error("Error reading the private key file content", e);
                throw new RuntimeException("Error reading the private key file content", e);
            }

            // Parsing the PEM content
            try (BufferedReader pemReader = new BufferedReader(new InputStreamReader(privateKeyResource.getInputStream()));
                 PemReader pemParser = new PemReader(pemReader)) {

                PemObject pemObject = pemParser.readPemObject();
                if (pemObject == null) {
                    logger.error("Failed to read PEM object from file");
                    throw new IllegalStateException("Failed to read PEM object from file");
                }

                byte[] pemContentBytes = pemObject.getContent();
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pemContentBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

                logger.info("Successfully loaded CloudFront private key");
                return privateKey;
            } catch (Exception e) {
                logger.error("Failed to parse the private key from PEM file", e);
                throw new RuntimeException("Failed to parse the private key from PEM file", e);
            }
        } catch (IOException e) {
            logger.error("Failed to load CloudFront private key", e);
            throw new RuntimeException("Failed to load CloudFront private key", e);
        }
    }
}