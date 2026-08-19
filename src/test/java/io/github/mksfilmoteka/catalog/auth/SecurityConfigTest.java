package io.github.mksfilmoteka.catalog.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.github.mksfilmoteka.catalog.util.TestUtil.adminJwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityConfigTest.TestController.class)
@Import({SecurityConfig.class, KeycloakRealmRoleConverter.class, SecurityConfigTest.TestController.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowPublicGet() throws Exception {
        mockMvc.perform(get("/api/v1/test"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectUnauthenticatedWrite() throws Exception {
        mockMvc.perform(post("/api/v1/test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowPublicFilmCollectionPost() throws Exception {
        mockMvc.perform(post("/api/v1/films/collection"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectUserWrite() throws Exception {
        mockMvc.perform(post("/api/v1/test")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminWrite() throws Exception {
        mockMvc.perform(post("/api/v1/test")
                        .with(adminJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowSwaggerUiAndApiDocs() throws Exception {
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mockMvc.perform(get("/api-docs")).andExpect(status().isOk());
        mockMvc.perform(get("/api-docs/films")).andExpect(status().isOk());
    }

    @RestController
    static class TestController {

        @GetMapping("/api/v1/test")
        String get() {
            return "ok";
        }

        @PostMapping("/api/v1/test")
        String post() {
            return "ok";
        }

        @PostMapping("/api/v1/films/collection")
        String collectionPost() {
            return "ok";
        }

        @GetMapping({"/swagger-ui.html", "/swagger-ui/index.html", "/api-docs", "/api-docs/{group}"})
        String swagger() {
            return "ok";
        }
    }
}
