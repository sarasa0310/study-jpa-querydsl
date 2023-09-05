package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDto {

    private String username;
    private int age;

    @QueryProjection // querydsl 의존성 추가 단점
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }

}
