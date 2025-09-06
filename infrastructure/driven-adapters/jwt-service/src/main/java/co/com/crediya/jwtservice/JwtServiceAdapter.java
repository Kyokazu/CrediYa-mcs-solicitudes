package co.com.crediya.jwtservice;

import co.com.crediya.model.user.User;
import co.com.crediya.model.user.UserTokenInfo;
import co.com.crediya.model.user.gateways.JwtGateway;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtServiceAdapter implements JwtGateway {

    private final String secret = "dGhpcy1pcy1hLXRlc3Qtc2VjcmV0LWZvci1kZXZlbG9wbWVudC1vbmx5LWRvLW5vdC11c2UtaW4tcHJvZHVjdGlvbi1lbnZpcm9ubWVudA==";
    private final long expirationMs = 3600_000;

    @Override
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRoleId())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS256, secret.getBytes())
                .compact();
    }

    @Override
    public Mono<UserTokenInfo> validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secret.getBytes())
                    .parseClaimsJws(token)
                    .getBody();

            return Mono.just(UserTokenInfo.builder()
                    .token(token)
                    .email(claims.get("email", String.class))
                    .role(claims.get("role", String.class))
                    .build());
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Invalid or expired token"));
        }
    }
}