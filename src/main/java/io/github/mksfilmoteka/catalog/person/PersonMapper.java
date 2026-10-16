package io.github.mksfilmoteka.catalog.person;

import io.github.mksfilmoteka.catalog.person.dto.PersonRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PersonMapper {
    Person personRequestToPerson(PersonRequest request);
}
