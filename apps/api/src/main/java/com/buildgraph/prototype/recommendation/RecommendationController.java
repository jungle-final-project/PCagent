package com.buildgraph.prototype.recommendation;

import com.buildgraph.prototype.user.CurrentUserService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RecommendationController {
    private final RecommendationLearningService recommendationLearningService;
    private final HomePartRecommendationService homePartRecommendationService;
    private final CurrentUserService currentUserService;

    public RecommendationController(
            RecommendationLearningService recommendationLearningService,
            HomePartRecommendationService homePartRecommendationService,
            CurrentUserService currentUserService
    ) {
        this.recommendationLearningService = recommendationLearningService;
        this.homePartRecommendationService = homePartRecommendationService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/recommendation-events")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> recordRecommendationEvent(
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        CurrentUserService.CurrentUser user = currentUserService.requireUser(authorization);
        return recommendationLearningService.recordEvent(request == null ? Map.of() : request, user);
    }

    @GetMapping("/recommendations/home-parts")
    Map<String, Object> homeRecommendedParts(
            @org.springframework.web.bind.annotation.RequestParam(value = "limit", required = false) Integer limit,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        CurrentUserService.CurrentUser user = currentUserService.requireUser(authorization);
        return homePartRecommendationService.homeParts(user, limit);
    }

    @PostMapping("/admin/recommendation-feedback/as-tickets/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> confirmAsRecommendationFeedback(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        CurrentUserService.CurrentUser admin = currentUserService.requireAdmin(authorization);
        return recommendationLearningService.confirmAsNegativeFeedback(id, request == null ? Map.of() : request, admin);
    }

    @GetMapping("/admin/recommendation-models")
    Map<String, Object> recommendationModels(@RequestHeader(value = "Authorization", required = false) String authorization) {
        currentUserService.requireAdmin(authorization);
        return recommendationLearningService.modelVersions();
    }
}
