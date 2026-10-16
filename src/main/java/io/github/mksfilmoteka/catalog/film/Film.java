package io.github.mksfilmoteka.catalog.film;

import io.github.mksfilmoteka.catalog.common.BaseEntity;
import io.github.mksfilmoteka.catalog.person.Person;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Getter
@Setter
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "film",
        uniqueConstraints = @UniqueConstraint(columnNames = {"title", "release_year"})
)
public class Film extends BaseEntity {
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private String title;

    @Column(name = "release_year", nullable = false)
    private Integer releaseYear;

    @ElementCollection(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "film_countries",
            joinColumns = @JoinColumn(name = "film_id", nullable = false),
            uniqueConstraints = @UniqueConstraint(columnNames = {"film_id", "country"}),
            indexes = @Index(columnList = "country, film_id")
    )
    @Column(name = "country", nullable = false, length = 100)
    private List<Country> countries = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "film_genres",
            joinColumns = @JoinColumn(name = "film_id", nullable = false),
            uniqueConstraints = @UniqueConstraint(columnNames = {"film_id", "genre"}),
            indexes = @Index(columnList = "genre, film_id")
    )
    @Column(name = "genre", nullable = false, length = 100)
    private List<Genre> genres = new ArrayList<>();

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "poster_name", length = 1000)
    private String posterName;

    @ManyToMany
    @JoinTable(
            name = "film_actor",
            joinColumns = @JoinColumn(name = "film_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "person_id", nullable = false),
            indexes = @Index(columnList = "person_id")
    )
    @OrderColumn(name = "position")
    private List<Person> actors = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "film_director",
            joinColumns = @JoinColumn(name = "film_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "person_id", nullable = false),
            indexes = @Index(columnList = "person_id")
    )
    @OrderColumn(name = "position")
    private List<Person> directors = new ArrayList<>();

    public void addActor(Person person) {
        actors.add(person);
        person.getActingFilms().add(this);
    }

    public void removeActor(Person person) {
        actors.remove(person);
        person.getActingFilms().remove(this);
    }

    public void addDirector(Person person) {
        directors.add(person);
        person.getDirectingFilms().add(this);
    }

    public void removeDirector(Person person) {
        directors.remove(person);
        person.getDirectingFilms().remove(this);
    }
}