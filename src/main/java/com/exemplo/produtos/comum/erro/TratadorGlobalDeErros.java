package com.exemplo.produtos.comum.erro;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Converte exceções em respostas no formato "Problem Details" (RFC 9457).
 * Os erros padrão do Spring MVC (JSON inválido, tipo de parâmetro errado, método não suportado etc.)
 * já são tratados pela classe base.
 */
@RestControllerAdvice
public class TratadorGlobalDeErros extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalDeErros.class);

    public record ErroDeCampo(String campo, String mensagem) {
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ProblemDetail recursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage());
    }

    @ExceptionHandler(ConflitoException.class)
    ProblemDetail conflito(ConflitoException ex) {
        return problema(HttpStatus.CONFLICT, "Conflito", ex.getMessage());
    }

    /** Rede de segurança: a constraint do banco barrou algo que escapou das verificações da aplicação. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail violacaoDeIntegridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade de dados: {}", ex.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "Conflito",
                "A operação viola uma restrição de integridade dos dados.");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail edicaoConcorrente(ObjectOptimisticLockingFailureException ex) {
        return problema(HttpStatus.CONFLICT, "Conflito",
                "O registro foi alterado por outra operação. Recarregue os dados e tente novamente.");
    }

    /** Ex.: ?sort=campoQueNaoExiste */
    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail ordenacaoInvalida(PropertyReferenceException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Parâmetro inválido",
                "Não é possível ordenar por '%s'.".formatted(ex.getPropertyName()));
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail erroInesperado(Exception ex) {
        log.error("Erro inesperado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente mais tarde.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        List<ErroDeCampo> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroDeCampo(erro.getField(), erro.getDefaultMessage()))
                .sorted((a, b) -> a.campo().compareTo(b.campo()))
                .toList();

        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Um ou mais campos estão inválidos.");
        problema.setProperty("erros", erros);
        return handleExceptionInternal(ex, problema, headers, status, request);
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        return problema;
    }
}
