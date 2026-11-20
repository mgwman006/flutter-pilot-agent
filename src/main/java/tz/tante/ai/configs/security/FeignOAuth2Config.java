package tz.tante.ai.configs.security;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

@Configuration
public class FeignOAuth2Config
{
  private static final String CLIENT_REGISTRATION_ID = "property-manager-client";
  private static final String PRINCIPAL = "property-manager-client";

  @Bean
  public RequestInterceptor feignOAuth2RequestInterceptor(OAuth2AuthorizedClientManager authorizedClientManager)
  {
    return requestTemplate -> {
      OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
        .withClientRegistrationId(CLIENT_REGISTRATION_ID)
        .principal(PRINCIPAL)
        .build();

      OAuth2AuthorizedClient client = authorizedClientManager.authorize(authorizeRequest);
      if (client == null || client.getAccessToken() == null)
      {
        throw new IllegalStateException("Unable to acquire OAuth access token for Property Manager client");
      }

      requestTemplate.header("Authorization", "Bearer " + client.getAccessToken().getTokenValue());
    };
  }
}
