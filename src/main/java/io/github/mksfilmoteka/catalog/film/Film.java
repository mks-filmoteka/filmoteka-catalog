package io.github.mksfilmoteka.catalog.film;

import io.github.mksfilmoteka.catalog.actor.Actor;
import io.github.mksfilmoteka.catalog.common.BaseEntity;
import io.github.mksfilmoteka.catalog.director.Director;
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
            inverseJoinColumns = @JoinColumn(name = "actor_id", nullable = false),
            indexes = @Index(columnList = "actor_id")
    )
    private List<Actor> actors = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "film_director",
            joinColumns = @JoinColumn(name = "film_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "director_id", nullable = false),
            indexes = @Index(columnList = "director_id")
    )
    private List<Director> directors = new ArrayList<>();

    public void addActor(Actor actor) {
        actors.add(actor);
        actor.getFilms().add(this);
    }

    public void removeActor(Actor actor) {
        actors.remove(actor);
        actor.getFilms().remove(this);
    }

    public void addDirector(Director director) {
        directors.add(director);
        director.getFilms().add(this);
    }

    public void removeDirector(Director director) {
        directors.remove(director);
        director.getFilms().remove(this);
    }
}
