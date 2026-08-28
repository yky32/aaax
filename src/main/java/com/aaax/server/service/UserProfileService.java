package com.aaax.server.service;

import com.aaax.core.utils.IdSplitter;
import com.aaax.core.utils.jpa.JpaSearchFieldMetadata;
import com.aaax.core.utils.jpa.JpaUtil;
import com.aaax.server.entity.po.user_management.UserProfile;
import com.aaax.server.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final ResourceLoader resourceLoader;
    private final UserProfileRepository userProfileRepository;

    public List<UserProfile> search(String searchText) {
        if (!StringUtils.isEmpty(searchText)) {
            Specification<UserProfile> specification = Specification.unrestricted();
            List<JpaSearchFieldMetadata> filters = JpaUtil.getJpaSearchFieldMetadata("jpa_specification/get_user_profile_specification.json", resourceLoader);
            log.info("-- List<JpaSearchFieldMetadata> for [Specification<UserProfile>] => {}", filters);
            specification = specification.and((Specification<UserProfile>) JpaUtil.fuzzySearchSpecification(searchText, filters));
            return userProfileRepository.findAll(specification);
        }
        return List.of();
    }

    public List<UserProfile> searchNames(String searchText) {
        if (!StringUtils.isEmpty(searchText)) {
            Specification<UserProfile> specification = Specification.unrestricted();
            List<JpaSearchFieldMetadata> filters = JpaUtil.getJpaSearchFieldMetadata("jpa_specification/get_user_profile_names_specification.json", resourceLoader);
            log.info("-- List<JpaSearchFieldMetadata> for [Specification<UserProfile>] Names=> {}", filters);
            specification = specification.and((Specification<UserProfile>) JpaUtil.fuzzySearchSpecification(searchText, filters));
            return userProfileRepository.findAll(specification);
        }
        return List.of();
    }

    public List<UserProfile> searchEmail(String searchText) {
        if (!StringUtils.isEmpty(searchText)) {
            Specification<UserProfile> specification = Specification.unrestricted();
            List<JpaSearchFieldMetadata> filters = JpaUtil.getJpaSearchFieldMetadata("jpa_specification/get_user_profile_email_specification.json", resourceLoader);
            log.info("-- List<JpaSearchFieldMetadata> for [Specification<UserProfile>] Email=> {}", filters);
            specification = specification.and((Specification<UserProfile>) JpaUtil.fuzzySearchSpecification(searchText, filters));
            return userProfileRepository.findAll(specification);
        }
        return List.of();
    }

    public List<UserProfile> getByUserIds(List<Long> userId) {
        return userProfileRepository.findByUserIdIn(userId.stream().map(u -> Long.valueOf(IdSplitter.split(u))).toList());
    }
}
