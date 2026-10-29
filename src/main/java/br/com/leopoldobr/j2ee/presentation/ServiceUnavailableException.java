package br.com.leopoldobr.j2ee.presentation;

public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(Throwable cause) { super("Serviço temporariamente indisponível.", cause); }
}
