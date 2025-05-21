package com.selimhorri.app.integration;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.hamcrest.Matchers.containsString;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CloudConfigApplicationTests {

	@LocalServerPort
	private int port;

	// Define a Eureka container that will be started before tests
	@Container
	static GenericContainer<?> eurekaServer = new GenericContainer<>(DockerImageName.parse("ecommerce-microservice-backend-app-service-discovery-container:latest"))
			.withExposedPorts(8761);

	// Dynamically set the Eureka server URL for the application properties
	@DynamicPropertySource
	static void eurekaProperties(DynamicPropertyRegistry registry) {
		String eurekaUrl = "http://" + eurekaServer.getHost() + ":" + eurekaServer.getMappedPort(8761) + "/eureka/";
		registry.add("eureka.client.serviceUrl.defaultZone", () -> eurekaUrl);
		// Disable config server bootstrap for this test as we are testing Eureka connection, not config serving
		registry.add("spring.cloud.config.server.bootstrap", () -> "false");
		// Ensure the application attempts to register with Eureka
		registry.add("eureka.client.register-with-eureka", () -> "true");
		registry.add("eureka.client.fetch-registry", () -> "true");
		// Speed up registration for tests
		registry.add("eureka.instance.lease-renewal-interval-in-seconds", () -> "1");
		registry.add("eureka.instance.lease-expiration-duration-in-seconds", () -> "2");
	}

	@Test
	void cloudConfigRegistersWithEureka() throws InterruptedException {
		Thread.sleep(15000); // Increased wait time

		// Check Eureka's registered applications endpoint
		String eurekaApiUrl = "http://" + eurekaServer.getHost() + ":" + eurekaServer.getMappedPort(8761) + "/eureka/apps/CLOUD-CONFIG";
		System.out.println("Checking Eureka server at: " + eurekaApiUrl);

		RestAssured.given()
			.when()
			.get(eurekaApiUrl)
			.then()
			.statusCode(200)
			.body(containsString("<name>CLOUD-CONFIG</name>"));
		
		System.out.println("CLOUD-CONFIG service successfully registered with Eureka.");
	}

}






