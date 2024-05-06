package me.sonam.authzmanager.config;


import me.sonam.authzmanager.tokenfilter.JwtPath;
import me.sonam.authzmanager.tokenfilter.TokenFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@Profile("!localdevtest")
@Configuration
public class WebClientConfig {
    private static final Logger LOG = LoggerFactory.getLogger(WebClientConfig.class);
    @Value("${auth-server.root}${auth-server.oauth2token.path}")
    private String oauth2TokenEndpoint;
    @Value("${auth-server.oauth2token.grantType}")
    private String grantType;

    @Autowired
    private JwtPath jwtPath;

    @LoadBalanced
    @Bean("regular")
    public WebClient.Builder webClientBuilder() {
        LOG.info("returning load balanced webclient part");
        return WebClient.builder();
    }

    @LoadBalanced
    @Bean("tokenFilter")
    public WebClient.Builder webClientBuilderForTokenFilter() {
        LOG.info("returning load balanced webclient part");
        return WebClient.builder();
    }

    @LoadBalanced
    @Bean("webClientWithTokenFilter")
    public WebClient.Builder webClientBuilderNoFilter() {
        LOG.info("creating a WebClient.Builder with tokenFilter set");
        TokenFilter tokenFilter = new TokenFilter(webClientBuilderForTokenFilter(), jwtPath, oauth2TokenEndpoint, grantType);
        WebClient.Builder webClientBuilder = WebClient.builder();
        webClientBuilder.filter(tokenFilter.renewTokenFilter()).build();

        return webClientBuilder;
    }
}

  /*   @LoadBalanced
       @Bean("webClientWithTokenFilter")
       public WebClient.Builder webClientBuilderNoFilter() {
           LOG.info("creating a WebClient.Builder with tokenFilter set");
           WebClient.Builder webClientBuilder = webClientBuilderForTokenFilter();

           TokenFilter tokenFilter = new TokenFilter(webClientBuilder, jwtPath, oauth2TokenEndpoint);
           //WebClient.Builder webClientBuilder = WebClient.builder();
           webClientBuilder.filter(tokenFilter.renewTokenFilter()).build();

           return webClientBuilder;
       }
   */
