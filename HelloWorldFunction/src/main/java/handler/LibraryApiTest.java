package handler;

import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;

public class LibraryApiTest {
	//given - all details ( query params, header, body),
	//when - ( submit api (http method and resource) ,
	//then  - validate response
	
	String bookId;
	
	@Test(dataProvider = "BookData")
	public void addBook(String pisbn, String paisle) {
	
		System.out.println("addBook:");
		RestAssured.baseURI = "https://rahulshettyacademy.com";
		String res = given().header("Content-Type", "application/json")
		.body(Payload.bookInfo(pisbn, paisle))
				.log().all()
		.when().post("/Library/Addbook.php")
		.then().statusCode(200).extract().asString();
		
		JsonPath js = new JsonPath(res);
		bookId = js.getString("ID");
		System.out.println(res);
		System.out.println(bookId);
		
				
		
	}
	
	@Test(dependsOnMethods = {"addBook"})
	public void deleteBook() {

		System.out.println("deleteBook:");
		RestAssured.baseURI = "https://rahulshettyacademy.com";
		String res = given().header("Content-Type", "application/json")
				.body(Payload.getdeleteBook(bookId)).log().all()
	            .when().post("Library/DeleteBook.php")
	            .then().statusCode(200).extract().asString();
	
		JsonPath js = new JsonPath(res);
		String msg = js.getString("msg");
		System.out.println(res);
		System.out.println(msg);	
		
		
	}
	
	@Test(dependsOnMethods = {"deleteBook"})
	public void getBook() {
		System.out.println("getBook:");
		RestAssured.baseURI = "https://rahulshettyacademy.com";
		Response res = given().header("Content-Type", "application/json").queryParam("ID", bookId)
	            .log().all()
				.when().get("/Library/GetBook.php");

		res.then().log().all();

		//fail intentionally

		String res1 = res.getBody().asString();
		JsonPath js = new JsonPath(res1);
		System.out.println(res1);
		String msg = js.getString("msg");
		System.out.println(msg);

		int response = res.getStatusCode();
		Assert.assertTrue(response==200, "Status code should be 404 because book was deleted!");

		
	}
	
	
	@DataProvider(name = "BookData" )
	public Object[][] getInfo() {
		
		
		Object [][] obj = new Object[1][2];
		String aisle, isbn;
		Faker faker = new Faker();

		aisle = faker.number().digits(7);
		isbn = faker.aviation().aircraft();
		
		obj[0][0] = isbn;
		obj[0][1] = aisle;
		
//		aisle = faker.number().digits(7);
//		isbn = faker.aviation().aircraft();
//		obj[1][0] = isbn;
//		obj[1][1] = aisle;
//
//		aisle = faker.number().digits(7);
//		isbn = faker.aviation().aircraft();
//
//		obj[2][0] = isbn;
//		obj[2][1] = aisle;
		
		return obj;
	}
	
	
	

}
