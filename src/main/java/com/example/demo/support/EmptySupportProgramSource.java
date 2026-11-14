package com.example.demo.support;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/** {@code YOUTH_API_KEY}가 없을 때의 사업 목록. 가상 사업을 만들어 내지 않고 빈 목록을 돌려준다. */
@Component
@ConditionalOnExpression("'${youth.api.key:}'.isBlank()")
public class EmptySupportProgramSource implements SupportProgramSource {
    @Override
    public Catalog catalog() {
        return new Catalog(List.of());
    }
}
