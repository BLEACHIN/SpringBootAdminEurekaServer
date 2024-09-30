package com.bleachin.config;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

/**
 * web安全相关配置
 * 包括springboot admin登录相关配置
 * @author bleachin
 */
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    private final String adminContextPath; // 对应的配置项：spring.boot.admin.context-path

    public WebSecurityConfig(AdminServerProperties adminServerProperties) {
        this.adminContextPath = adminServerProperties.getContextPath();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // http.httpBasic()
        //         .and().authorizeRequests()
        //         .anyRequest().permitAll()
        //         .and().csrf().disable();

        // 登录成功之后重定向到applications页
        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl(adminContextPath + "/applications");

        http.authorizeRequests()
                // 授予对所有静态资源、登录页面的公共访问权限
                .antMatchers(adminContextPath + "/assets/**").permitAll()
                .antMatchers(adminContextPath + "/login").permitAll()
                // 这里根据你的实际情况暴露对应需要监控的服务
                .antMatchers("/actuator/**").permitAll()
                // 除了以上配置的，其他的请求必须进行身份验证
                .anyRequest().authenticated()
                .and()
                // 配置登录和注销
                .formLogin().loginPage(adminContextPath + "/login").successHandler(successHandler)
                .and()
                .logout().logoutUrl(adminContextPath + "/logout")
                .and()
                // 启用HTTP-Basic支持。这是Spring Boot Admin Client注册所必需的
                .httpBasic()
                .and()
                // 关闭跨站请求伪造，否则登录会失败
                .csrf().disable();

        // System.out.println("adminContextPath：" + adminContextPath);
        // SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        // successHandler.setTargetUrlParameter("redirectTo");
        // http.authorizeRequests()
        //         .antMatchers(adminContextPath + "/assets/**").permitAll()
        //         .antMatchers(adminContextPath + "/login").permitAll()
        //         .antMatchers(adminContextPath + "/eureka/**").permitAll()
        //         .anyRequest().authenticated()
        //         .and()
        //         .formLogin().loginPage(adminContextPath + "/login").successHandler(successHandler)
        //         .and()
        //         .logout().logoutUrl(adminContextPath + "/logout")
        //         .and()
        //         .httpBasic()
        //         .and()
        //         .csrf()
        //         .ignoringAntMatchers(adminContextPath + "/instances", adminContextPath + "/actuator/**")
        //         .disable();
    }
}