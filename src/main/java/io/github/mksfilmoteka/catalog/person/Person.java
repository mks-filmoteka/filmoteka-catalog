package io.github.mksfilmoteka.catalog.person;

import io.github.mksfilmoteka.catalog.common.BaseEntity;
import io.github.mksfilmoteka.catalog.film.Film;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "person", uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
public class Person extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "death_date")
    private LocalDate deathDate;

    @Column(name = "place_of_birth")
    private String placeOfBirth;

    @Column(name = "photo_name", length = 1000)
    private String photoName;

    @ManyToMany(mappedBy = "actors")
    private List<Film> actingFilms = new ArrayList<>();

    @ManyToMany(mappedBy = "directors")
    private List<Film> directingFilms = new ArrayList<>();
}
