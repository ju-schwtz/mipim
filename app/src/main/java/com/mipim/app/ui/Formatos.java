package com.mipim.app.ui;

import com.google.firebase.Timestamp;
import com.mipim.app.model.Volume;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Formatação de valores para exibição (padrão brasileiro) */
final class Formatos {

    private static final Locale BR = Locale.forLanguageTag("pt-BR");

    private Formatos() { }

    /** 24.9 -> "R$ 24,90" */
    static String moeda(double valor) {
        return NumberFormat.getCurrencyInstance(BR).format(valor);
    }

    /** 24.9 -> "24,90" (para preencher o campo de edição) */
    static String valorEditavel(double valor) {
        return String.format(BR, "%.2f", valor);
    }

    static String data(Timestamp ts) {
        return data(ts.toDate().getTime());
    }

    /**
     * O seletor de datas do Material devolve meia-noite em UTC.
     * Formatar em UTC garante que o dia mostrado é o mesmo que o usuário escolheu.
     */
    static String data(long millis) {
        SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy", BR);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date(millis));
    }

    static String rotuloConservacao(String conservacao) {
        if (conservacao == null) return "";
        switch (conservacao) {
            case Volume.CONS_NOVO: return "Novo";
            case Volume.CONS_BOM: return "Bom";
            case Volume.CONS_REGULAR: return "Regular";
            case Volume.CONS_RUIM: return "Ruim";
            default: return conservacao;
        }
    }
}
