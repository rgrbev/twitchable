package com.twitchable.project.config;


import com.twitchable.project.authentication.LoginFailureHandler;
import com.twitchable.project.authentication.LoginSuccessHandler;
import com.twitchable.project.authentication.OAuth2TokenService;
import com.twitchable.project.authentication.OAuthClientResource;
import com.twitchable.project.model.Provider;
import com.twitchable.project.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.session.SessionManagementFilter;
import org.springframework.web.filter.CompositeFilter;
import org.springframework.web.filter.RequestContextFilter;

import javax.servlet.Filter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ristes on 3/9/16.
 */
@Configuration
@EnableWebSecurity
@EnableOAuth2Client
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    private final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    OAuth2ClientContext oauth2ClientContext;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
    }

    protected void configure(HttpSecurity http) throws Exception {
     http.csrf().disable();

        http.requiresChannel().anyRequest().requiresSecure();

        http.logout()
                .logoutUrl("/logout")
                .deleteCookies("JSESSIONID")
                .logoutSuccessUrl("/")
                .permitAll();


        http.formLogin()
                .loginPage("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .loginProcessingUrl("/doLogin")
                .failureHandler(loginFailureHandler())
                .successHandler(twitchSuccessHendler())
                .permitAll();


        http.authorizeRequests()
                .antMatchers("/me", "/oauth/authorize", "/oauth/token", "/user")
                .authenticated();

        http.authorizeRequests()
                .antMatchers("/admin/**")
                .hasRole("ADMIN");

        http.authorizeRequests()
                .antMatchers("/**")
                .permitAll();

        http.addFilterBefore(ssoFilter(), BasicAuthenticationFilter.class);
        http.addFilterAfter(oauth2AuthenticationFilter(), SessionManagementFilter.class);

    }

    private Filter oauth2AuthenticationFilter() throws Exception {
        OAuth2AuthenticationProcessingFilter filter = new OAuth2AuthenticationProcessingFilter();
        filter.setStateless(false);
        filter.setAuthenticationManager(authenticationManager());
        return filter;
    }

    @Bean
    public AuthenticationFailureHandler loginFailureHandler() {
        return new LoginFailureHandler();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder;
    }

    @Bean
    public FilterRegistrationBean oauth2ClientFilterRegistration(OAuth2ClientContextFilter filter) {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(filter);
        registration.setOrder(-100);
        return registration;
    }


    @Bean
    public FilterRegistrationBean requestContextFilter() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new RequestContextFilter());
        registration.setOrder(-105);
        return registration;
    }




    @Bean
    @ConfigurationProperties("twitch")
    OAuthClientResource twitch() {
        return new OAuthClientResource();
    }




    @Bean
    LoginSuccessHandler twitchSuccessHendler() {
        return new LoginSuccessHandler(Provider.TWITCH, Role.ROLE_USER);
    }


    private Filter ssoFilter() {

        CompositeFilter filter = new CompositeFilter();
        List<Filter> filters = new ArrayList<Filter>();



        filters.add(
                getOauthFilter(
                        "/login/twitch",
                        twitch(),
                        twitchSuccessHendler()
                )
        );

        filter.setFilters(filters);
        return filter;

    }

    public Filter getOauthFilter(
            String loginUrl,
            OAuthClientResource client,
            AuthenticationSuccessHandler successHandler) {
        OAuth2ClientAuthenticationProcessingFilter oauthFilter =
                new OAuth2ClientAuthenticationProcessingFilter(loginUrl);
        OAuth2RestTemplate template = new OAuth2RestTemplate(client.getClient(), oauth2ClientContext);
        oauthFilter.setRestTemplate(template);
        oauthFilter.setTokenServices(
                new OAuth2TokenService(
                        client.getResource().getUserInfoUri(),
                        client.getClient().getClientId()
                )
        );
        oauthFilter.setAuthenticationSuccessHandler(successHandler);
        return oauthFilter;
    }

}
