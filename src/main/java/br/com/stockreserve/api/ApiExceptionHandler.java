package br.com.stockreserve.api;

import br.com.stockreserve.service.RegraNegocioException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(RegraNegocioException.class)
    public ProblemDetail negocio(RegraNegocioException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(e.status()), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacao(MethodArgumentNotValidException e) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Verifique os campos enviados.");
        problem.setProperty("erros", e.getBindingResult().getFieldErrors().stream()
                .map(f -> new CampoInvalido(f.getField(), f.getDefaultMessage())).toList());
        return problem;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail formato(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Formato inválido. Confira os tipos, campos e valores enviados.");
    }

    public record CampoInvalido(String campo, String mensagem) {}
}
