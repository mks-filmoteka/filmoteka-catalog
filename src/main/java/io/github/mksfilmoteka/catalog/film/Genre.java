package io.github.mksfilmoteka.catalog.film;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.exc.InvalidFormatException;

@Schema(description = "Film genre")
public enum Genre {
    ACTION("Action"),
    ADVENTURE("Adventure"),
    ANIMATION("Animation"),
    BIOGRAPHY("Biography"),
    COMEDY("Comedy"),
    CRIME("Crime"),
    DISASTER("Disaster"),
    DOCUMENTARY("Documentary"),
    DRAMA("Drama"),
    FAMILY("Family"),
    FANTASY("Fantasy"),
    NOIR("Noir"),
    HISTORY("History"),
    HORROR("Horror"),
    MUSIC("Music"),
    MUSICAL("Musical"),
    MYSTERY("Mystery"),
    ROMANCE("Romance"),
    SCI_FI("Sci-Fi"),
    SPORT("Sport"),
    SPY("Spy"),
    THRILLER("Thriller"),
    WAR("War"),
    WESTERN("Western");

    private final String jsonValue;

    Genre(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String getJsonValue() {
        return jsonValue;
    }

    @JsonCreator
    public static Genre fromJson(String value) {
        if (value == null) {
            return null;
        }
        for (Genre genre : values()) {
            if (genre.name().equals(value) || genre.jsonValue.equals(value)) {
                return genre;
            }
        }
        throw InvalidFormatException.from(null, "Invalid genre value", value, Genre.class);
    }
}
