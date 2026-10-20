package com.mercadosolidario.config;

import com.mercadosolidario.security.CustomUserDetailsService;
import com.mercadosolidario.security.JwtAccessDeniedHandler;
import com.mercadosolidario.security.JwtAuthenticationEntryPoint;
import com.mercadosolidario.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Value("${cors.allowed-origins:http://127.0.0.1:5173,http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/set-password").permitAll()
                .requestMatchers("/api/auth/logout").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/donations").permitAll()
                
                // Swagger (development only)
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                
                // Protected endpoints - require authentication (excluding login/logout)
                .requestMatchers("/api/auth/me").authenticated()

                // Priority configuration and calculations are restricted at method level as well.
                .requestMatchers("/api/priority/**").hasAnyRole("ADMIN", "RH_TESOURARIA")
                .requestMatchers(HttpMethod.GET, "/api/families/priority", "/api/families/*/priority")
                    .hasAnyRole("ADMIN", "RH_TESOURARIA")

                .requestMatchers(HttpMethod.GET, "/api/donations/configuration")
                    .hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/donations/configuration")
                    .hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/donations", "/api/donations/*")
                    .hasAnyRole("ADMIN", "RH_TESOURARIA")
                .requestMatchers(HttpMethod.POST,
                    "/api/donations/*/confirm-manually",
                    "/api/donations/*/cancel",
                    "/api/donations/*/expire")
                    .hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/donations/products")
                    .hasAnyRole("ADMIN", "RH_TESOURARIA")
                .requestMatchers("/api/donations/**")
                    .hasAnyRole("ADMIN", "RH_TESOURARIA")

                // Operational distribution is allowed to ADMIN, RH/Tesouraria and DIACONO.
                .requestMatchers(HttpMethod.GET, "/api/distributions/**", "/api/families/*/distributions")
                    .hasAnyRole("ADMIN", "RH_TESOURARIA", "DIACONO")
                .requestMatchers(HttpMethod.POST, "/api/distributions/*/cancel")
                    .hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/distributions")
                    .hasAnyRole("ADMIN", "RH_TESOURARIA", "DIACONO")
                .requestMatchers("/api/distributions/**")
                    .hasAnyRole("ADMIN", "RH_TESOURARIA", "DIACONO")

                // Stock reads are available to operational roles; stock writes exclude DIACONO.
                .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/stock/**")
                    .hasAnyRole("ADMIN", "RH_TESOURARIA", "DIACONO")
                .requestMatchers("/api/products/**", "/api/stock/**")
                    .hasAnyRole("ADMIN", "RH_TESOURARIA")
                
                // Family endpoints - ADMIN, RH_TESOURARIA, DIACONO can read, ADMIN and RH_TESOURARIA can write
                .requestMatchers(HttpMethod.GET, "/api/families/**").hasAnyRole("ADMIN", "RH_TESOURARIA", "DIACONO")
                .requestMatchers(HttpMethod.POST, "/api/families").hasAnyRole("ADMIN", "RH_TESOURARIA")
                .requestMatchers(HttpMethod.PUT, "/api/families/**").hasAnyRole("ADMIN", "RH_TESOURARIA")
                .requestMatchers(HttpMethod.PATCH, "/api/families/**/status").hasAnyRole("ADMIN", "RH_TESOURARIA")
                
                // Family member endpoints - ADMIN, RH_TESOURARIA, DIACONO can read, ADMIN and RH_TESOURARIA can write
                .requestMatchers(HttpMethod.GET, "/api/families/*/members/**").hasAnyRole("ADMIN", "RH_TESOURARIA", "DIACONO")
                .requestMatchers(HttpMethod.POST, "/api/families/*/members").hasAnyRole("ADMIN", "RH_TESOURARIA")
                .requestMatchers(HttpMethod.PUT, "/api/families/*/members/**").hasAnyRole("ADMIN", "RH_TESOURARIA")
                .requestMatchers(HttpMethod.DELETE, "/api/families/*/members/**").hasAnyRole("ADMIN", "RH_TESOURARIA")
                
                // Admin only endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/tasks/assignable-users").hasAnyRole("ADMIN", "RH_TESOURARIA")
                .requestMatchers(HttpMethod.POST, "/api/tasks").hasAnyRole("ADMIN", "RH_TESOURARIA")
                .requestMatchers("/api/tasks/**").authenticated()
                
                // Import endpoints - ADMIN and RH_TESOURARIA
                .requestMatchers("/api/import/**").hasAnyRole("ADMIN", "RH_TESOURARIA")

                // Campaign audit: history and registrations readable by operational roles; imports exclude DIACONO.
                .requestMatchers(HttpMethod.GET, "/api/historical-beneficiaries/**", "/api/campaign/**")
                    .hasAnyRole("ADMIN", "RH_TESOURARIA", "DIACONO")
                .requestMatchers("/api/historical-beneficiaries/**", "/api/campaign/**")
                    .hasAnyRole("ADMIN", "RH_TESOURARIA")

                // Sponsors are managed by ADMIN and RH/Tesouraria only.
                .requestMatchers("/api/sponsors/**").hasAnyRole("ADMIN", "RH_TESOURARIA")

                // Caixa/transparência: ADMIN e RH/Tesouraria (Pastor entra como ADMIN).
                .requestMatchers("/api/cash/**").hasAnyRole("ADMIN", "RH_TESOURARIA")
                
                // RH/Tesouraria endpoints
                .requestMatchers("/api/rh/**").hasAnyRole("ADMIN", "RH_TESOURARIA")
                
                // Diácono endpoints
                .requestMatchers("/api/diacono/**").hasAnyRole("ADMIN", "RH_TESOURARIA", "DIACONO")
                
                // Any other request requires authentication
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toList()));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
