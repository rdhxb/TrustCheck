package rdhxb.TrustCheck.krsPdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KrsPdfReader {
    // ------------------------------------------------------------------ model

    /** Ustaw na true, żeby zobaczyć, które linie parser uznał za wnętrze sekcji. */
    public static boolean DEBUG = false;

    // ------------------------------------------------------------------ model

    public record OsobaOrganu(
            int lp,
            String organ,            // np. "Organ uprawniony do reprezentacji podmiotu"
            String nazwisko,
            String imiona,
            String pesel,
            String funkcja,
            String wpisWprowadzajacy,
            String wpisWykreslajacy
    ) {
        public boolean aktualny() {
            return wpisWykreslajacy == null;
        }

        @Override
        public String toString() {
            return "%-18s %-12s %-12s %-42s %s".formatted(
                    nazwisko,
                    imiona == null ? "-" : imiona,
                    pesel == null ? "-" : pesel,
                    funkcja == null ? "-" : funkcja,
                    aktualny() ? "AKTUALNY" : "wykreślony (wpis " + wpisWykreslajacy + ")");
        }
    }

    // --------------------------------------------------------------- wzorce

    /** Dowolny nagłówek strukturalny - kończy bieżącą sekcję. */
    private static final Pattern NAGLOWEK =
            Pattern.compile("^(Dział|Rubryka|Podrubryka)\\b.*");

    /**
     * Tytuł rubryki. PDFBox gubi myślnik separujący ("Rubryka 2 Organ nadzoru"),
     * więc jest opcjonalny - inaczej wzorzec nigdy nie trafia.
     */
    private static final Pattern TYTUL_RUBRYKI =
            Pattern.compile("^Rubryka\\s+\\d+\\s*[-–—]?\\s*(\\p{L}.*?)\\s*$");

    /**
     * Wejście w sekcję z osobami. Wzorzec celowo luźny - PDFBox potrafi
     * inaczej oddać znaki diakrytyczne, więc nie opieramy się na "ą"/"ó".
     */
    private static final Pattern START_SEKCJI =
            Pattern.compile("Dane os.b wchodz");

    /** Początek bloku osoby: "  2 1.Nazwisko / Nazwa lub Firma 1 7 KOŁPA" */
    private static final Pattern POCZATEK_OSOBY =
            Pattern.compile("^\\s*(\\d+)\\s+1\\.\\s*Nazwisko");

    /** Pole rubryki: [lp] N.<etykieta> <wprow> <wykr> <wartość> */
    private static final Pattern POLE =
            Pattern.compile("^\\s*(?:\\d+\\s+)?(\\d)\\.\\s*(.+?)\\s+(\\d+|-)\\s+(\\d+|-)\\s*(.*)$");

    private static final Pattern STOPKA =
            Pattern.compile("^\\s*Strona\\s+\\d+\\s+z\\s+\\d+\\s*$");

    private static final Pattern NAGLOWEK_TABELI =
            Pattern.compile("^\\s*L\\.?\\s*p\\.?\\s+Numer i nazwa pola.*");

    private static final Pattern PESEL = Pattern.compile("\\b(\\d{11})\\b");

    // --------------------------------------------------------------- parsing

    public static List<OsobaOrganu> parsuj(String tekst) {
        List<OsobaOrganu> wynik = new ArrayList<>();

        String tytulRubryki = "";
        String organSekcji = null;
        boolean wSekcji = false;

        int lp = 0;
        String nazwisko = null, imiona = null, pesel = null, funkcja = null;
        String wprow = null, wykr = null;
        int ostatniePole = -1;
        boolean maOsobe = false;

        for (String rawLine : tekst.split("\\R")) {
            String linia = wyczysc(rawLine);

            if (linia.isEmpty()
                    || STOPKA.matcher(linia).matches()
                    || NAGLOWEK_TABELI.matcher(linia).matches()) {
                continue;
            }

            if (DEBUG) System.out.println((wSekcji ? "[S] " : "    ") + linia);

            // --- nawigacja po strukturze dokumentu -----------------------
            Matcher tytul = TYTUL_RUBRYKI.matcher(linia);
            if (tytul.matches()) {
                tytulRubryki = tytul.group(1);
            }

            // UWAGA: start sekcji sprawdzamy PRZED nagłówkiem, bo bywa
            // sklejony w jedną linię: "Podrubryka 1 Dane osób wchodzących...".
            // Odwrotna kolejność powoduje, że sekcja nigdy się nie włącza.
            if (START_SEKCJI.matcher(linia).find()) {
                if (maOsobe) {
                    wynik.add(zbuduj(lp, organSekcji, nazwisko, imiona, pesel, funkcja, wprow, wykr));
                    maOsobe = false;
                }
                wSekcji = true;
                organSekcji = tytulRubryki;
                ostatniePole = -1;
                continue;
            }

            if (NAGLOWEK.matcher(linia).matches()) {
                if (maOsobe) {
                    wynik.add(zbuduj(lp, organSekcji, nazwisko, imiona, pesel, funkcja, wprow, wykr));
                    maOsobe = false;
                }
                wSekcji = false;
                ostatniePole = -1;
                continue;
            }

            if (!wSekcji) continue;

            // --- nowa osoba ----------------------------------------------
            Matcher start = POCZATEK_OSOBY.matcher(linia);
            if (start.find()) {
                if (maOsobe) {
                    wynik.add(zbuduj(lp, organSekcji, nazwisko, imiona, pesel, funkcja, wprow, wykr));
                }
                lp = Integer.parseInt(start.group(1));
                nazwisko = imiona = pesel = funkcja = null;
                wprow = wykr = null;
                maOsobe = true;
            }

            // --- pola ------------------------------------------------------
            Matcher pole = POLE.matcher(linia);
            if (pole.matches()) {
                int nr = Integer.parseInt(pole.group(1));
                String wartosc = wyczysc(pole.group(5));
                ostatniePole = nr;

                switch (nr) {
                    case 1 -> {
                        nazwisko = wartosc;
                        wprow = normalizujNumer(pole.group(3));
                        wykr = normalizujNumer(pole.group(4));
                    }
                    case 2 -> imiona = wartosc;
                    case 3 -> {
                        Matcher m = PESEL.matcher(wartosc);
                        pesel = m.find() ? m.group(1) : null;
                    }
                    case 5 -> funkcja = wartosc;
                    default -> { /* 4, 6, 7 - nieinteresujące */ }
                }
                continue;
            }

            // --- kontynuacja zawiniętej wartości ---------------------------
            // Wartości w KRS są WERSALIKAMI, etykiety pisane normalnie
            // ("urodzenia", "reprezentującym") - to wystarczy do rozróżnienia.
            if (maOsobe && jestWartoscia(linia)) {
                switch (ostatniePole) {
                    case 1 -> nazwisko = polacz(nazwisko, linia);
                    case 2 -> imiona = polacz(imiona, linia);
                    case 5 -> funkcja = polacz(funkcja, linia);
                    default -> { }
                }
            }
        }

        if (maOsobe) {
            wynik.add(zbuduj(lp, organSekcji, nazwisko, imiona, pesel, funkcja, wprow, wykr));
        }
        return wynik;
    }

    /** Tylko organ reprezentacji - pomija radę nadzorczą i prokurentów. */
    public static List<OsobaOrganu> tylkoReprezentacja(List<OsobaOrganu> osoby) {
        List<OsobaOrganu> wynik = osoby.stream()
                .filter(o -> o.organ() != null
                        && o.organ().toLowerCase().contains("reprezentacji"))
                .toList();

        // Cicha pustka po filtrze prawie zawsze znaczy, że nie rozpoznano
        // tytułu rubryki - a nie że w KRS nikogo nie ma.
        if (wynik.isEmpty() && !osoby.isEmpty()) {
            System.err.println("Uwaga: sparsowano " + osoby.size()
                    + " osób, ale żadna nie ma organu pasującego do filtru.");
            osoby.stream()
                    .map(OsobaOrganu::organ)
                    .distinct()
                    .forEach(o -> System.err.println("  rozpoznany organ: \"" + o + "\""));
        }
        return wynik;
    }

    // -------------------------------------------------------------- pomocnicze

    private static OsobaOrganu zbuduj(int lp, String organ, String nazwisko, String imiona,
                                      String pesel, String funkcja, String wprow, String wykr) {
        return new OsobaOrganu(lp, organ, nazwisko, imiona, pesel, funkcja, wprow, wykr);
    }

    /** Usuwa miękkie łączniki (U+00AD), placeholdery "------" i nadmiarowe spacje. */
    private static String wyczysc(String s) {
        if (s == null) return "";
        return s.replace('\u00AD', ' ')
                .replaceAll("-{3,}", " ")
                .replaceAll(",\\s*$", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String normalizujNumer(String s) {
        return "-".equals(s) ? null : s;
    }

    private static boolean jestWartoscia(String linia) {
        String litery = linia.replaceAll("[^\\p{L}]", "");
        return !litery.isEmpty() && litery.equals(litery.toUpperCase());
    }

    private static String polacz(String a, String b) {
        return a == null ? b : a + " " + b;
    }

    // -------------------------------------------------------------------- main

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Użycie: ZarzadParser <ścieżka-do-pdf>");
            System.exit(1);
        }
        Path pdf = Path.of(args[0]);
        if (!Files.isReadable(pdf)) {
            System.err.println("Nie mogę odczytać pliku: " + pdf.toAbsolutePath());
            System.exit(1);
        }

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            List<OsobaOrganu> zarzad = tylkoReprezentacja(parsuj(stripper.getText(doc)));

            System.out.println("=== Aktualny skład zarządu ===");
            zarzad.stream().filter(OsobaOrganu::aktualny).forEach(System.out::println);

            System.out.println("\n=== Wykreśleni ===");
            zarzad.stream().filter(o -> !o.aktualny()).forEach(System.out::println);
        }
    }

}
