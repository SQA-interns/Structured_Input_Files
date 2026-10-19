package si.konferenca.registration.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/** Public API endpoints, the organizer export, deny-by-default for everything else (SB-02). */
@Configuration
public class SecurityConfiguration {

  static final String ORGANIZER = "ORGANIZER";

  @Bean
  SecurityFilterChain apiSecurity(
      HttpSecurity http, @Value("${app.organizer.https-only}") boolean httpsOnly) throws Exception {
    BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
    entryPoint.setRealmName("Organizer export");
    entryPoint.afterPropertiesSet();
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
        .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint))
        .addFilterBefore(new OrganizerHttpsOnlyFilter(httpsOnly), BasicAuthenticationFilter.class)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.GET, "/api/form-config")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/registrations")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, OrganizerHttpsOnlyFilter.EXPORT_PATH)
                    .hasRole(ORGANIZER)
                    .anyRequest()
                    .denyAll());
    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  /** The organizer account; only the BCrypt hash of the password is kept (SB-03). */
  @Bean
  UserDetailsService organizers(
      @Value("${app.organizer.username}") String username,
      @Value("${app.organizer.password}") String password,
      PasswordEncoder encoder) {
    return new InMemoryUserDetailsManager(
        User.withUsername(username).password(encoder.encode(password)).roles(ORGANIZER).build());
  }
}
