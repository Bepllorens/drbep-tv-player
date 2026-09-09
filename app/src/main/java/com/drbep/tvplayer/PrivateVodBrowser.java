package com.drbep.tvplayer;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/** Metadata-only pilot; does not add titles to the device snapshot. */
final class PrivateVodBrowser {
    interface Host {
        void show(String title, String message, List<String> labels, List<Runnable> actions, Runnable back);
        void search(String value, Consumer<String> submit, Runnable back);
        void ui(Runnable action);
    }
    interface Source { JSONObject get(String query) throws Exception; }
    private final Host host;
    private final Source source;
    private final ExecutorService executor;
    private Future<?> pending;
    private int generation;
    private String revision = "";
    private Runnable exit;
    PrivateVodBrowser(Host host, Source source, ExecutorService executor) {
        this.host = host; this.source = source; this.executor = executor;
    }
    void close() { generation++; if (pending != null) pending.cancel(true); pending = null; }
    void open(Runnable back) {
        exit = () -> { close(); if (back != null) back.run(); };
        load("HBO Max", "", descriptor -> {
            revision = descriptor.optString("revision");
            if (!revision.matches("[a-f0-9]{32}") || !descriptor.optBoolean("metadata_preview")) {
                error("Respuesta de catálogo no válida."); return;
            }
            home();
        }, exit);
    }
    private void home() {
        host.show("HBO Max · Catálogo en pruebas", "Solo metadatos. Reproducción aún no habilitada.",
                Arrays.asList("Películas", "Series"), Arrays.asList(
                () -> page("movie", "", "Películas", "", new ArrayList<>(), this::home),
                () -> page("series", "", "Series", "", new ArrayList<>(), this::home)), exit);
    }
    private void load(String title, String query, Consumer<JSONObject> ready, Runnable back) {
        close(); final int request = generation;
        Runnable cancel = () -> { close(); back.run(); };
        host.show(title, "Cargando una página…", Arrays.asList("Cancelar"), Arrays.asList(cancel), cancel);
        pending = executor.submit(() -> {
            try {
                JSONObject result = source.get(query);
                host.ui(() -> { if (request == generation) ready.accept(result); });
            } catch (Exception e) {
                final String message = e.getMessage() == null ? "" : e.getMessage();
                host.ui(() -> {
                    if (request != generation) return;
                    error(message.contains("HTTP 409") ? "El catálogo ha cambiado. Vuelve a abrir HBO Max."
                            : message.contains("HTTP 404") || message.contains("HTTP 403")
                            ? "El piloto no está habilitado para este usuario."
                            : "No se pudo consultar el catálogo. Inténtalo de nuevo más tarde.");
                });
            }
        });
    }
    private void error(String message) {
        host.show("HBO Max", message, Arrays.asList("Volver"), Arrays.asList(exit), exit);
    }
    private static String enc(String value) { return android.net.Uri.encode(value); }
    private void page(String kind, String series, String title, String query, List<String> previous, Runnable parent) {
        String after = previous.isEmpty() ? "" : previous.get(previous.size()-1);
        String params = "revision="+enc(revision)+"&kind="+kind+"&series_id="+enc(series)
                +"&q="+enc(query)+"&after="+enc(after)+"&order="+(kind.equals("episode") ? "episode" : "newest");
        load(title, params, result -> {
            JSONArray items = result.optJSONArray("items");
            if (items == null || items.length() > 40) { error("Página de catálogo no válida."); return; }
            List<String> labels = new ArrayList<>(); List<Runnable> actions = new ArrayList<>();
            Runnable current = () -> page(kind, series, title, query, previous, parent);
            labels.add("Buscar" + (query.isEmpty() ? "" : ": " + query));
            actions.add(() -> host.search(query, text -> page(kind, series, title, text.trim(), new ArrayList<>(), parent), current));
            JSONObject synopses = result.optJSONObject("synopses");
            for (int i=0; i<items.length(); i++) {
                JSONObject row = items.optJSONObject(i);
                if (row == null || row.optString("id").isEmpty()) continue;
                String name = row.optString("title", "Sin título");
                if (kind.equals("episode")) name = "T"+row.optInt("season")+" · E"+row.optInt("episode")+" — "+name;
                labels.add(name);
                if (kind.equals("series")) {
                    actions.add(() -> page("episode", row.optString("id"), row.optString("title", "Serie"), "", new ArrayList<>(), current));
                } else {
                    final String itemTitle = name;
                    JSONObject synopsis = synopses == null ? null : synopses.optJSONObject(row.optString("id"));
                    final String text = (kind.equals("episode") ? title+"\n" : "")
                            + row.optString("air_date") + " · " + row.optLong("duration_seconds")/60 + " min\n\n"
                            + (synopsis == null ? "Sin sinopsis disponible." : synopsis.optString("text", "Sin sinopsis disponible."))
                            + "\n\nSolo consulta. Reproducción aún no habilitada.";
                    actions.add(() -> host.show(itemTitle, text, Arrays.asList("Volver al catálogo"), Arrays.asList(current), current));
                }
            }
            String next = result.optString("next");
            if (!next.isEmpty() && !next.equals(after) && previous.size() < 1000) {
                List<String> forward = new ArrayList<>(previous); forward.add(next);
                labels.add("Página siguiente"); actions.add(() -> page(kind, series, title, query, forward, parent));
            }
            if (!previous.isEmpty()) {
                List<String> prior = new ArrayList<>(previous); prior.remove(prior.size()-1);
                labels.add("Página anterior"); actions.add(() -> page(kind, series, title, query, prior, parent));
            }
            host.show("HBO Max · " + title, "Página " + (previous.size()+1) + " · " + items.length()
                    + " elementos · Catálogo en pruebas", labels, actions, parent);
        }, parent);
    }
}
