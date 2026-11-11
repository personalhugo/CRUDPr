package com.exemplo.produtos.comum.erro;

/** A operação conflita com o estado atual dos dados (ex.: valor único já em uso). Vira HTTP 409. */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
