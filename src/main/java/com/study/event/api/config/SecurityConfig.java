package com.study.event.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// 스프링 시큐리티 설정 파일
// 인터셉터, 필터 처리 가능
// 세션 인증, 토큰 인증
// 권한 처리
// OAuth2 - SNS 로그인

@EnableWebSecurity
public class SecurityConfig {

    // 비밀번호 암호화 객체 컨테이너에 등록(스프링에게 주입받는 설정)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 시큐리티 설정 (스프링 부트 2.7버전 이전 - 인터페이스를 통해 오버라이딩)
    // 이후엔 이렇게
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors()
                .and()
                .csrf().disable() // 필터 설정 off
                .httpBasic().disable() // 베이직 인증 off
                .formLogin().disable() // 로그인 폼 off
                .authorizeRequests() // 요청 별로 인가 설정
                .antMatchers("/**").permitAll(); // 인가 설정 off

        return http.build();
    }


}
