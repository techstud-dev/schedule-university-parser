package com.techstud.sch_parser.security.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class TokenServiceImpl implements com.techstud.sch_parser.security.TokenService {

    @Value("${jwt.secret.key}")
    private String SECRET_KEY;

    private Algorithm algorithm;

    @PostConstruct
    public void init() {
        if (SECRET_KEY == null || SECRET_KEY.isEmpty()) {
            throw new IllegalArgumentException("The Secret cannot be null or empty");
        }
        this.algorithm = Algorithm.HMAC256(SECRET_KEY);
    }

    @Override
    public String generateServiceToken() {
        return JWT.create()
                .withIssuer("sch-parser")
                .withClaim("role", "SERVICE")
                .withIssuedAt(Date.from(Instant.now()))
                .withExpiresAt(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .sign(algorithm);
    }
}
