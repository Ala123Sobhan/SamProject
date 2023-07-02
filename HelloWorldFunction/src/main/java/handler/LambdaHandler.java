package handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;
import software.amazon.awssdk.services.ses.model.Destination;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class LambdaHandler implements RequestHandler<Object, String> {

	static final String S3_BUCKET_NAME = "api-tests-logs";
	final String SENDER_EMAIL_ADDRESS = "ala123sobhan@gmail.com";
	final String RECIPIENT_EMAIL_ADDRESS = "alasobhan.work@gmail.com";
	final String EMAIL_SUBJECT = "Test Results Log";
	final String EMAIL_BODY = "Please find attached the test results log file.";
	StringBuilder logsBuilder = new StringBuilder();

	@Override
	public String handleRequest(Object o, Context context) {

		String s3key = null;
		String executionLogs = null;
		TestResultListener resultListener = null;

		try {

			TestNG runner = new TestNG();
			// Create a list of String
			List<String> suitefiles = new ArrayList<String>();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			PrintStream printStream = new PrintStream(outputStream);
			System.setOut(printStream);
			System.setErr(printStream);

			// Add xml file which you have to execute
			suitefiles.add("handler/testng.xml");

			// now set xml file for execution
			runner.setTestSuites(suitefiles);

			// Create a custom TestListener to capture the test results
			 resultListener = new TestResultListener();

			// Add the custom listener to TestNG
			runner.addListener(resultListener);

			runner.run();

			// Get the captured logs
			executionLogs = outputStream.toString();

			// Get the captured test results
			String testResults = resultListener.getTestResults();

			// Log the test results to S3
			S3Client s3Client = S3Client.create();
			s3key = getKey();
			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
					.bucket(S3_BUCKET_NAME)
					.key(s3key)
					.build();

			PutObjectResponse putObjectResponse = s3Client.putObject(putObjectRequest, RequestBody.fromString(testResults+"\n\n\n"+executionLogs));
			//return "Tests run successfully! Test results logged to S3: " + putObjectResponse.eTag();

			//return "tests run successfully "+"\n"+logs;

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
			//return "Error occurred during test execution";
		}finally {
			if(resultListener.isTestFailed())
			sendEmailWithAttachment(s3key, executionLogs);
		}
			return "Done running the tests and sending email";
	}

	private String getKey() {

		String timestamp = String.valueOf(System.currentTimeMillis());
		String randomId = UUID.randomUUID().toString();
		String key = "logs/" + timestamp + "-" + randomId + ".log";
		return key;

	}

	private static class TestResultListener extends TestListenerAdapter {

		private StringBuilder testResultsBuilder = new StringBuilder();

		ExtentReports extent = ExtentReporter.getReportObject();
		ExtentTest test;
		ThreadLocal <ExtentTest> extentTest = new ThreadLocal<ExtentTest>();

		boolean testFailed = false;
		@Override
		public void onTestStart(ITestResult result) {
			// Log test start event
			String testName = result.getName();
			testResultsBuilder.append("Test Start: ").append(testName).append("\n\n");
			test = extent.createTest(result.getMethod().getMethodName());
			extentTest.set(test);
		}

		@Override
		public void onTestSuccess(ITestResult result) {
			// Log test success event
			String testName = result.getName();
			testResultsBuilder.append("Test Success: ").append(testName).append("\n\n");
			extentTest.get().log(Status.PASS, "Test Passed");

		}

		@Override
		public void onTestFailure(ITestResult result) {
			// Log test failure event
			String testName = result.getName();
			testResultsBuilder.append("Test Failure: ").append(testName).append("\n\n");
			testFailed = true;
			extentTest.get().fail(result.getThrowable());

		}

		@Override
		public void onFinish(ITestContext context) {
			// Log test execution summary
			testResultsBuilder.append("Total Tests: ").append(context.getAllTestMethods().length).append("\n");
			testResultsBuilder.append("Pass: ").append(context.getPassedTests().size()).append("\n");
			testResultsBuilder.append("Fail: ").append(context.getFailedTests().size()).append("\n");
			testResultsBuilder.append("Skip: ").append(context.getSkippedTests().size()).append("\n");
			extent.flush();
			ExtentReporter.uploadFile();
		}

		public String getTestResults() {
			return testResultsBuilder.toString();
		}

		public boolean isTestFailed() {
			return testFailed;
		}
	}


	public void sendEmailWithAttachment(String logKey, String executionLogs) {

		SesClient sesClient = SesClient.create();

		// Retrieve the log file content from S3
		String logFileContent = getLogFileContentFromS3(logKey);

		String logPresignedUrl = getPresignedUrl(S3_BUCKET_NAME,logKey);
		String reportPresignedUrl = getPresignedUrl(S3_BUCKET_NAME, ExtentReporter.s3key);
		// Generate the email body with attachment
		String emailBody = getEmailBodyWithAttachment(logKey, logFileContent, executionLogs, logPresignedUrl,reportPresignedUrl);


		// Create the email request
		SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
				.source(SENDER_EMAIL_ADDRESS)
				.destination(Destination.builder().toAddresses(RECIPIENT_EMAIL_ADDRESS).build())
				.message(Message.builder()
						.subject(Content.builder().data(EMAIL_SUBJECT).build())
						.body(Body.builder().text(Content.builder().data(emailBody).build()).build())
						.build())
				.build();

		// Send the email with the log file attachment
		sesClient.sendEmail(sendEmailRequest);
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



	private String getLogFileContentFromS3(String logKey) {
		S3Client s3Client = S3Client.create();
		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(S3_BUCKET_NAME)
				.key(logKey)
				.build();
		ResponseInputStream<GetObjectResponse> getObjectResponse = s3Client.getObject(getObjectRequest);
		try {
			byte[] contentBytes = IOUtils.toByteArray(getObjectResponse);

			String content = new String(contentBytes, StandardCharsets.UTF_8);
			return content;

		} catch (IOException e) {
			e.printStackTrace();
			// Handle the exception as needed
		} finally {
			// Close the ResponseInputStream
			try {
				getObjectResponse.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null; // or throw an exception if desired
	}

	private String getEmailBodyWithAttachment(String logKey, String logFileContent, String executionLogs, String presignedUrl1,
											  String presignedUrl2) {
		// Your code here to generate email body with attachment
		//return EMAIL_BODY;
		String emailBody = "Please find attached the test results log file."+"\n\n"+logFileContent
				+"\n\n\n\n"+
				//executionLogs+"\n\n"+
				"Log presigned url:\n"+presignedUrl1+"\n\n"+"Report presigned url:"+
				presignedUrl2;
		return emailBody;
	}





		}


//aws lambda create-function  --function-name LambdaApiTest --runtime java11  --handler handler.LambdaHandler::handleRequest --code S3Bucket=ala123sobhanbucket,S3Key=APITestingBDDApproach-1.0-SNAPSHOT.jar  --role arn:aws:iam::199055471403:role/lambdarole1 --profile ala123sobhan --timeout 300 --memory-size 512