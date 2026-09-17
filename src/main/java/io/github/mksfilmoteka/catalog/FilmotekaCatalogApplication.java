package io.github.mksfilmoteka.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FilmotekaCatalogApplication {

    static void main(String[] args) {
        SpringApplication.run(FilmotekaCatalogApplication.class, args);
    }

}
