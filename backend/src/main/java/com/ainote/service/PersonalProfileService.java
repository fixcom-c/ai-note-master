package com.ainote.service;

import com.ainote.entity.PersonalProfile;
import com.ainote.repository.PersonalProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonalProfileService {

    private final PersonalProfileRepository personalProfileRepository;
    private final UserContextService userContextService;

    public PersonalProfileService(PersonalProfileRepository personalProfileRepository, UserContextService userContextService) {
        this.personalProfileRepository = personalProfileRepository;
        this.userContextService = userContextService;
    }

    public PersonalProfile getProfile() {
        Long userId = userContextService.getCurrentUserId();
        return personalProfileRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyProfile(userId));
    }

    @Transactional
    public PersonalProfile update(PersonalProfile request) {
        PersonalProfile profile = getProfile();
        profile.setDisplayName(normalize(request.getDisplayName()));
        profile.setIdentitySummary(normalize(request.getIdentitySummary()));
        profile.setCurrentFocus(normalize(request.getCurrentFocus()));
        profile.setLongTermGoals(normalize(request.getLongTermGoals()));
        profile.setWorkStyle(normalize(request.getWorkStyle()));
        profile.setLifeAreas(normalize(request.getLifeAreas()));
        profile.setAiPreference(normalize(request.getAiPreference()));
        return personalProfileRepository.save(profile);
    }

    private PersonalProfile createEmptyProfile(Long userId) {
        PersonalProfile profile = new PersonalProfile();
        profile.setUserId(userId);
        profile.setDisplayName("");
        profile.setIdentitySummary("");
        profile.setCurrentFocus("");
        profile.setLongTermGoals("");
        profile.setWorkStyle("");
        profile.setLifeAreas("");
        profile.setAiPreference("");
        return personalProfileRepository.save(profile);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
