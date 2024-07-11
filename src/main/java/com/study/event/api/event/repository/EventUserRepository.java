package com.study.event.api.event.repository;

import com.study.event.api.event.entity.EventUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventUserRepository extends JpaRepository<EventUser, String> {

    // query method로 Jpql 생성
    boolean existsByEmail(String email); // 이름 필드명과 일치해야함(email)

    Optional<EventUser> findByEmail(String email);

}
