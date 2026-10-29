package br.com.leopoldobr.j2ee.presentation;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Named("format") @RequestScoped
public class FormatHelper {
    public String money(BigDecimal value) { return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(value); }
    public String date(Instant value) {
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Sao_Paulo")).format(value);
    }
}
