package com.chessmate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(
    scanBasePackages = "com.chessmate"
)
@EnableJpaRepositories(basePackages = "com.chessmate.infra_persistence.jpaRepository")
@EntityScan(basePackages = "com.chessmate.infra_persistence.entity")
public class ChessmateApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(ChessmateApiApplication.class, args);
  }
}
