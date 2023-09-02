package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@Transactional
@SpringBootTest
public class QuerydslBasicTest {

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
    void startJPQL() {
        // member1 찾기
        Member member = em.createQuery(
                        "select m from Member m where m.username = :username",
                        Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(member.getUsername()).isEqualTo("member1");
        assertThat(member.getTeam().getName()).isEqualTo("teamA");
    }

    @Test
    void startQuerydsl() {
        // member1 찾기
//        QMember m = new QMember("m");
//        QMember m = QMember.member;

        Member foundMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(foundMember.getUsername()).isEqualTo("member1");
        assertThat(foundMember.getTeam().getName()).isEqualTo("teamA");
    }

    @Test
    void search() {
        // username이 member1 이면서, age가 10살인 회원 찾기
        Member foundMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(foundMember.getUsername()).isEqualTo("member1");
        assertThat(foundMember.getAge()).isEqualTo(10);
    }

    @Test
    void searchAndParam() {
        // username이 member1 이면서, age가 10살인 회원 찾기
        Member foundMember = queryFactory
                .selectFrom(member)
                .where( // 여러 개의 인자가 있으면 AND
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(foundMember.getUsername()).isEqualTo("member1");
        assertThat(foundMember.getAge()).isEqualTo(10);
    }

    @Test
    void resultPatch() {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

//        Member fetchOne = queryFactory
//                .selectFrom(member)
//                .fetchOne();

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();

        // fetchResults() & fetchCount()는 Deprecated 되었다.
//        QueryResults<Member> queryResults = queryFactory
//                .selectFrom(member)
//                .fetchResults();
//
//        long total = queryResults.getTotal();
//        System.out.println(total);
//
//        List<Member> results = queryResults.getResults();
//        System.out.println(results);

//        long count = queryFactory
//                .selectFrom(member)
//                .fetchCount();
//
//        System.out.println(count);
    }

    /*
    * 회원 정렬 순서
    * 1. 회원 나이 내림차순(desc)
    * 2. 회원 이름 오름차순(asc)
    * 단, 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */

    @Test
    void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(
                        member.age.desc(),
                        member.username.asc().nullsLast())
                .fetch();

//        System.out.println(result);

        assertThat(result.get(0).getUsername()).isEqualTo("member5");
        assertThat(result.get(1).getUsername()).isEqualTo("member6");
        assertThat(result.get(result.size() - 1).getUsername()).isNull();
    }

    @Test
    void paging() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(0)
                .limit(2)
                .fetch();

        System.out.println(result);

        assertThat(result).hasSize(2);
    }

    @Test // fetchResults()는 deprecated 되었다. 복잡한 쿼리에서는 성능 문제 발생 가능 -> count 쿼리 별도로 작성
    void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(0)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getOffset()).isEqualTo(0);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    @Test
    void aggregation() {
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();

        Tuple tuple = result.get(0); // 실무에서는 Tuple을 많이 쓰지 않고, dto로 많이 조회

        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /*
    * 팀의 이름과 각 팀의 평균 연령 구하기
    * 팀의 이름으로 그룹핑
     */
    @Test
    void groupBy() {
        // given & when
        List<Tuple> result = queryFactory
                .select(
                        team.name,
                        member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        // then
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

}
