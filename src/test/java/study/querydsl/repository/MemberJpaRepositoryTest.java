package study.querydsl.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCond;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@SpringBootTest
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository repository;

    @Test
    @DisplayName("기본적인 동작 확인 테스트")
    void basicTest() {
        Member member = new Member("member1", 10);
        repository.save(member);

        Member findMember = repository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

//        List<Member> members = repository.findAll();
        List<Member> members = repository.findAll_Querydsl();
        assertThat(members).containsExactly(member);

//        List<Member> byUsername = repository.findByUsername(member.getUsername());
        List<Member> byUsername = repository.findByUsername_Querydsl(member.getUsername());
        assertThat(byUsername).containsExactly(member);
    }

    @Test
    @DisplayName("BooleanBuilder 방식 동적 쿼리 테스트")
    void searchByBuilder() {
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

        MemberSearchCond condition = new MemberSearchCond();
        condition.setUsername("member1");
        condition.setTeamName("teamA");
        condition.setAgeGoe(10);
        condition.setAgeLoe(20);

//        List<MemberTeamDto> result = repository.searchByBuilder(condition);
        List<MemberTeamDto> result = repository.searchByWhere(condition);
        System.out.println("result = " + result);

        assertThat(result).extracting("username")
                .containsExactly("member1");
    }

}