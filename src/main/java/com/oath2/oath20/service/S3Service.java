package com.oath2.oath20.service;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@Service
public class S3Service {
    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    private final AmazonS3 s3Client;
    private final String bucketName;

    public S3Service(@Value("${aws.s3.bucket-name}") String bucketName,
                     @Value("${aws.region}") String region,
                     @Value("${aws.access-key-id}") String accessKey,
                     @Value("${aws.secret-access-key}") String secretKey) {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        this.s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(region)
                .build();
        this.bucketName = bucketName;
        logger.info("Initialized S3 client with region: {} and bucket: {}", region, bucketName);
    }

//    public String generatePresignedUrl(String objectKey, HttpMethod httpMethod) {
//        try {
//            Date expiration = Date.from(Instant.now().plus(Duration.ofMinutes(15)));
//            GeneratePresignedUrlRequest generatePresignedUrlRequest =
//                    new GeneratePresignedUrlRequest(bucketName, objectKey)
//                            .withMethod(httpMethod)
//                            .withExpiration(expiration);
//            URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
//            logger.info("Generated pre-signed URL for object: {}", objectKey);
//            return url.toString();
//        } catch (AmazonServiceException e) {
//            logger.error("AmazonServiceException when generating pre-signed URL for object: {}", objectKey, e);
//            throw new RuntimeException("Failed to generate pre-signed URL", e);
//        } catch (SdkClientException e) {
//            logger.error("SdkClientException when generating pre-signed URL for object: {}", objectKey, e);
//            throw new RuntimeException("Failed to generate pre-signed URL", e);
//        }
//    }

    public String uploadFile(MultipartFile file) {
        validateJpgFile(file);
        String fileKey = generateUniqueFileKey(file.getOriginalFilename());
        try {
            byte[] compressedImageBytes = compressImage(file);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(compressedImageBytes.length);

            s3Client.putObject(bucketName, fileKey, new ByteArrayInputStream(compressedImageBytes), metadata);
            logger.info("Successfully uploaded file to S3: {}", fileKey);
            return fileKey;
        } catch (IOException e) {
            logger.error("IOException when uploading file to S3", e);
            throw new RuntimeException("Failed to upload file", e);
        } catch (AmazonServiceException e) {
            logger.error("AmazonServiceException when uploading file to S3", e);
            throw new RuntimeException("Failed to upload file", e);
        } catch (SdkClientException e) {
            logger.error("SdkClientException when uploading file to S3", e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    public void deleteFile(String objectKey) {
        try {
            s3Client.deleteObject(bucketName, objectKey);
            logger.info("Successfully deleted file from S3: {}", objectKey);
        } catch (AmazonServiceException e) {
            logger.error("AmazonServiceException when deleting file from S3: {}", objectKey, e);
            throw new RuntimeException("Failed to delete file", e);
        } catch (SdkClientException e) {
            logger.error("SdkClientException when deleting file from S3: {}", objectKey, e);
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    private String generateUniqueFileKey(String originalFilename) {
        return UUID.randomUUID().toString() + "-" + originalFilename;
    }

    private void validateJpgFile(MultipartFile file) {
        if (!"image/jpeg".equalsIgnoreCase(file.getContentType())) {
            throw new IllegalArgumentException("Only JPG files are allowed.");
        }
    }

    private byte[] compressImage(MultipartFile file) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        BufferedImage scaledImage = scaleImage(originalImage, 800, 600); // Change the resolution to 800x600
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(scaledImage, "jpg", baos);
        return baos.toByteArray();
    }

    private BufferedImage scaleImage(BufferedImage originalImage, int width, int height) {
        BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaledImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.drawImage(originalImage, 0, 0, width, height, null);
        graphics.dispose();
        return scaledImage;
    }
}