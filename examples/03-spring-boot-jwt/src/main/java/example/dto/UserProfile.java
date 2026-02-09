package example.dto;

import java.util.Set;

public record UserProfile(String userId, Set<String> roles) {}
