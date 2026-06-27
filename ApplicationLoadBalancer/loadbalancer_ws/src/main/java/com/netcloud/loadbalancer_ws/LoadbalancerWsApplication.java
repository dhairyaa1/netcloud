package com.netcloud.loadbalancer_ws;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories
@EnableCaching
@ComponentScan("com.netcloud")
public class LoadbalancerWsApplication {

	public static void main(String[] args) {
		SpringApplication.run(LoadbalancerWsApplication.class, args);
	}

}
