package capstone.support.userprofile;

import lombok.Builder;
import lombok.Value;
import java.util.Map;

@Value
@Builder
public class UserProfile {
    String userId;
    String childAge;
    String parentingStyle;
    String parentingGoal;
    String childTraits;
    String preferredTone;
    String language;
    String healthIssues; // CSV의 allergies_or_health_issues

    public Map<String, Object> toPythonMap() {
        return Map.of(
                "user_id", userId,
                "child_age", childAge,
                "parenting_style", parentingStyle,
                "parenting_goal", parentingGoal,
                "child_traits", childTraits,
                "preferred_tone", preferredTone,
                "language", language,
                "health_issues", healthIssues
        );
    }
}
