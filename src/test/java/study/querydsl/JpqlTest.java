package study.querydsl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@SpringBootTest
public class JpqlTest {

    @Autowired
    EntityManager em;

    @BeforeEach
    void beforeEach() {
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
    @DisplayName("JPQL을 사용하여 username이 member1인 회원 조회")
    void startJPQL() {
        Member member = em.createQuery(
                        "select m from Member m where m.username = :username",
                        Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(member.getUsername()).isEqualTo("member1");
        assertThat(member.getTeam().getName()).isEqualTo("teamA");
    }

    /**
     * JPQL로 DTO 조회 시
     * new 키워드를 사용하여 dto 패키지 경로를 다 적어줘야 하고
     * 생성자 방식만 지원해서 불편함 -> Querydsl로 편리하게 할 수 있음
     */

    @Test
    @DisplayName("JPQL로 DTO 조회 테스트")
    void findDtoByJPQL() {
        List<MemberDto> result = em.createQuery(
                "select new study.querydsl.dto.MemberDto(m.username, m.age)" +
                        " from Member m",
                MemberDto.class
        ).getResultList();

        System.out.println("result = " + result);
    }

}
