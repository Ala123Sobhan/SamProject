package handler;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import org.apache.commons.io.output.ByteArrayOutputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;


public class ExtentReporter {

    public static ExtentReports extent;
    public static String bucketName = "api-tests-logs";
    public static String s3key = "reports/index.html";


    public static ExtentReports getReportObject() {
        // Create an instance of ExtentReports
        ExtentReports extent = new ExtentReports();

        // Create an instance of ExtentHtmlReporter
        ExtentHtmlReporter htmlReporter = new ExtentHtmlReporter("/tmp/index.html");
        htmlReporter.config().setReportName("API Test Results");
        htmlReporter.config().setDocumentTitle("Test Results");

        // Attach the reporter to ExtentReports
        extent.attachReporter(htmlReporter);
        extent.setSystemInfo("Tester", "Ala Sobhan");

        return extent;
    }


    public static void uploadFile() {


        // Upload the report file to S3
        S3Client s3Client = S3Client.builder().build();
        // Upload the report to S3
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3key)
                .build();
        PutObjectResponse putObjectResponse = s3Client.putObject(putObjectRequest,
                RequestBody.fromFile(new File("/tmp/index.html")));


    }

}
