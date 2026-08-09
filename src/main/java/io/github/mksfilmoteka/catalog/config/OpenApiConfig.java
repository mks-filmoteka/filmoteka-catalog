package io.github.mksfilmoteka.catalog.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.method.HandlerMethod;

@Configuration
public class OpenApiConfig {

    static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI filmotekaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Filmoteka catalog API")
                        .version("v1")
                        .description("Movie management system API"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Keycloak bearer JWT. Write operations require the ADMIN realm role.")));
    }

    @Bean
    public OperationCustomizer adminWriteOperationCustomizer() {
        return (operation, handlerMethod) -> {
            if (isAdminWriteOperation(handlerMethod)) {
                operation.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
                ApiResponses responses = operation.getResponses();
                if (responses == null) {
                    responses = new ApiResponses();
                    operation.setResponses(responses);
                }
                addResponseIfMissing(responses, "401", "Unauthorized");
                addResponseIfMissing(responses, "403", "Forbidden - ADMIN role required");
            }
            return operation;
        };
    }

    private boolean isAdminWriteOperation(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(PostMapping.class)
                || handlerMethod.hasMethodAnnotation(PutMapping.class)
                || handlerMethod.hasMethodAnnotation(DeleteMapping.class);
    }

    private void addResponseIfMissing(ApiResponses responses, String responseCode, String description) {
        if (!responses.containsKey(responseCode)) {
            responses.addApiResponse(responseCode, new ApiResponse().description(description));
        }
    }

    @Bean
    public GroupedOpenApi filmApi() {
        return GroupedOpenApi.builder().group("films").pathsToMatch("/api/v1/films/**").build();
    }

    @Bean
    public GroupedOpenApi actorApi() {
        return GroupedOpenApi.builder().group("actors").pathsToMatch("/api/v1/actors/**").build();
    }

    @Bean
    public GroupedOpenApi directorApi() {
        return GroupedOpenApi.builder().group("directors").pathsToMatch("/api/v1/directors/**").build();
    }
}
