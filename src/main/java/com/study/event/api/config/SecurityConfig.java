package com.study.event.api.config;

import com.study.event.api.auth.filter.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.CorsFilter;

// 스프링 시큐리티 설정 파일
// 인터셉터, 필터 처리 가능
// 세션 인증, 토큰 인증
// 권한 처리
// OAuth2 - SNS 로그인

@EnableWebSecurity
// 컨트롤러에서 사전, 사후에 권한 정보를 캐치해서 막을건지
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

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

                // 세션 인증은 더이상 사용하지 않음
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()

                .authorizeRequests() // 요청 별로 인가 설정

                // /events/* -> 뒤에 딱 하나만
                // /events/** -> 뒤에 여러개
                .antMatchers(HttpMethod.DELETE, "/events/*").hasAnyAuthority("ADMIN")

                .antMatchers(HttpMethod.PUT, "/auth/promote").hasAuthority("COMMON")
                // 아래의 URL 요청은 로그인 없이 모두 허용
                .antMatchers("/", "/auth/**", "/file/**").permitAll() // 로그인이나 회원가입은 인증안받아도 들어올 수 있게
//                .antMatchers(HttpMethod.POST,"/events/**").permitAll()
//                .antMatchers(HttpMethod.POST,"/events/**").hasAnyRole("VIP", "ADMIN")

                // 나머지 요청은 전부 인증(로그인) 후 진행 가능
                .anyRequest().authenticated(); // 인가 설정 on

        // 토큰 위조 검사 커스텀 필터 필터체인에 연결
        // CorsFilter(스프링의 필터) 뒤에 커스텀 필터를 연결
        http.addFilterAfter(jwtAuthFilter, CorsFilter.class);

        return http.build();
    }


}
