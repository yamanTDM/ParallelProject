package com.example.Parallel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ParallelApplication {

	public static void main(String[] args) {
		SpringApplication.run(ParallelApplication.class, args);
	}

}
