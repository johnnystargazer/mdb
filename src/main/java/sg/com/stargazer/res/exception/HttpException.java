package sg.com.stargazer.res.exception;

import lombok.Value;

@Value
public class HttpException extends RuntimeException {
    private int code;
    private String body;
}
