package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberSearchCond;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@Repository
@RequiredArgsConstructor
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory; // JPAQueryFactory Bean을 등록하고 생성자 주입 받는 방식

//    public MemberJpaRepository(EntityManager em) {
//        this.em = em;
//        this.queryFactory = new JPAQueryFactory(em);
//    }

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long memberId) {
        return Optional.ofNullable(em.find(Member.class, memberId));
    }

    public List<Member> findAll() {
        return em.createQuery(
                "select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findAll_Querydsl() {
        return queryFactory
                .selectFrom(member)
                .fetch();
    }

    public List<Member> findByUsername(String username) {
        return em.createQuery(
                        "select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList();
    }

    public List<Member> findByUsername_Querydsl(String username) {
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }

    public List<MemberTeamDto> searchByBuilder(MemberSearchCond cond) {
        BooleanBuilder builder = new BooleanBuilder();

        if (hasText(cond.getUsername())) {
            builder.and(member.username.eq(cond.getUsername()));
        }
        if (hasText(cond.getTeamName())) {
            builder.and(team.name.eq(cond.getTeamName()));
        }
        if (cond.getAgeGoe() != null) {
            builder.and(member.age.goe(cond.getAgeGoe()));
        }
        if (cond.getAgeLoe() != null) {
            builder.and(member.age.loe(cond.getAgeLoe()));
        }

        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .fetch();
    }

    public List<MemberTeamDto> searchByWhere(MemberSearchCond cond) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(cond.getUsername()),
                        teamNameEq(cond.getTeamName()),
                        ageGoe(cond.getAgeGoe()),
                        ageLoe(cond.getAgeLoe())
                )
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return hasText(usernameCond) ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression teamNameEq(String teamNameCond) {
        return hasText(teamNameCond) ? team.name.eq(teamNameCond) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoeCond) {
        return ageGoeCond == null ? null : member.age.goe(ageGoeCond);
    }

    private BooleanExpression ageLoe(Integer ageLoeCond) {
        return ageLoeCond == null ? null : member.age.loe(ageLoeCond);
    }

    // MemberTeamDto가 아닌 Member 엔티티 조회 시에도 위에서 작성한 BooleanExpression 사용 가능
    public List<Member> searchMemberByWhere(MemberSearchCond cond) {
        return queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where( // where 방식 동적 쿼리의 장점 -> 재사용성
                        usernameEq(cond.getUsername()),
                        teamNameEq(cond.getTeamName()),
                        ageGoe(cond.getAgeGoe()),
                        ageLoe(cond.getAgeLoe())
                )
                .fetch();
    }

}
