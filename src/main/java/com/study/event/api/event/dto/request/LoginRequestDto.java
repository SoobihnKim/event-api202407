package com.study.event.api.event.dto.request;

import lombok.*;

@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequestDto {

    // 서버에서도 검증해야함

    private String email;
    private String password;
    // 자동로그인 여부...

}
