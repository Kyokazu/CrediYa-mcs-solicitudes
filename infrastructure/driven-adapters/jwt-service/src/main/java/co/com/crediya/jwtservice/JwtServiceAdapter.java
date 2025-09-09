package co.com.crediya.jwtservice;

import co.com.crediya.model.loan.UserTokenInfo;
import co.com.crediya.model.loan.gateways.JwtGateway;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtServiceAdapter implements JwtGateway {

    @Value("${jwt.secret}")
    private String secret;

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