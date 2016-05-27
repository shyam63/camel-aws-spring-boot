package com.shyam.camel.config;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;

@Configuration
public class BeanConfigurations {

    private AWSCredentials awsCredentials = new BasicAWSCredentials("AKIAIVBJUYL7PEHQFFYQ",
            "eEXIpVzTiR+Ery26YvAM9ahjAgXUn8cnKWeCRvmz");

    @Bean public RandomDataGenerator randomDataGenerator() {
        return new RandomDataGenerator();
    }

    @Bean public AmazonS3 amazonS3Client() {
        AmazonS3 client = new AmazonS3Client(awsCredentials);
        return client;
    }

    @Bean public AmazonSQS amazonSQSClient() {
        AmazonSQS client = new AmazonSQSClient(awsCredentials);
        return client;
    }
}
