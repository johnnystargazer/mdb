package sg.com.stargazer.res.security;

import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import sg.com.stargazer.res.exception.HttpException;
import spark.Filter;
import spark.Request;
import spark.Response;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.JwtHelper;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

@Slf4j
public class SecurityFilter implements Filter {
    public static String BEARER_TYPE = "Bearer";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    JWTVerifier check = JWT.require(Algorithm.HMAC256("dashur")).build();
    public static final String TOKEN_USER = "token_user";

    public SecurityFilter() {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    }

    protected String extractHeaderToken(HttpServletRequest request) {
        Enumeration<String> headers = request.getHeaders("Authorization");
        while (headers.hasMoreElements()) { // typically there is only one (most
            String value = headers.nextElement();
            if ((value.toLowerCase().startsWith(BEARER_TYPE.toLowerCase()))) {
                String authHeaderValue = value.substring(BEARER_TYPE.length()).trim();
                int commaIndex = authHeaderValue.indexOf(',');
                if (commaIndex > 0) {
                    authHeaderValue = authHeaderValue.substring(0, commaIndex);
                }
                return authHeaderValue;
            }
        }
        return null;
    }

    @Override
    public void handle(Request request, Response response) throws Exception {
        String token = extractHeaderToken(request.raw());
        if (token == null) {
            log.warn("no token found ");
            throw new HttpException(401, "Not token");
        } else {
            DecodedJWT jwt = JWT.decode(token);
            String cilentId = jwt.getClaim("client_id").asString();
            // TODO read key setting by client id
            try {
                DecodedJWT decoded = check.verify(token);
                Map map = JwtHelper.getJsonBody(decoded);
                TokenUser tokenUser = TokenUser.of(map);
                request.attribute(TOKEN_USER, tokenUser);
            } catch (Exception e) {
                log.error("token expired or not valied ", e);
                throw new HttpException(401, "token expired or not valied");
            }
        }
    }
}
