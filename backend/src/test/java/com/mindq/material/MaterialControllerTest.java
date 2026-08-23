package com.mindq.material;

import com.mindq.config.DotenvInitializer;
import com.mindq.model.User;
import com.mindq.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = DotenvInitializer.class)
@Transactional
class MaterialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String MATERIALS_URL = "/api/v1/materials";
    private static final String LOGIN_URL = "/api/v1/auth/login";

    @Test
    void shouldCreateMaterial() throws Exception {
        String token = registerAndLogin("create@example.com");

        mockMvc.perform(post(MATERIALS_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Java Collections",
                                  "content": "ArrayList implements List. HashMap implements Map."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Java Collections"))
                .andExpect(jsonPath("$.data.materialType").value("TEXT_PASTE"))
                .andExpect(jsonPath("$.data.wordCount").value(6))
                .andExpect(jsonPath("$.data.content").value("ArrayList implements List. HashMap implements Map."));
    }

    @Test
    void shouldRejectBlankCreate() throws Exception {
        String token = registerAndLogin("blank@example.com");

        mockMvc.perform(post(MATERIALS_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "content": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.title").exists())
                .andExpect(jsonPath("$.data.content").exists());
    }

    @Test
    void shouldRequireAuthForMaterials() throws Exception {
        mockMvc.perform(post(MATERIALS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "T", "content": "C"}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(MATERIALS_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldIsolateMaterialsPerUser() throws Exception {
        String tokenA = registerAndLogin("user-a@example.com");
        String tokenB = registerAndLogin("user-b@example.com");

        createMaterial(tokenA, "A1");
        createMaterial(tokenA, "A2");
        createMaterial(tokenB, "B1");

        mockMvc.perform(get(MATERIALS_URL).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2));

        mockMvc.perform(get(MATERIALS_URL).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("B1"));
    }

    @Test
    void shouldGetOwnMaterialButNotOthers() throws Exception {
        String tokenA = registerAndLogin("get-a@example.com");
        String tokenB = registerAndLogin("get-b@example.com");
        long idA = createMaterial(tokenA, "OwnedA");

        // Own material -> 200 with full content.
        mockMvc.perform(get(MATERIALS_URL + "/" + idA).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("OwnedA"))
                .andExpect(jsonPath("$.data.content").value("content-OwnedA"));

        // Someone else's material -> 404 (not revealed).
        mockMvc.perform(get(MATERIALS_URL + "/" + idA).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Material not found"));

        // Nonexistent id -> 404.
        mockMvc.perform(get(MATERIALS_URL + "/999999").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateOwnMaterial() throws Exception {
        String token = registerAndLogin("update@example.com");
        long id = createMaterial(token, "Original");

        mockMvc.perform(put(MATERIALS_URL + "/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Title",
                                  "content": "one two three four five six seven eight"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated Title"))
                .andExpect(jsonPath("$.data.wordCount").value(8));
    }

    @Test
    void shouldNotUpdateOthersMaterial() throws Exception {
        String tokenA = registerAndLogin("upd-a@example.com");
        String tokenB = registerAndLogin("upd-b@example.com");
        long id = createMaterial(tokenA, "Secret");

        mockMvc.perform(put(MATERIALS_URL + "/" + id)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Hacked", "content": "x"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteOwnMaterial() throws Exception {
        String token = registerAndLogin("delete@example.com");
        long id = createMaterial(token, "ToDelete");

        mockMvc.perform(delete(MATERIALS_URL + "/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(MATERIALS_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    void shouldNotDeleteOthersMaterial() throws Exception {
        String tokenA = registerAndLogin("del-a@example.com");
        String tokenB = registerAndLogin("del-b@example.com");
        long id = createMaterial(tokenA, "Keep");

        mockMvc.perform(delete(MATERIALS_URL + "/" + id).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    /** Creates a material and returns its id. */
    private long createMaterial(String token, String title) throws Exception {
        String body = mockMvc.perform(post(MATERIALS_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "content": "content-%s"
                                }
                                """.formatted(title, title)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return new ObjectMapper().readTree(body).path("data").path("id").asLong();
    }

    /** Creates a user in the DB and returns a token obtained via the login API. */
    private String registerAndLogin(String email) throws Exception {
        userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .fullName("Material User")
                .build());

        String responseBody = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "password123"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(responseBody);
        return root.path("data").path("token").asText();
    }
}
