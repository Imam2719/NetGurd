package NetGuard.Dashboard_Features_Backend.Config;

import NetGuard.Dashboard_Features_Backend.Security.DashboardJwtAuthenticationFilter;
import NetGuard.Dashboard_Features_Backend.Security.JwtAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Order(1) // CRITICAL: Higher priority than Login_Backend to avoid conflicts
public class DashboardSecurityConfig {

    private final DashboardJwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean(name = "dashboardSecurityFilterChain")
    public SecurityFilterChain dashboardSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CRITICAL: Only handle dashboard-specific paths
                .securityMatcher(
                        "/api/overview/**",
                        "/api/dashboard/**",
                        "/api/devices/**",
                        "/api/analytics/**",
                        "/api/monitoring/**",
                        "/api/networks/**"
                )
                // CRITICAL: Enable CORS FIRST
                .cors(cors -> cors.configurationSource(dashboardCorsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/overview/networks/health").permitAll()
                        .requestMatchers("/api/dashboard/health").permitAll()
                        // Protected endpoints
                        .requestMatchers("/api/overview/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/dashboard/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/devices/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/analytics/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/monitoring/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/networks/**").hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean(name = "dashboardCorsConfigurationSource")
    public CorsConfigurationSource dashboardCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow specific origins
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:3001",
                "http://127.0.0.1:3000",
                "http://localhost:5173", // Vite dev server
                "http://localhost:4173"  // Vite preview
        ));

        // Allow all HTTP methods including OPTIONS for preflight
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));

        // Allow all headers including Authorization
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // CRITICAL: Allow credentials for JWT token
        configuration.setAllowCredentials(true);

        // Expose necessary headers
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "Access-Control-Allow-Headers",
                "Access-Control-Allow-Origin", "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}