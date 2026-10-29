package br.com.leopoldobr.j2ee.domain;

import jakarta.ejb.ApplicationException;

@ApplicationException
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException() { super("Pedido não encontrado."); }
}
