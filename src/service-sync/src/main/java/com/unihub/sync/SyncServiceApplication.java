package com.unihub.sync;

import com.unihub.sync.service.StudentSyncService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SyncServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SyncServiceApplication.class, args);
    }

    // @Bean
    // public CommandLineRunner runSyncOnStartup(StudentSyncService syncService) {
    // return args -> {
    // System.out.println(">>> Triggering manual sync on startup...");
    // syncService.scheduleSync();
    // };
    // }
}
