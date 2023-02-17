package engine.controller.response;

import lombok.Getter;

@Getter
public class ResponseError {
    private final boolean result = false;
    private final String error;
    public ResponseError(String error) {
        this.error = error;
    }
}
