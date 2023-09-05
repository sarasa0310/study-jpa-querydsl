package study.querydsl;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import java.util.List;

import static study.querydsl.entity.QMember.member;

@Transactional
@SpringBootTest
@DisplayName("Querydsl 중급 문법 - Projection")
public class ProjectionTest {

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
    @DisplayName("단순한 프로젝션")
    void simpleProjection() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        System.out.println("result = " + result);
    }

    /**
     * Tuple은 querydsl에 종속적이므로
     * Repository 계층 내부에서만 사용하고
     * 외부로 내보낼 때는 DTO로 변환해서 반환하는 것을 추천
     */

    @Test
    @DisplayName("여러 필드를 조회할 때 사용하는 Tuple")
    void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("username = " + tuple.get(member.username));
            System.out.println("age = " + tuple.get(member.age));
        }
    }

    /**
     * DTO 조회
     * 1. 프로퍼티 접근 방식(Setter 방식)
     * 2. Field 접근 방식
     * 3. 생성자 방식
     */

    @Test
    @DisplayName("Setter 방식 DTO 조회")
    void findDtoBySetter() {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    @DisplayName("Field 접근 방식 DTO 조회")
    void findDtoByField() {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    @DisplayName("생성자 방식 DTO 조회")
    void findDtoByConstructor() {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    @DisplayName("필드명이 다른 DTO 조회")
    void findUserDto() {
//        List<UserDto> result = queryFactory
//                .select(Projections.fields(UserDto.class,
//                        member.username.as("name"),
//                        member.age))
//                .from(member)
//                .fetch();

        QMember mSub = new QMember("mSub");

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"), // Member.username -> UserDto.name 매핑
                        ExpressionUtils.as( // 서브 쿼리는 ExpressionUtils.as로 감싸줘야 함
                                JPAExpressions
                                        .select(mSub.age.max())
                                        .from(mSub),
                                "age"
                        )))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    @DisplayName("생성자 방식으로 필드명이 다른 DTO 조회") // as로 매핑해줄 필요 X
    void findUserDtoByConstructor() {
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    @DisplayName("DTO를 Q클래스로 생성하여 안전하게 DTO 조회")
    void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age)) // 컴파일 오류 체크 가능
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

}
