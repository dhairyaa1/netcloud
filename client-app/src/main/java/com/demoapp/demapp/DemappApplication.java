package com.demoapp.demapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan
public class DemappApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemappApplication.class, args);
	}

}
