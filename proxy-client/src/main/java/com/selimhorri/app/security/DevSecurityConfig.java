package com.selimhorri.app.security;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Configuration
@EnableWebSecurity
@Profile("dev")
public class DevSecurityConfig extends WebSecurityConfigurerAdapter {
	
	@Override
	protected void configure(final HttpSecurity http) throws Exception {
		http
			.cors().disable()
			.csrf().disable()
			.authorizeRequests()
				.anyRequest().permitAll()
			.and()
			.sessionManagement()
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			.and()
			.headers()
				.frameOptions()
				.sameOrigin();
	}
    
	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return (Authentication authentication) -> {
			// Acepta cualquier usuario en dev y le asigna ROLE_USER
			return new UsernamePasswordAuthenticationToken(
				authentication.getPrincipal(),
				authentication.getCredentials(),
				Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
			);
		};
	}
	
}
