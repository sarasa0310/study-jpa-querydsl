package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import study.querydsl.entity.Member;

import java.util.List;

public interface MemberRepository extends
        JpaRepository<Member, Long>,
        MemberRepositoryCustom,
        QuerydslPredicateExecutor<Member> { // 실제 실무 적용 한계 O, Service나 Controller 코드가 Querydsl 의존 + 제한된 기능

    List<Member> findByUsername(String username);

}
