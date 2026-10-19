package si.konferenca.registration.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/** Public API endpoints and deny-by-default for everything else (SB-02). */
@Configuration
public class SecurityConfiguration {

  @Bean
  SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.GET, "/api/form-config")
                    .permitAll()
                    .anyRequest()
                    .denyAll());
    return http.build();
  }
}
