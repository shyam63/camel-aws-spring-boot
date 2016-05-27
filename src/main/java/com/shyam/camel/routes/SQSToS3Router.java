package com.shyam.camel.routes;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.s3.S3Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.shyam.camel.utils.Counter;

@Component
public class SQSToS3Router extends RouteBuilder {

    @Autowired
    private Counter counter;

    @Override
    public void configure() throws Exception {
        from("aws-sqs://shyam-test-queue?amazonSQSClient=#amazonSQSClient")
                .routeId("SQSToS3Route")
                .onCompletion()
                    .log(LoggingLevel.INFO,"incrementing the counter")
                    // on complete of this route, reset the file counts variable
                    .bean(counter,"incrementCount")
                .end()
                .process(exchange -> {
                    String fileName = "random-text-"+counter.getCount()+".txt";
                    exchange.setProperty("fileName",fileName);
                })
                .log(LoggingLevel.INFO, "Started Uploading to S3 bucket")
                //set the required headers for file upload to aws s3
                .setHeader(S3Constants.CONTENT_LENGTH, simple("${body.length}"))
                .setHeader(S3Constants.KEY, simple("${property.fileName}"))
                .setHeader(S3Constants.CONTENT_TYPE, constant("text/plain"))
                .setHeader(S3Constants.CONTENT_ENCODING, constant("UTF-8"))
                .setHeader(S3Constants.CANNED_ACL,constant("PublicRead"))
                .log(LoggingLevel.INFO,"File name used to upload : random-text.txt")
                .to("aws-s3://shyam-first-cli-bucket?amazonS3Client=#amazonS3Client")
                .log("Completed uploading to s3 bucket");
    }
}
