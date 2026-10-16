package io.github.mksfilmoteka.catalog.person;

import io.github.mksfilmoteka.catalog.film.FilmMapper;
import io.github.mksfilmoteka.catalog.person.dto.DetailedPersonResponse;
import io.github.mksfilmoteka.catalog.person.dto.PersonRequest;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = FilmMapper.class)
public interface PersonMapper {
    Person personRequestToPerson(PersonRequest request);

    void updatePersonRequestToPerson(PersonRequest request, @MappingTarget Person person);

    DetailedPersonResponse personToDetailedPersonResponse(Person person);
}
