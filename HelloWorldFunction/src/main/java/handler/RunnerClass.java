package handler;

import org.apache.logging.log4j.util.PropertySource;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.testng.TestNG;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.util.Collections;

public class RunnerClass {

    public static void main(String[] args) {
//        JUnitCore jUnitCore = new JUnitCore();
//        Result result = jUnitCore.run(IzaanSchoolAPITest.class);
//

        try {
//            TestNG testng = new TestNG();
//            testng.setTestClasses(new Class[]{DummyAPITest.class});
//            testng.run() ;


            //TestNG testng = new TestNG();
//			testng.setTestClasses(new Class[]{DummyAPITest.class});
//
//			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//			PrintStream printStream = new PrintStream(outputStream);
//			System.setOut(printStream);
//			System.setErr(printStream);
//
//
//			testng.run();
//
//			// Get the captured logs
//			String logs = outputStream.toString();
//
//			// Return the logs as part of the response
//			return logs;



        }catch(Exception e)
        {System.out.print(e.getMessage());}

        getPresignedUrl("api-tests-logs","logs/1687017763737-57d832a7-ee0a-4b04-b134-b0914f480fc4.log"  );


    }

    public static String getPresignedUrl(String bucketName, String keyName ) {

        String myURL = null;
        try {

            ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.create();
            Region region = Region.US_WEST_2;
            String profileName = "ala123sobhan";
            S3Presigner presigner = S3Presigner.builder()
                    .region(region)
                    .credentialsProvider(DefaultCredentialsProvider.builder()
                            .profileName(profileName)
                            .build())
                    .build();

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(keyName)
                    .build();

            GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(60))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedGetObjectRequest = presigner.presignGetObject(getObjectPresignRequest);
            myURL = presignedGetObjectRequest.url().toString();
            System.out.println("Presigned URL: " + myURL);

        }catch (Exception e) {
            e.printStackTrace();
        }
        return myURL;
    }
    }

