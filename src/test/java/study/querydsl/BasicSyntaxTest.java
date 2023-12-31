package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.select;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

/**
 * 1. 기본 Q-Type 활용
 * 2. 검색 조건 쿼리
 * 3. 다양한 결과 조회 방법 - fetchXX
 * 4. 정렬 & 페이징 & 집합
 * 5. 조인 - 기본 조인, on절, 패치 조인
 * 6. 서브 쿼리
 * 7. Case 문
 * 8. 상수, 문자 더하기
 */

@Transactional
@SpringBootTest
@DisplayName("Querydsl 기본 문법")
public class BasicSyntaxTest {

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
    @DisplayName("Querydsl을 사용하여 username이 member1인 회원 조회")
    void startQuerydsl() {
        // Q클래스 인스턴스 사용 방법
//        QMember m = new QMember("m");
//        QMember m = QMember.member;

        Member foundMember = queryFactory
                .select(member) // 다음과 같이 static import해서 사용하는것을 추천
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(foundMember.getUsername()).isEqualTo("member1");
        assertThat(foundMember.getTeam().getName()).isEqualTo("teamA");
    }

    @Test
    @DisplayName("username이 member1이면서, age가 10살인 회원 조회")
    void findByUsernameAndAgeTest() {
//        Member foundMember = queryFactory
//                .selectFrom(member)
//                .where(member.username.eq("member1")
//                        .and(member.age.eq(10)))
//                .fetchOne();

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
    @DisplayName("다양한 리턴 타입을 갖는 fetch 메서드 테스트")
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

        // 주의! fetchResults() & fetchCount()는 Deprecated 되었다.

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
    * 단, 2에서 회원 이름이 없으면(null이면) 마지막에 출력(nulls last)
     */

    @Test
    @DisplayName("정렬 테스트")
    void sort() {
        // beforeEach() 이후 초기 회원 데이터 추가
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(
                        member.age.desc(), // 먼저, 회원 나이로 내림차순 정렬
                        member.username.asc().nullsLast()) // 나이가 같다면 이름 오름차순 정렬
                .fetch();

        assertThat(result.get(0).getUsername()).isEqualTo("member5");
        assertThat(result.get(1).getUsername()).isEqualTo("member6");
        assertThat(result.get(result.size() - 1).getUsername()).isNull();
    }

    @Test
    @DisplayName("페이징 테스트 1")
    void paging() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(0)
                .limit(2)
                .fetch();

        assertThat(result).hasSize(2);
    }

    @Test // fetchResults()는 deprecated 되었다. 복잡한 쿼리에서는 성능 문제 발생 가능 -> count 쿼리 별도로 작성
    @DisplayName("페이징 테스트 2 - count 쿼리 포함")
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
    @DisplayName("집합 테스트")
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

    @Test
    @DisplayName("팀의 이름과 회원의 평균 나이를 구하고, 팀의 이름으로 그룹화하는 테스트")
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

    @Test
    @DisplayName("회원과 팀을 조인하고, 회원이 속한 팀의 이름이 teamA인 모든 회원 조회")
    void joinMemberAndTeam() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(member.team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    @Test
    @DisplayName("회원의 이름이 팀의 이름과 같은 모든 회원 조회 - 연관 관계가 없는 필드 조회")
    void thetaJoin() {
        // 회원 이름이 팀의 이름과 같은 더미 회원 데이터 추가
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * JPQL -> select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    @DisplayName("회원과 팀을 left outer join하면서, 팀 이름이 teamA인 회원 조회")
    void joinOnFiltering() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA")) // left join일 경우 on 사용해야 함
//                .where(team.name.eq("teamA")) // inner join일 경우 where 사용하면 결과 same
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple); // left outer join이므로 teamA가 아닌 회원의 팀 null 처리
        }
    }

    /**
     * 연관관계 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    void joinOnNoRelation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .join(team).on(member.username.eq(team.name))
                .fetch();

        System.out.println(result);
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    void noFetchJoin() {
        em.flush();
        em.clear();

        Member result = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(result.getTeam());
        assertThat(loaded).isFalse();
    }

    @Test
    void fetchJoin() {
        em.flush();
        em.clear();

        Member result = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(result.getTeam());
        assertThat(loaded).isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    void subQuery() {
        QMember mSub = new QMember("mSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(mSub.age.max())
                                .from(mSub)
                )).fetch();

        assertThat(result)
                .extracting("age")
                .containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원 조회
     */
    @Test
    void subQueryGoe() {
        QMember mSub = new QMember("mSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(mSub.age.avg())
                                .from(mSub)
                )).fetch();

        assertThat(result)
                .extracting("age")
                .containsExactly(30, 40);
    }

    /**
     * 나이가 평균 이상인 회원 조회
     */
    @Test
    void subQueryIn() {
        QMember mSub = new QMember("mSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(mSub.age)
                                .from(mSub)
                                .where(mSub.age.gt(10))
                )).fetch();

        assertThat(result)
                .extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    void selectSubQuery() {
        QMember mSub = new QMember("mSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        select(mSub.age.avg())
                                .from(mSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("10살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void complexCase() { // 실무에서는 애플리케이션에서 복잡한 비즈니스 로직을 처리하고, DB에서는 데이터를 최소화해서 가져오는데만 집중하자
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0 ~ 20살")
                        .when(member.age.between(21, 30)).then("21 ~ 30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void concat() {
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("username_age = " + s);
        }
    }

}
