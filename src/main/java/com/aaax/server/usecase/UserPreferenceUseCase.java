package com.aaax.server.usecase;

import com.aaax.core.entity.dto.aaax.response.GetUserPreferenceResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.utils.IdSplitter;
import com.aaax.core.utils.ResourcesUtil;
import com.aaax.server.entity.dto.request.UpdateUserPreferenceRequestDto;
import com.aaax.server.entity.enu.UserPreferenceType;
import com.aaax.server.entity.po.user_management.UserPreference;
import com.aaax.server.exception.response.UserPreferenceErrorResponse;
import com.aaax.server.repository.UserPreferenceRepository;
import com.aaax.server.service.DtoWrapper;
import com.aaax.server.service.AaaxService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserPreferenceUseCase {

    private final UserPreferenceRepository userPreferenceRepository;
    private final AaaxService aaaxService;
    private final ResourceLoader resourceLoader;

    public GetUserPreferenceResponseDto getUserPreference(String userId, String key) {
        Map config = getConfig();
        if (!config.containsKey(key)) {
            throw new BizException(UserPreferenceErrorResponse.UPN0001, Map.of("availableKeys", config.keySet()));
        }
        aaaxService.getById(IdSplitter.splitToLong(userId));
        UserPreference userPreference = userPreferenceRepository.findByUserIdAndTypeAndKey(
                IdSplitter.splitToLong(userId), UserPreferenceType.DEFAULT.name(), key
        ).orElseGet(() -> this.generateUserPreference(userId, key, (Map<String, Object>) config.getOrDefault(key, new HashMap<>())));
        return DtoWrapper.getGetUserPreferenceResponseDto(userPreference);
    }

    private Map getConfig() {
        return ResourcesUtil.readJson("config/user_preference/user_preference_keys.json", resourceLoader, Map.class);
    }

    public void doCreateDefault(String userId, String key) {
        Map config = getConfig();
        key = Optional.ofNullable(key).orElse("general");
        this.generateUserPreference(userId, key, (Map<String, Object>) config.getOrDefault(key, new HashMap<>()));
    }

    @Transactional
    public UserPreference generateUserPreference(String userId, String key, Map<String, Object> config) {
        UserPreference userPreference = UserPreference.builder()
                .userId(Long.valueOf(IdSplitter.split(userId)))
                .type(UserPreferenceType.DEFAULT.name())
                .key(key)
                .actualPreference(config)
                .build();
        return userPreferenceRepository.save(userPreference);
    }

    @Transactional
    public GetUserPreferenceResponseDto updateUserPreference(String userId, String key, String preference, Map<String, Object> putDto) {
        // == Validations
        Map config = getConfig();
        if (!config.containsKey(key)) {
            throw new BizException(UserPreferenceErrorResponse.UPN0001, Map.of("availableKeys", config.keySet()));
        }
        Map preferences = (Map) config.get(key);
        if (!preferences.containsKey(preference)) {
            throw new BizException(UserPreferenceErrorResponse.UPN0001, Map.of("availablePreferences", preferences.keySet()));
        }
        Map targetPreference = (Map) preferences.get(preference);
        Map allValidations = ResourcesUtil.readJson("config/user_preference/user_preference_validations.json", resourceLoader, Map.class);
        Map _validation = (Map) allValidations.get(preference);
        Map result = doUpdatePreferenceMap(allValidations, putDto, _validation, targetPreference);

        UserPreference userPreference = userPreferenceRepository.findByUserIdAndTypeAndKey(Long.valueOf(IdSplitter.split(userId)), UserPreferenceType.DEFAULT.name(), key).orElseGet(() -> this.generateUserPreference(userId, key, (Map<String, Object>) config.getOrDefault(key, new HashMap<>())));
        userPreference.getActualPreference().put(preference, result);
        userPreference = userPreferenceRepository.save(userPreference);
        return DtoWrapper.getGetUserPreferenceResponseDto(userPreference, preference);
    }

    private Map doUpdatePreferenceMap(Map allValidations, Map<String, Object> putDto, Map _validation, Map targetPreference) {
        boolean isRecursive = (boolean) _validation.get("isRecursive");
        List<String> validValues = (List<String>) _validation.get("values");

        if (putDto.keySet().stream().noneMatch(validValues::contains)) {
            String message = "%s .".formatted(putDto.keySet());
            throw new BizException(UserPreferenceErrorResponse.UPN0001, Map.of("validValues", validValues, "message", message));
        }
        // check over the input is existed options we provided to client
        for (String validValue : validValues) {
            Object userInput = putDto.get(validValue);
            if (userInput == null) { // User no intend to change this value.
                break;
            }

            if (isRecursive) {
                Map<?, ?> map = (Map<?, ?>) allValidations.get(_validation.get("recursiveTarget"));
                doUpdatePreferenceMap(allValidations, (Map) userInput, map, (Map) targetPreference.get(validValue));
            } else {
                validateSelectedAgainstOptions(targetPreference, userInput);
                targetPreference.put(validValue, userInput); // update , replace
            }
        }
        return targetPreference;
    }

    /**
     * {@code options} may be:
     * <ul>
     *   <li>a list of scalars (themes, localizations)</li>
     *   <li>a list of {@code { "value", "label" }} maps</li>
     *   <li>a list of lists of those maps (e.g. timezone grouped in {@code user_preference_keys.json})</li>
     * </ul>
     */
    private void validateSelectedAgainstOptions(Map<?, ?> targetPreference, Object userInput) {
        Object object = targetPreference.get("options");
        if (!(object instanceof List<?> options) || options.isEmpty()) {
            return;
        }
        Object first = options.get(0);
        if (first instanceof Map<?, ?>) {
            List<Object> allowedValues = collectValueFieldsFromLabelValueMaps(options);
            if (!allowedValues.isEmpty() && !allowedValues.contains(userInput)) {
                throwWrongSelected(userInput, allowedValues);
            }
            return;
        }
        if (first instanceof List<?>) {
            List<Object> allowedValues = new ArrayList<>();
            for (Object o : options) {
                if (o instanceof List<?> row) {
                    allowedValues.addAll(collectValueFieldsFromLabelValueMaps(row));
                }
            }
            if (!allowedValues.isEmpty() && !allowedValues.contains(userInput)) {
                throwWrongSelected(userInput, allowedValues);
            }
            return;
        }
        if (!options.contains(userInput)) {
            String message = "Wrong values [%s]".formatted(userInput);
            throw new BizException(UserPreferenceErrorResponse.UPN0001, Map.of("message", message, "availableOptions", options));
        }
    }

    private List<Object> collectValueFieldsFromLabelValueMaps(List<?> mapsOrMixed) {
        List<Object> allowedValues = new ArrayList<>();
        for (Object o : mapsOrMixed) {
            if (o instanceof Map<?, ?> map) {
                Object value = map.get("value");
                if (value != null) {
                    allowedValues.add(value);
                }
            }
        }
        return allowedValues;
    }

    private void throwWrongSelected(Object userInput, List<Object> allowedValues) {
        String message = "Wrong values [%s]".formatted(userInput);
        throw new BizException(UserPreferenceErrorResponse.UPN0001, Map.of("message", message, "availableOptions", allowedValues));
    }

    @Transactional
    public GetUserPreferenceResponseDto updateUserPreference(String userId, String key, UpdateUserPreferenceRequestDto putDto) {
        UserPreference userPreference = userPreferenceRepository.findByUserIdAndTypeAndKey(
                Long.valueOf(IdSplitter.split(userId)), UserPreferenceType.DEFAULT.name(), key
        ).orElseThrow(() -> new BizException(UserPreferenceErrorResponse.UPN0001));
        return DtoWrapper.getGetUserPreferenceResponseDto(userPreference);
    }

    public List<GetUserPreferenceResponseDto> queryUserPreference(String userId) {
        aaaxService.getById(IdSplitter.split(userId));
        List<UserPreference> userPreferences = userPreferenceRepository.findAllByUserIdAndType(Long.valueOf(IdSplitter.split(userId)), UserPreferenceType.DEFAULT.name());
        return userPreferences.stream().map(DtoWrapper::getGetUserPreferenceResponseDto).toList();
    }
}
