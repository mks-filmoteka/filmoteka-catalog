package io.github.mksfilmoteka.catalog.film;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.exc.InvalidFormatException;

@Schema(description = "Film countries of origin")
public enum Country {
    UNITED_STATES("United States"),
    UNITED_KINGDOM("United Kingdom"),
    FRANCE("France"),
    GERMANY("Germany"),
    ITALY("Italy"),
    SPAIN("Spain"),
    POLAND("Poland"),
    RUSSIA("Russia"),
    SOVIET_UNION("Soviet Union"),
    CZECH_REPUBLIC("Czech Republic"),
    UKRAINE("Ukraine"),
    BELARUS("Belarus"),
    FINLAND("Finland"),
    SWEDEN("Sweden"),
    IRELAND("Ireland"),
    BULGARIA("Bulgaria"),
    HUNGARY("Hungary"),
    JAPAN("Japan"),
    SOUTH_KOREA("South Korea"),
    CHINA("China"),
    THAILAND("Thailand"),
    INDIA("India"),
    INDONESIA("Indonesia"),
    CANADA("Canada"),
    AUSTRALIA("Australia"),
    NEW_ZEALAND("New Zealand"),
    MEXICO("Mexico"),
    BRAZIL("Brazil"),
    NETHERLANDS("Netherlands"),
    SOUTH_AFRICA("South Africa"),
    ARGENTINA("Argentina");

    private final String jsonValue;

    Country(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String getJsonValue() {
        return jsonValue;
    }

    @JsonCreator
    public static Country fromJson(String value) {
        if (value == null) {
            return null;
        }
        for (Country country : values()) {
            if (country.name().equals(value) || country.jsonValue.equals(value)) {
                return country;
            }
        }
        throw InvalidFormatException.from(null, "Invalid country value", value, Country.class);
    }
}
