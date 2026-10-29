package br.com.leopoldobr.j2ee.integration;

import br.com.leopoldobr.j2ee.domain.Order;
import br.com.leopoldobr.j2ee.dto.OrderSummaryDto;
import jakarta.ejb.*;
import jakarta.persistence.*;
import java.util.*;

@Stateless
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class JpaOrderDao implements OrderDao {
    @PersistenceContext private EntityManager em;

    @Override public void save(Order order) {
        order.validateForSubmission();
        em.persist(order); // cascade persiste os itens; a fachada controla a transação.
    }
    @Override public Optional<Order> find(String id) {
        return Optional.ofNullable(em.find(Order.class, id));
    }
    @Override public Optional<Order> findForUpdate(String id) {
        return Optional.ofNullable(em.find(Order.class, id, LockModeType.PESSIMISTIC_WRITE));
    }
    @Override public List<OrderSummaryDto> findRecent(int limit) {
        if (limit < 1 || limit > 501) throw new IllegalArgumentException("Limite entre 1 e 501.");
        // Projeção: não materializa entidades nem faz fetch join paginado de uma coleção.
        return em.createQuery("""
                select new br.com.leopoldobr.j2ee.dto.OrderSummaryDto(
                    o.id, o.customer, o.status, sum(l.unitPrice * l.quantity), o.createdAt)
                from PurchaseOrder o join o.lines l
                group by o.id, o.customer, o.status, o.createdAt
                order by o.createdAt desc, o.id desc
                """, OrderSummaryDto.class).setMaxResults(limit).getResultList();
    }
}
