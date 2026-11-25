package dev.rota.bootstrap;

import dev.rota.application.RosterNotFoundException;
import dev.rota.application.UnknownFixtureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps use-case exceptions to the error bodies in rota.yaml. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(UnknownFixtureException.class)
    public ResponseEntity<ErrorResponse> unknownFixture(UnknownFixtureException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ErrorResponse.UNKNOWN_FIXTURE, e.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> missingParameter(MissingServletRequestParameterException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ErrorResponse.MISSING_PARAMETER,
                        "missing query parameter '" + e.getParameterName() + "'"));
    }

    @ExceptionHandler(RosterNotFoundException.class)
    public ResponseEntity<ErrorResponse> rosterNotFound(RosterNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ErrorResponse.ROSTER_NOT_FOUND, e.getMessage()));
    }
}
