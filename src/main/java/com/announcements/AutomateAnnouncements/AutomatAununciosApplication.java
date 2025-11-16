package com.announcements.AutomateAnnouncements;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AutomatAununciosApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutomatAununciosApplication.class, args);
	}

}
