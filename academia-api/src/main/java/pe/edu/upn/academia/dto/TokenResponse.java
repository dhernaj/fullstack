package pe.edu.upn.academia.dto;

import java.util.List;

public record TokenResponse(String accessToken, String tokenType, long expiresIn,
                            String username, List<String> roles) {
}
