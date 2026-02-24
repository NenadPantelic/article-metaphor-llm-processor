package com.article.metaphor_llm_processor.api;

import com.article.metaphor_llm_processor.api.properties.AuthConfigProperties;
import com.article.metaphor_llm_processor.api.properties.UserDetailsConfigProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.article.metaphor_llm_processor.api", basePackageClasses = {
		AuthConfigProperties.class,
		UserDetailsConfigProperties.class
})
public class ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}

}
