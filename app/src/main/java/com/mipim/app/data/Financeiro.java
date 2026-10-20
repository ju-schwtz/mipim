package com.mipim.app.data;

import com.mipim.app.model.Volume;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

/**
 * Cálculos financeiros sobre os volumes adquiridos (histórias 15 e 16).
 * Classe de Java puro, sem dependência de tela, o que facilita testar.
 */
public final class Financeiro {

    private Financeiro() { }

    /** Gastos de um mês */
    public static class Mes {
        public final int ano;
        public final int mes; // 0 = janeiro ... 11 = dezembro
        public double total;
        public int quantidade;

        Mes(int ano, int mes) {
            this.ano = ano;
            this.mes = mes;
        }
    }

    public static class Resultado {
        /** História 15: soma de todos os valores registrados */
        public double total;
        /** Quantidade de compras que tiveram valor informado */
        public int compras;
        /** História 16: gastos por mês, do mais recente para o mais antigo */
        public final List<Mes> meses = new ArrayList<>();
        /** Compras com valor, mas sem data informada (não entram em nenhum mês) */
        public double semData;
        public int semDataQtd;

        public double gastoDoMes(int ano, int mes) {
            for (Mes m : meses) {
                if (m.ano == ano && m.mes == mes) return m.total;
            }
            return 0;
        }
    }

    public static Resultado calcular(List<Volume> volumes) {
        Resultado r = new Resultado();
        // Chave = ano * 12 + mês; ordem decrescente deixa o mês mais recente primeiro
        Map<Integer, Mes> porMes = new TreeMap<>(Collections.reverseOrder());

        // A data é guardada à meia-noite UTC, então o agrupamento também usa UTC
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        for (Volume v : volumes) {
            if (!v.isAdquirido() || v.getValorPago() == null) continue;

            double valor = v.getValorPago();
            r.total += valor;
            r.compras++;

            if (v.getDataCompra() == null) {
                r.semData += valor;
                r.semDataQtd++;
                continue;
            }

            cal.setTime(v.getDataCompra().toDate());
            int ano = cal.get(Calendar.YEAR);
            int mes = cal.get(Calendar.MONTH);
            int chave = ano * 12 + mes;

            Mes m = porMes.get(chave);
            if (m == null) {
                m = new Mes(ano, mes);
                porMes.put(chave, m);
            }
            m.total += valor;
            m.quantidade++;
        }

        r.total = arredondar(r.total);
        r.semData = arredondar(r.semData);
        for (Mes m : porMes.values()) {
            m.total = arredondar(m.total);
            r.meses.add(m);
        }
        return r;
    }

    /** Evita erros de ponto flutuante (ex.: 0.1 + 0.2) arredondando para centavos */
    private static double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
