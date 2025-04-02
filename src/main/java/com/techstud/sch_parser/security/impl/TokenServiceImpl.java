package com.techstud.sch_parser.security.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
@Slf4j
public class TokenServiceImpl implements com.techstud.sch_parser.security.TokenService {

    @Value("${jwt.secret.key}")
    private String SECRET_KEY;

    @Value("${jwt.issuer}")
    private String issuer;

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
        var jwtCredit = JWT.create()
                .withIssuer(issuer)
                .withClaim("type", "jwt")
                .withIssuedAt(Date.from(Instant.now()))
                .withExpiresAt(Date.from(Instant.now().plus(2, ChronoUnit.MINUTES)))
                .sign(algorithm);
        log.info("Generated JWT token: {}", jwtCredit);
        return jwtCredit;
    }
}
