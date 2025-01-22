package com.techstud.sch_parser.security;

public interface AuthClient {

    void authenticateService();
    void refreshTokens();

}
