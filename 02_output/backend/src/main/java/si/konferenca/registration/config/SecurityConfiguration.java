package si.konferenca.registration.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Organizer-only export over HTTP Basic, everything else public; security headers (S9, S10). */
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

  static final String ORGANIZER_PATHS = "/api/organizer/**";

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, AppProperties properties) {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers(ORGANIZER_PATHS).hasRole("ORGANIZER").anyRequest().permitAll())
        .httpBasic(Customizer.withDefaults())
        .csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(
            headers ->
                headers
                    .contentSecurityPolicy(
                        csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                    .frameOptions(frame -> frame.deny())
                    .referrerPolicy(
                        referrer ->
                            referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)));
    List<String> origins = properties.corsAllowedOrigins();
    if (origins != null && origins.stream().anyMatch(o -> !o.isBlank())) {
      http.cors(cors -> cors.configurationSource(corsSource(origins)));
    }
    return http.build();
  }

  private static CorsConfigurationSource corsSource(List<String> origins) {
    CorsConfiguration cors = new CorsConfiguration();
    cors.setAllowedOrigins(origins.stream().filter(o -> !o.isBlank() && !"*".equals(o)).toList());
    cors.setAllowedMethods(List.of("GET", "POST"));
    cors.setAllowedHeaders(List.of("Content-Type", "Authorization"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", cors);
    return source;
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** The one organizer; the password is kept only as a BCrypt hash (SB-03). */
  @Bean
  UserDetailsService organizers(AppProperties properties, PasswordEncoder encoder) {
    return new InMemoryUserDetailsManager(
        User.withUsername(properties.organizer().username())
            .password(encoder.encode(properties.organizer().password()))
            .roles("ORGANIZER")
            .build());
  }
}
