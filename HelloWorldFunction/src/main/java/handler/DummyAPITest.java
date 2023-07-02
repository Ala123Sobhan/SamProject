package handler;
import static io.restassured.RestAssured.given;

import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;


public class DummyAPITest {


    @Test
    public void testAddPlaceGoogleApi() {

        System.out.println("testAddPlaceGoogleApi:");
        AddAPlace ap = new AddAPlace();
        location l = new location();
        l.setLat(-38.383494);
        l.setLng(33.435353);

        ap.setLocation(l);
        ap.setAccuracy(60);
        ap.setName("1567 metro north bookiyklo");
        ap.setPhone_number("9175610897");
        ap.setAddress("28, side layout boo");
        List <String>list = Arrays.asList("shop", "shoe market");
        ap.setTypes(list);
        ap.setWebsite("http://google.com");
        ap.setLanguage("Bengali");

        RequestSpecification requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON)
                .setBaseUri("https://rahulshettyacademy.com")
                .addQueryParam("key", "qaclick123")
                .build();


        ResponseSpecification responseSpec = new ResponseSpecBuilder().expectStatusCode(200).expectContentType(ContentType.JSON)
                .build();



        String res = given().log().all().spec(requestSpec)
                .body(ap)
                .when()
                .post("/maps/api/place/add/json")
                .then().spec(responseSpec).extract().asString();

        System.out.println(res);

    }
}
