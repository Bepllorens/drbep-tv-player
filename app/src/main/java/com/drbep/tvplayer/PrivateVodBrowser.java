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
        void play(String id, String title, String kind, String posterQuery);
        void ui(Runnable action);
        default void dismiss() {}
        default void cards(String title, String message, List<Card> cards, List<String> labels, List<Runnable> actions, Runnable back) {
            show(title, message, labels, actions, back);
        }
    }
    static final class Card {
        final String title, synopsis, posterQuery;
        final Runnable action;
        Card(String title, String synopsis, String posterQuery, Runnable action) {
            this.title = title; this.synopsis = synopsis; this.posterQuery = posterQuery; this.action = action;
        }
    }
    interface Source { JSONObject get(String query) throws Exception; }
    private final Host host;
    private final Source source;
    private final ExecutorService executor;
    private Future<?> pending;
    private int generation;
    private String revision = "";
    private boolean playbackEnabled;
    private Runnable exit;
    PrivateVodBrowser(Host host, Source source, ExecutorService executor) {
        this.host = host; this.source = source; this.executor = executor;
    }
    void close() { generation++; if (pending != null) pending.cancel(true); pending = null; host.dismiss(); }
    void open(Runnable back) {
        exit = () -> { close(); if (back != null) back.run(); };
        load("HBO Max", "", descriptor -> {
            revision = descriptor.optString("revision");
            playbackEnabled = descriptor.optBoolean("playback_enabled");
            if (!revision.matches("[a-f0-9]{32}") || !descriptor.optBoolean("metadata_preview")) {
                error("Respuesta de catálogo no válida."); return;
            }
            home();
        }, exit);
    }
    private void home() {
        host.show("HBO Max · Catálogo en pruebas", playbackEnabled ? "Catálogo en pruebas. Selecciona un título para reproducirlo." : "Solo metadatos. Reproducción aún no habilitada.",
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
        page(kind, series, title, query, previous, parent, 0);
    }
    static String seasonLabel(int season) {
        return season > 0 ? "Temporada " + season : "Todas las temporadas";
    }
    private void page(String kind, String series, String title, String query, List<String> previous, Runnable parent, int season) {
        String after = previous.isEmpty() ? "" : previous.get(previous.size()-1);
        String params = "revision="+enc(revision)+"&kind="+kind+"&series_id="+enc(series)
                +"&q="+enc(query)+"&after="+enc(after)+"&season="+season+"&order="+(kind.equals("episode") ? "episode" : "newest");
        load(title, params, result -> {
            JSONArray items = result.optJSONArray("items");
            if (items == null || items.length() > 40) { error("Página de catálogo no válida."); return; }
            List<String> labels = new ArrayList<>(); List<Runnable> actions = new ArrayList<>();
            List<Card> cards = new ArrayList<>();
            Runnable current = () -> page(kind, series, title, query, previous, parent, season);
            labels.add("Buscar" + (query.isEmpty() ? "" : ": " + query));
            actions.add(() -> host.search(query, text -> page(kind, series, title, text.trim(), new ArrayList<>(), parent, season), current));
            JSONArray seasons = result.optJSONArray("seasons");
            if (kind.equals("episode") && seasons != null && seasons.length() > 0) {
                labels.add(seasonLabel(season));
                actions.add(() -> {
                    List<String> choices = new ArrayList<>();
                    List<Runnable> select = new ArrayList<>();
                    choices.add(seasonLabel(0));
                    select.add(() -> page(kind, series, title, query, new ArrayList<>(), parent, 0));
                    for (int index = 0; index < seasons.length(); index++) {
                        JSONObject row = seasons.optJSONObject(index);
                        if (row == null) continue;
                        final int number = row.optInt("number");
                        if (number < 1 || number > 10000) continue;
                        choices.add(seasonLabel(number) + " · " + row.optInt("episodes") + " episodios");
                        select.add(() -> page(kind, series, title, query, new ArrayList<>(), parent, number));
                    }
                    host.show(title, "Selecciona una temporada", choices, select, current);
                });
            }
            JSONObject synopses = result.optJSONObject("synopses");
            for (int i=0; i<items.length(); i++) {
                JSONObject row = items.optJSONObject(i);
                if (row == null || row.optString("id").isEmpty()) continue;
                String name = row.optString("title", "Sin título");
                if (kind.equals("episode")) name = "T"+row.optInt("season")+" · E"+row.optInt("episode")+" — "+name;
                int actionIndex = actions.size();
                final String itemId = row.optString("id");
                final String poster = "revision="+enc(revision)+"&kind="+kind+"&id="+enc(itemId);
                if (kind.equals("series")) {
                    actions.add(() -> page("episode", itemId, row.optString("title", "Serie"), "", new ArrayList<>(), current));
                } else if (playbackEnabled) {
                    final String itemTitle = name;
                    actions.add(() -> host.play(itemId, itemTitle, kind, poster));
                } else {
                    final String itemTitle = name;
                    JSONObject synopsis = synopses == null ? null : synopses.optJSONObject(itemId);
                    final String text = (kind.equals("episode") ? title+"\n" : "")
                            + row.optString("air_date") + " · " + row.optLong("duration_seconds")/60 + " min\n\n"
                            + (synopsis == null ? "Sin sinopsis disponible." : synopsis.optString("text", "Sin sinopsis disponible."))
                            + "\n\nSolo consulta. Reproducción aún no habilitada.";
                    actions.add(() -> host.show(itemTitle, text, Arrays.asList("Volver al catálogo"), Arrays.asList(current), current));
                }
                Runnable itemAction = actions.remove(actionIndex);
                JSONObject summary = synopses == null ? null : synopses.optJSONObject(itemId);
                cards.add(new Card(name, summary == null ? "" : summary.optString("text"), poster, itemAction));
            }
            String next = result.optString("next");
            if (!next.isEmpty() && !next.equals(after) && previous.size() < 1000) {
                List<String> forward = new ArrayList<>(previous); forward.add(next);
                labels.add("Página siguiente"); actions.add(() -> page(kind, series, title, query, forward, parent, season));
            }
            if (!previous.isEmpty()) {
                List<String> prior = new ArrayList<>(previous); prior.remove(prior.size()-1);
                labels.add("Página anterior"); actions.add(() -> page(kind, series, title, query, prior, parent, season));
            }
            host.cards("HBO Max · " + title, (kind.equals("episode") ? seasonLabel(season) + " · " : "") + "Página " + (previous.size()+1) + " · " + items.length()
                    + " elementos" + (playbackEnabled ? "" : " · Solo consulta"), cards, labels, actions, parent);
        }, parent);
    }
}
