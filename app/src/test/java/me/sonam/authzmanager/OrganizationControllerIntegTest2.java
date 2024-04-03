package me.sonam.authzmanager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@SpringBootTest*/
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
@SpringBootTest
@WithMockUser(username = "username", password = "password", roles = "USER")

public class OrganizationControllerIntegTest2 {
    private static final Logger LOG = LoggerFactory.getLogger(OrganizationControllerIntegTest2.class);

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void createOrganization() throws Exception {
        LOG.info("create organization with rest controller");
        // Create a post-processor that adds a JWT token to the request



        MvcResult mvcResult = mockMvc.perform(post("/admin/organizations")).andExpect(status().isOk()).andReturn();
        // Assert that the response is correct
        assertThat(mvcResult.getResponse().getContentAsString()).isNotNull();


    }

}
