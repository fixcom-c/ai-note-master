package com.ainote.controller;

import com.ainote.common.Result;
import com.ainote.entity.PersonalProfile;
import com.ainote.service.PersonalProfileService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class PersonalProfileController {

    private final PersonalProfileService personalProfileService;

    public PersonalProfileController(PersonalProfileService personalProfileService) {
        this.personalProfileService = personalProfileService;
    }

    @GetMapping
    public Result<PersonalProfile> getProfile() {
        return Result.success(personalProfileService.getProfile());
    }

    @PutMapping
    public Result<PersonalProfile> update(@RequestBody PersonalProfile request) {
        return Result.success(personalProfileService.update(request));
    }
}
