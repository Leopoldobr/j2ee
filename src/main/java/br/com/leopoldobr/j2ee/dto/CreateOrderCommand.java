package br.com.leopoldobr.j2ee.dto;

import br.com.leopoldobr.j2ee.domain.InvalidOrderException;
import java.io.Serializable;
import java.util.*;

public record CreateOrderCommand(String customer, List<Line> lines) implements Serializable {
    public CreateOrderCommand {
        if (customer == null || customer.isBlank() || customer.strip().length() > 100) {
            throw new InvalidOrderException("Informe um cliente com até 100 caracteres.");
        }
        if (lines == null || lines.isEmpty() || lines.size() > 20 || lines.stream().anyMatch(Objects::isNull)) {
            throw new InvalidOrderException("Informe entre 1 e 20 produtos.");
        }
        Set<Long> ids = new HashSet<>();
        for (Line line : lines) {
            if (!ids.add(line.productId())) throw new InvalidOrderException("Produto repetido.");
        }
        customer = customer.strip();
        lines = List.copyOf(lines);
    }

    public record Line(long productId, int quantity) implements Serializable {
        public Line {
            if (productId <= 0 || quantity < 1 || quantity > 100) {
                throw new InvalidOrderException("Produto ou quantidade inválidos.");
            }
        }
    }
}
