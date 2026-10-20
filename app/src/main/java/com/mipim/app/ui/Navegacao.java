package com.mipim.app.ui;

import android.app.Activity;
import android.content.Intent;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mipim.app.R;

/**
 * Barra de navegação inferior compartilhada pelas telas principais
 * (Início, Biblioteca, Faltantes, Desejos e Perfil).
 *
 * O Dashboard é a base da pilha: as outras abas ficam sobre ele e, ao trocar de aba,
 * a anterior é fechada. Assim, o botão "voltar" sempre leva ao Dashboard.
 */
final class Navegacao {

    private Navegacao() { }

    /** Marca a aba da tela atual e ativa a troca de aba */
    static void configurar(Activity activity, BottomNavigationView nav, int itemAtual) {
        // A seleção vem antes do listener, para não disparar uma navegação
        nav.setSelectedItemId(itemAtual);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == itemAtual) return true;
            irPara(activity, destino(id));
            return false; // a tela atual será fechada; não mantém a seleção nova
        });
    }

    private static Class<? extends Activity> destino(int itemId) {
        if (itemId == R.id.nav_inicio) return DashboardActivity.class;
        if (itemId == R.id.nav_biblioteca) return MainActivity.class;
        if (itemId == R.id.nav_faltantes) return FaltantesActivity.class;
        if (itemId == R.id.nav_desejos) return InteresseActivity.class;
        return PerfilActivity.class;
    }

    private static void irPara(Activity atual, Class<? extends Activity> destino) {
        Intent intent = new Intent(atual, destino);
        if (destino == DashboardActivity.class) {
            // Volta ao Dashboard que já está na pilha, em vez de abrir outro
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        atual.startActivity(intent);

        // O Dashboard fica na pilha; as demais abas são substituídas
        if (!(atual instanceof DashboardActivity)) {
            atual.finish();
        }
        atual.overridePendingTransition(0, 0);
    }
}
