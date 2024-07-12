package com.study.event.api.event.service;

import com.study.event.api.event.dto.request.EventUserSaveDto;
import com.study.event.api.event.entity.EmailVerification;
import com.study.event.api.event.entity.EventUser;
import com.study.event.api.event.repository.EmailVerificationRepository;
import com.study.event.api.event.repository.EventUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class EventUserService {

    @Value("${study.mail.host}")
    private String mailHost;

    private final EventUserRepository eventUserRepository;
    private final EmailVerificationRepository emailVerificationRepository;

    // 이메일 전송 객체
    private final JavaMailSender mailSender;

    // 패스워드 암호화 객체
    private final PasswordEncoder encoder;


    // 이메일 중복확인 처리
    public boolean checkEmailDuplicate(String email) {

//        boolean exists = eventUserRepository.existsByEmail(email);
//        log.info("Checking email {} is duplicate : {}", email, exists);

        Optional<EventUser> existingUserOpt = eventUserRepository.findByEmail(email);
        if (existingUserOpt.isPresent()) {
            EventUser existingUser = existingUserOpt.get();
            if(existingUser.getPassword() == null) { // 비밀번호 설정하지 않은 경우
                generateAndSendCode(email, existingUser);
                return false; // 중복으로 처리하지 않음
            } else {
                return true;
            }
        } else {
            processSignUp(email);
            return false;
        }

        // 일련의 후속 처리 (데이터베이스 처리, 이메일 보내는 것...)
//        if (!exists) processSignUp(email);
//
//        return exists;
    }

    public void processSignUp(String email) {

        // 1. 임시 회원가입
        EventUser newEventUser = EventUser
                .builder()
                .email(email)
                .build();

        EventUser savedUser = eventUserRepository.save(newEventUser);

        generateAndSendCode(email, savedUser);

    }

    private void generateAndSendCode(String email, EventUser eventUser) {
        // 2. 이메일 인증 코드 발송
        String code = sendVerificationEmail(email);

        // 3. 인증 코드 정보를 데이터베이스에 저장
        EmailVerification verification = EmailVerification.builder()
                .verificationCode(code) // 인증 코드
                .expiryDate(LocalDateTime.now().plusMinutes(5)) // 만료 시간 (5분 뒤)
                .eventUser(eventUser) // FK
                .build();

        emailVerificationRepository.save(verification);
    }

    // 이메일 인증 코드 보내기
    public String sendVerificationEmail(String email) {

        // 검증 코드 생성하기
        String code = generateVerificationCode();

        // 이메일을 전송할 객체 생성
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            // 누구에게 이메일을 보낼 것인지
            messageHelper.setTo(email);
            // 이메일 제목 설정
            messageHelper.setSubject("[인증메일] 중앙정보스터디 가입 인증 메일입니다.");
            // 이메일 내용 설정
            messageHelper.setText(
                    "인증 코드: <b style=\"font-weight: 700; letter-spacing: 5px; font-size: 30px;\">" + code + "</b>"
                    , true
            );

            // 전송자의 이메일 주소
            messageHelper.setFrom(mailHost);

            // 이메일 보내기
            mailSender.send(mimeMessage);

            log.info("{} 님에게 이메일 전송!", email);

            return code;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 검증 코드 생성 로직 1000~9999 사이의 4자리 숫자
    private String generateVerificationCode() {
        return String.valueOf((int) (Math.random() * 9000 + 1000));
    }

    // 인증코드 체크
    public boolean isMatch(String email, String code) {

        // 이메일을 통해 회원정보를 탐색
        EventUser eventUser = eventUserRepository.findByEmail(email).orElse(null);

        if (eventUser != null) {
            // 인증코드가 있는지 탐색
            EmailVerification ev = emailVerificationRepository.findByEventUser(eventUser).orElse(null);

            // 인증코드가 있고 만료시간이 지나지 않았고 코드 번호가 일치할 경우
            if (ev != null && ev.getExpiryDate().isAfter(LocalDateTime.now())
                    && code.equals(ev.getVerificationCode())) {

                // 이메일 인증여부 true로 수정
                eventUser.setEmailVerified(true);
                eventUserRepository.save(eventUser); // UPDATE

                // 인증코드 데이터베이스에서 삭제
                emailVerificationRepository.delete(ev);
                return true;

            } else { // 인증코드가 틀렸거나 만료된 경우
                // 인증코드 재발송
                // 원래 인증코드 삭제
                emailVerificationRepository.delete(ev);

                // 새 인증코드 발급 이메일 재전송
                // 데이터베이스에 새 인증코드 저장
                generateAndSendCode(email, eventUser);
                return false;
            }
        }
        return false;
    }


    // 회원가입 마무리
    public void confirmSignUp(EventUserSaveDto dto) {

        // 기존 회원정보 조회
        EventUser foundUser = eventUserRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("회원 정보가 존재하지 않습니다."));

        // 인증코드 검증 후 패스워드 등록 안한 경우 인증코드 재발송
        if(foundUser.getPassword() == null ) {
            generateAndSendCode(dto.getEmail(), foundUser);
//            throw new RuntimeException("비밀번호를 설정하지 않은 사용자입니다. 인증 코드를 재발송했습니다.");
        }

        // 데이터 반영 (패스워드, 가입시간)
        String password = dto.getPassword();
        String encodedPassword = encoder.encode(password); // 암호화

        foundUser.confirm(encodedPassword);
        eventUserRepository.save(foundUser);
    }
}
