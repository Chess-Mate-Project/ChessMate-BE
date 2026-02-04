package com.chessmate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChessmateWorkerApplication {

  public static void main(String[] args) {
    SpringApplication.run(ChessmateWorkerApplication.class, args);
  }

}
