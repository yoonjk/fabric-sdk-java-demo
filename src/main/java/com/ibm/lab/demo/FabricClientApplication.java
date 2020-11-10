package com.ibm.lab.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FabricClientApplication {

	
	private static final Logger logger = LoggerFactory.getLogger(FabricClientApplication.class);
	
	public static void main(String[] args) {
		SpringApplication.run(FabricClientApplication.class, args);
	}
}
