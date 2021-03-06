package com.shyam.camel.routes;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.s3.S3Constants;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class WebToS3Router extends RouteBuilder {

    @Autowired
    private RandomDataGenerator randomDataGenerator;

    @Override
    public void configure() {
        from("timer:trigger?period=12h")
                .routeId("RandomTextGeneratorRoute")
                .process(exchange -> {
                    StringBuilder stringBuilder = new StringBuilder();
                    for(int i=0;i<1000;i++) {
                        String str = RandomStringUtils.randomAlphabetic(randomDataGenerator.nextInt(1, 10));
                        stringBuilder.append(str).append(" ");
                    }
                    exchange.getIn().setBody(stringBuilder.toString());
                })
                .log(LoggingLevel.INFO, "Started Uploading to S3 bucket")
                //set the required headers for file upload to aws s3
                .setHeader(S3Constants.CONTENT_LENGTH, simple("${body.length}"))
                .setHeader(S3Constants.KEY, constant("random-text.txt"))
                .setHeader(S3Constants.CONTENT_TYPE, constant("text/plain"))
                .setHeader(S3Constants.CONTENT_ENCODING, constant("UTF-8"))
                .setHeader(S3Constants.CANNED_ACL,constant("PublicRead"))
                .log(LoggingLevel.INFO,"File name used to upload : random-text.txt")
                .to("aws-s3://shyam-test-bucket?amazonS3Client=#amazonS3Client")
                .log("Completed uploading to s3 bucket");

    }
}
