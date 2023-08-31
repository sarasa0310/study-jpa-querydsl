package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.QTestEntity;
import study.querydsl.entity.TestEntity;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@SpringBootTest
class QuerydslApplicationTests {

	@Autowired
	EntityManager em;

	@Test
	void contextLoads() {
		TestEntity testEntity = new TestEntity();
		em.persist(testEntity);

		JPAQueryFactory query = new JPAQueryFactory(em);
		QTestEntity qTestEntity = QTestEntity.testEntity;

		TestEntity result = query
				.selectFrom(qTestEntity)
				.fetchOne();

		assertThat(result).isEqualTo(testEntity);
		assertThat(result.getId()).isEqualTo(testEntity.getId());
	}

}
