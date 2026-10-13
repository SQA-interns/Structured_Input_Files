package si.konferenca.registration.security;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import si.konferenca.registration.RegistrationProperties;

/** Access rules, organizer login and security headers of the API (SB-02, SB-03, SB-10). */
@Configuration
public class SecurityConfiguration {

  static final String REALM = "organizer";
  static final String ORGANIZER = "ORGANIZER";

  @Bean
  public SecurityFilterChain apiSecurity(HttpSecurity http, RegistrationProperties properties)
      throws Exception {
    AuthenticationEntryPoint unauthorized =
        (request, response, e) -> {
          response.setHeader("WWW-Authenticate", "Basic realm=\"" + REALM + "\"");
          ErrorResponses.write(
              response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required.");
        };
    AccessDeniedHandler forbidden =
        (request, response, e) ->
            ErrorResponses.write(response, HttpServletResponse.SC_FORBIDDEN, "Access denied.");
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .cors(c -> c.configurationSource(cors(properties.cors().allowedOrigins())))
        .headers(
            h ->
                h.contentSecurityPolicy(
                        c -> c.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                    .referrerPolicy(
                        r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                    .frameOptions(f -> f.deny()))
        .httpBasic(b -> b.realmName(REALM).authenticationEntryPoint(unauthorized))
        .exceptionHandling(
            e -> e.authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden))
        .addFilterBefore(
            new HttpsOnlyFilter(properties.organizer().httpsOnly()),
            BasicAuthenticationFilter.class)
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(HttpMethod.GET, "/api/form-config")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/registrations")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/organizer/registrations.xlsx")
                    .hasRole(ORGANIZER)
                    .requestMatchers("/actuator/health", "/actuator/health/**", "/error")
                    .permitAll()
                    .anyRequest()
                    .denyAll());
    return http.build();
  }

  /** The single organizer account; the password is kept only as a BCrypt hash (SB-03). */
  @Bean
  public UserDetailsService organizers(RegistrationProperties properties, PasswordEncoder encoder) {
    RegistrationProperties.Organizer organizer = properties.organizer();
    if (isBlank(organizer.username()) || isBlank(organizer.password())) {
      throw new IllegalStateException("ORGANIZER_USERNAME and ORGANIZER_PASSWORD must be set");
    }
    return new InMemoryUserDetailsManager(
        User.withUsername(organizer.username())
            .password(encoder.encode(organizer.password()))
            .roles(ORGANIZER)
            .build());
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  static UrlBasedCorsConfigurationSource cors(List<String> allowedOrigins) {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    if (!allowedOrigins.isEmpty()) {
      CorsConfiguration config = new CorsConfiguration();
      config.setAllowedOrigins(allowedOrigins);
      config.setAllowedMethods(List.of("GET", "POST"));
      config.setAllowedHeaders(List.of("Content-Type", "Authorization"));
      source.registerCorsConfiguration("/api/**", config);
    }
    return source;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
