package com.article.metaphor_llm_processor.api.filter;

import ai.metaphor.document_api.exception.FilterException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Component
@Slf4j
public class FilterQueryParser {

    private static final String FILTER_SEPARATOR = ";";
    private static final String[] FILTER_OPERATORS = {"==", "!=", "~", "=gt=", "=gte=", "=lt=", "=lte="};
    private static final Collation COLLATION = Collation.of("en").strength(Collation.ComparisonLevel.secondary());

    private static final BiFunction<String, String, Criteria> EQ = (key, value) -> where(key).is(value);
    private static final BiFunction<String, String, Criteria> NEQ = (key, value) -> where(key).ne(value);
    private static final BiFunction<String, String, Criteria> GT = (key, value) -> where(key).gt(value);
    private static final BiFunction<String, String, Criteria> GTE = (key, value) -> where(key).gte(value);
    private static final BiFunction<String, String, Criteria> LT = (key, value) -> where(key).lt(value);
    private static final BiFunction<String, String, Criteria> LTE = (key, value) -> where(key).lte(value);
    private static final BiFunction<String, String, Criteria> ALIKE = (key, value) -> where(key).alike(Example.of(value));

    private static final Map<String, BiFunction<String, String, Criteria>> OPERATOR_MAP = Map.of(
            "==", EQ,
            "!=", NEQ,
            "~", ALIKE,
            "=gt=", GT,
            "=gte=", GTE,
            "=lt=", LT,
            "=lte=", LTE
    );

    public Query parseFilter(String filter, String sortBy, String sortDirection, int page, int limit, String... fields) {
        Query query = new Query();
        query.fields().include(fields);

        if (isNotBlank(filter)) {
            List<Triple> criteriaTriples = extractFilterCriteria(filter);

            for (Triple triple : criteriaTriples) {
                String key = triple.first;
                String value = triple.third;
                BiFunction<String, String, Criteria> biFunction = OPERATOR_MAP.get(triple.second);
                if (biFunction == null) {
                    continue;
                }

                Criteria criteria = biFunction.apply(key, value);
                query.addCriteria(criteria);
            }
        }

        Sort sort = null;
        if (isNotBlank(sortBy) && isNotBlank(sortDirection)) {
            sort = Sort.by(
                    new Sort.Order(getDirection(sortDirection), sortBy)
            );
            query.collation(COLLATION);
        }
        Pageable pageable = sort == null ? PageRequest.of(page, limit) : PageRequest.of(page, limit, sort);
        query.with(pageable);

        return query;
    }

    private static Sort.Direction getDirection(String sortDirection) {
        try {
            return Sort.Direction.valueOf(sortDirection);
        } catch (IllegalArgumentException e) {
            throw new FilterException(String.format("Invalid sort direction: %s", sortDirection));
        }
    }

    // lhs_1<op_1>rhs_1;lhs_2<op_2>rhs_2....
    // -> [lhs_1, op_1, rhs_1], [lhs_2, op_2, rhs_2]
    private List<Triple> extractFilterCriteria(String filter) {
        List<Triple> filterCriteriaTriples = new ArrayList<>();
        String[] filterExprs = filter.split(FILTER_SEPARATOR);
        for (String filterExpr : filterExprs) {
            Triple triple = convertFilterExpressionToTriple(filterExpr).orElseThrow(() -> {
                String errMessage = String.format("Unable to process the following filter expression: %s", filterExpr);
                log.warn(errMessage);
                return new FilterException(errMessage);
            });

            filterCriteriaTriples.add(triple);
        }

        log.info("Filter criteria: {}", filterCriteriaTriples);
        return filterCriteriaTriples;
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    // lhs<op>rhs -> [lhs, op, rhs]
    private Optional<Triple> convertFilterExpressionToTriple(String filterExpression) {
        for (String operator : FILTER_OPERATORS) {
            String[] filterOpParts = filterExpression.split(operator);
            if (filterOpParts.length != 2) {
                continue;
            }

            return Optional.of(new Triple(filterOpParts[0], operator, filterOpParts[1]));
        }

        return Optional.empty();
    }

    // make it generic
    private record Triple(String first,
                          String second,
                          String third) {
    }

}
