package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

@Transactional
@SpringBootTest
@DisplayName("Querydsl 중급 문법 - 수정, 삭제 벌크 연산")
public class BulkTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    void beforeEach() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
//    @Commit
    @DisplayName("수정 벌크 연산") // 주의! 영속성 컨텍스트와 DB 데이터 정합성 문제 발생 가능
    void bulkUpdate() {
        long updateCount = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(30))
                .execute();

        assertThat(updateCount).isEqualTo(2);

        em.flush(); // 영속성 컨텍스트와 DB 동기화
        em.clear(); // 영속성 컨텍스트 초기화

        // em.flush()와 em.clear()를 하지 않으면
        // member1, member2의 username을 "비회원"으로 수정했음에도 여전히 "member1", "member2"로 조회됨
        // why? 벌크 연산은 영속성 컨텍스트를 건너뛰고 바로 DB에 반영
        // 영속성 컨텍스트에는 여전히 member1, 2로 남아있게 되므로, DB에서 조회한 결과와 일치하지 않아서 조회 결과를 반영하지 않음

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member m : result) {
            System.out.println("m.getUsername() = " + m.getUsername());
        }
    }

    @Test
//    @Commit
    @DisplayName("수정 벌크 연산 2")
    void bulkAdd() {
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
//                .set(member.age, member.age.add(-1))
//                .set(member.age, member.age.multiply(2))
                .execute();
    }

    @Test
//    @Commit
    @DisplayName("삭제 벌크 연산")
    void bulkDelete() {
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();

        assertThat(count).isEqualTo(2);
    }

}
