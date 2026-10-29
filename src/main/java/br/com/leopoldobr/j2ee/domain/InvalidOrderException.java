package br.com.leopoldobr.j2ee.domain;

import jakarta.ejb.ApplicationException;

/** Erro de negócio: o chamador pode corrigir a entrada; a transação é desfeita. */
@ApplicationException(rollback = true)
public class InvalidOrderException extends RuntimeException {
    public InvalidOrderException(String message) { super(message); }
}
