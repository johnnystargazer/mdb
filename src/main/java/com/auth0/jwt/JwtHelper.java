package com.auth0.jwt;

import java.lang.reflect.Field;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;

import sg.com.stargazer.res.exception.HttpException;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JwtHelper {
    static Field field;
    static ObjectMapper objectMapper = new ObjectMapper();
    static {
        try {
            field = JWTDecoder.class.getDeclaredField("parts");
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * so we get same map like spring did in dashur
     */
    public static Map getJsonBody(DecodedJWT claim) {
        try {
            String[] parts = (String[]) field.get(claim);
            String payloadJson = StringUtils.newStringUtf8(Base64.decodeBase64(parts[1]));
            return objectMapper.readValue(payloadJson, Map.class);
        } catch (Exception e) {
            throw new HttpException(500, e.getMessage());
        }
    }
}
