package com.aaax.core.utils.jpa;

import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.ResourcesUtil;
import com.aaax.core.utils.ValidationUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.criteria.Expression;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.jpa.domain.Specification;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class JpaUtil {
    private static final String SPLIT_SYMBOL = "-\\s*";
    private static final String SPLIT_SYMBOL_V2 = "::";

    @SneakyThrows
    public static List<JpaSearchFieldMetadata> getJpaSearchFieldMetadata(String filePath, ResourceLoader resourceLoader) {
        InputStream inputStream = ResourcesUtil.readJson(filePath, resourceLoader);
        return JSONUtil.readValue(inputStream, new TypeReference<>() {});
    }


    public static Specification<?> fuzzySearchSpecification(String searchText, List<JpaSearchFieldMetadata> filters) {
        Specification<?> specification = Specification.unrestricted();
        for (JpaSearchFieldMetadata filter : filters) {
            Specification similaritySpecificationString = null;
            switch (filter.getColumn()) {
                case TEXT -> similaritySpecificationString = getSimilaritySpecificationString(searchText, filter.getField(), filter.getRatio());
                case JSONB -> similaritySpecificationString = getSimilaritySpecificationJsonb(searchText, filter.getField(), filter.getJsonPath() , filter.getRatio());
            }
            specification = specification.or(similaritySpecificationString);
        }
        return specification;
    }


    /**
     * default case
     * @param jsonbFields - json.key
     * @param dbColumnField - po.jsonb column
     * @param searchValue - keyword
     *        startIndex = 1; endIndex=2000 as default
     * @return - Specification
     */
    public static Specification<?> hasJsonPropertyLike(
            String jsonbFields, String dbColumnField, String searchValue
    ) {
        Specification<?> specification = Specification.unrestricted();
                // detect its array.
        String[] values = searchValue.split(",");
        for (String value : values) {
            Specification sub_specification;
            String regex = "^[^:]+:\\d+-\\d+$"; // Regular expression to match the format
            boolean isDynamicFormat = ValidationUtil.patternMatches(value, regex);
            if (isDynamicFormat) {
                String _searchValue = value.split(":")[0];
                String[] params = (value.split(":")[1]).split("-");
                String target = _searchValue.replace("*", "%").replaceAll("\\s+", "");
                sub_specification = hasJsonPropertyLike(jsonbFields, dbColumnField, target, Integer.parseInt(params[0]), Integer.parseInt(params[1]));
                log.info("====@@@ query params : [{}]=>[{}]", jsonbFields, target);
            } else {
                String target = value.replace("*", "%").replaceAll("\\s+", "");
                sub_specification = hasJsonPropertyLikeNoIndex(jsonbFields, dbColumnField, target);
                log.info("====@@@ query params : [{}]=>[{}]", jsonbFields, target);
            }
            specification = specification.and(sub_specification);
        }
        return specification;
    }

    public static Specification<?> hasJsonPropertyLikeNoIndex(
            String jsonbFields, String dbColumnField, String searchValue
    ) {
        return (root, query, builder) -> {
            // check whether it is a nested object.
            List<Expression<?>> args = new ArrayList<>();
            args.add(root.get(dbColumnField));
            String[] fields = jsonbFields.split("\\.");
            for (String field : fields) {
                args.add(builder.literal(field).as(String.class));
            }
            Expression<?>[] args_in_where_clause = args.toArray(new Expression[args.size()]);
            Expression<String> jsonb_value = builder.function("jsonb_extract_path_text", String.class, args_in_where_clause);
            log.info("====@@@ hasJsonPropertyLikeNoIndex params : [{}]=>[{}]", jsonbFields, searchValue);
            return builder.like(jsonb_value.as(String.class), searchValue);
        };
    }

    public static Specification<?> hasJsonPropertyLike(
            String jsonbFields, String dbColumnField, String searchValue,
            int startIndex, int endIndex
    ) {
        return (root, query, builder) -> {
            // check whether it is a nested object.
            List<Expression<?>> args = new ArrayList<>();
            args.add(root.get(dbColumnField));
            String[] fields = jsonbFields.split("\\.");
            for (String field : fields) {
                args.add(builder.literal(field).as(String.class));
            }
            Expression<?>[] args_in_where_clause = args.toArray(new Expression[args.size()]);
            Expression<String> jsonb_value = builder.function("jsonb_extract_path_text", String.class, args_in_where_clause);
            log.info("====@@@ hasJsonPropertyLike.searchValue params : [{}]=>[{}]", jsonbFields, searchValue);
            return builder.like(
                    builder.function("substring", String.class,
                            jsonb_value,
                            builder.literal(startIndex), // Starting position
                            builder.literal(endIndex)  // Length
                    ),
                    searchValue // Add wildcard for LIKE
            );
        };
    }

    public static Specification<?> hasJsonPropertyEqual(String jsonbFields, String dbColumnField, String searchValue) {
        return (root, query, builder) -> {
            // check whether it is a nested object.
            List<Expression<?>> args = new ArrayList<>();
            args.add(root.get(dbColumnField));
            String[] fields = jsonbFields.split("\\.");
            for (String field : fields) {
                args.add(builder.literal(field).as(String.class));
            }
            Expression<?>[] args_in_where_clause = args.toArray(new Expression[args.size()]);
            Expression<String> jsonb_value = builder.function("jsonb_extract_path_text", String.class, args_in_where_clause);
            log.info("====@@@ hasJsonPropertyEqual params : [{}]=>[{}]", jsonbFields, searchValue);
            return builder.equal(
                    jsonb_value,
                    searchValue
            );
        };
    }

    public static Specification<?> hasJsonPropertyIn(String jsonbFields, String dbColumnField, List<String> searchValues) {
        return (root, query, builder) -> {
            // check whether it is a nested object.
            List<Expression<?>> args = new ArrayList<>();
            args.add(root.get(dbColumnField));
            String[] fields = jsonbFields.split("\\.");
            for (String field : fields) {
                args.add(builder.literal(field).as(String.class));
            }
            Expression<?>[] args_in_where_clause = args.toArray(new Expression[args.size()]);
            Expression<String> jsonb_value = builder.function("jsonb_extract_path_text", String.class, args_in_where_clause);
            return jsonb_value.in(searchValues);
        };
    }

    public static Specification<?> getSimilaritySpecificationString(String searchQuery, String searchField, double compareRatio){
        return (root, query, builder) -> {

            Expression<String> dbValueInString = builder.lower(root.get(searchField).as(String.class)); //# Important
            Expression<String> lowerSearchQuery = builder.literal(searchQuery.toLowerCase());

            Expression<Double> similarity = builder.function(
                    "similarity",
                    Double.class,
                    dbValueInString,
                    lowerSearchQuery);
            return builder.greaterThanOrEqualTo(similarity, compareRatio);
        };
    }

    public static Specification<?> getSimilaritySpecificationJsonb(String searchQuery, String searchField, String jsonbFields, double compareRatio) {
        return (root, query, builder) -> {
            // check whether it is a nested object.
            List<Expression<?>> args = new ArrayList<>();
            args.add(root.get(searchField));
            String[] fields = jsonbFields.split("\\.");
            for (String field : fields) {
                args.add(builder.literal(field).as(String.class));
            }
            Expression<?>[] args_in_where_clause = args.toArray(new Expression[args.size()]);
            Expression<String> jsonb_value = builder.function("jsonb_extract_path_text", String.class, args_in_where_clause);

            Expression<String> lowerSearchQuery = builder.literal(searchQuery.toLowerCase());

            Expression<Double> similarity = builder.function(
                    "similarity",
                    Double.class,
                    jsonb_value,
                    lowerSearchQuery);
            return builder.greaterThanOrEqualTo(similarity, compareRatio);
        };
    }

    public static @NotNull Map<String, List<String>> processDynamicQueryStringV2(List<String> _query) {
        Map<String, List<String>> queryMap = new HashMap<>();
        // dynamic query filters splitting.
        for (String input : Optional.ofNullable(_query).orElse(List.of())) {
            Map<String, List<String>> bracketQuery = findBracketQueryV2(input);
            log.info("====@@@ DynamicQueryStringV2 : bracketQuery=>[{}]", bracketQuery);
            queryMap.putAll(bracketQuery);
        }
        // dynamic query filters splitting.
        return queryMap;
    }

    public static @NotNull Map<String, List<String>> processDynamicQueryString(List<String> _query) {
        Map<String, List<String>> queryMap = new HashMap<>();
        // dynamic query filters splitting.
        for (String input : Optional.ofNullable(_query).orElse(List.of())) {
            Map<String, List<String>> bracketQuery = findBracketQuery(input);
            log.info("====@@@ DynamicQueryString : bracketQuery=>[{}]", bracketQuery);
            queryMap.putAll(bracketQuery);
        }
        // dynamic query filters splitting.
        return queryMap;
    }

    public static Map<String, List<String>> findBracketQuery(String input) {
        String regex = "\\(([^)]+)\\)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        Map<String, List<String>> resultMap = new HashMap<>();
        if (matcher.find()) {
            String value = matcher.group(1);
            String[] parts = value.split(":");
            String __query__key = parts[0];
            String __query__value = parts[1];

            switch (parts.length) {
                case 2 :
                    String[] values = __query__value.split(SPLIT_SYMBOL);
                    resultMap.put(__query__key, Arrays.asList(values));
                    break;
                case 3 :
                    if (__query__value.startsWith("*") || __query__value.endsWith("*")) { // Wildcard. [Prefix] or [Suffix]
                        // validations with index has been input.
                        resultMap.put(__query__key, List.of(__query__value.concat(":").concat(parts[2])));
                    }
                    break;
            }
        }
        return resultMap;
    }

    public static Map<String, List<String>> findBracketQueryV2(String input) {
        String regex = "\\(([^:]+):(.+)\\)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        Map<String, List<String>> resultMap = new HashMap<>();
        if (matcher.find()) {
            String __query__key = matcher.group(1);  // 'psp'
            String __query__value = matcher.group(2); // 'kvb-1::kvb-2::kvb-3'
            switch (matcher.groupCount()) {
                case 2 :
                    String[] values = __query__value.split(SPLIT_SYMBOL_V2);
                    resultMap.put(__query__key, Arrays.asList(values));
                    break;
                case 3 :
                    if (__query__value.startsWith("*") || __query__value.endsWith("*")) { // Wildcard. [Prefix] or [Suffix]
                        // validations with index has been input.
                        resultMap.put(__query__key, List.of(__query__value.concat(":").concat(matcher.group(3))));
                    }
                    break;
            }
        }
        return resultMap;
    }
}
