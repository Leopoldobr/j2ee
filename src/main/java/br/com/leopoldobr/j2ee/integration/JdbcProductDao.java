package br.com.leopoldobr.j2ee.integration;

import br.com.leopoldobr.j2ee.domain.Product;
import jakarta.annotation.Resource;
import jakarta.ejb.*;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/** DAO JDBC: SQL e gerenciamento de conexões ficam na integração. */
@Stateless
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class JdbcProductDao implements ProductDao {
    @Resource(lookup = "java:/jdbc/PatternsDS") private DataSource dataSource;

    @Override public Optional<Product> find(long id) {
        return Optional.ofNullable(findByIds(Set.of(id)).get(id));
    }

    @Override public List<Product> findAll() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("select id, name, price from products order by id");
             ResultSet rows = statement.executeQuery()) {
            List<Product> products = new ArrayList<>();
            while (rows.next()) products.add(map(rows));
            return List.copyOf(products);
        } catch (SQLException exception) {
            throw new EJBException("Falha ao consultar o catálogo.", exception);
        }
    }

    @Override public Map<Long, Product> findByIds(Set<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        if (ids.size() > 20) throw new IllegalArgumentException("Limite de 20 IDs.");
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = "select id, name, price from products where id in (" + placeholders + ")";
        // Só a quantidade de placeholders compõe o SQL; os valores usam bind parameters.
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameter = 1;
            for (Long id : ids) statement.setLong(parameter++, id);
            try (ResultSet rows = statement.executeQuery()) {
                Map<Long, Product> products = new HashMap<>();
                while (rows.next()) {
                    Product product = map(rows);
                    products.put(product.getId(), product);
                }
                return Map.copyOf(products);
            }
        } catch (SQLException exception) {
            throw new EJBException("Falha ao consultar produtos.", exception);
        }
    }

    private Product map(ResultSet rows) throws SQLException {
        return new Product(rows.getLong("id"), rows.getString("name"), rows.getBigDecimal("price"));
    }
}
