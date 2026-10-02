package com.sharveshinfra.rfiphotoproof;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChainageEngine {
    private static final double EARTH_R = 6371000.0;

    public static class Result {
        public final String packageCode;
        public final String packageName;
        public final double chainageMeters;
        public final double distanceFromAlignmentMeters;

        Result(String packageCode, String packageName, double chainageMeters, double distanceFromAlignmentMeters) {
            this.packageCode = packageCode;
            this.packageName = packageName;
            this.chainageMeters = chainageMeters;
            this.distanceFromAlignmentMeters = distanceFromAlignmentMeters;
        }

        public String formattedChainage() {
            int rounded = (int) Math.round(chainageMeters);
            return String.format(Locale.US, "%d+%03d", rounded / 1000, Math.abs(rounded % 1000));
        }
    }

    private static class Point {
        final double ch, lat, lon;
        Point(double ch, double lat, double lon) { this.ch = ch; this.lat = lat; this.lon = lon; }
    }

    private static class Alignment {
        final String code, name;
        final List<Point> points;
        Alignment(String code, String name, List<Point> points) {
            this.code = code; this.name = name; this.points = points;
        }
    }

    private final List<Alignment> alignments = new ArrayList<>();

    public ChainageEngine(Context context) {
        alignments.add(new Alignment("L&T",
                "Krishnagiri to Thumbipadi (Km 94+000–180+000)",
                loadCsv(context, "chainage_lnt.csv")));
        alignments.add(new Alignment("MVR",
                "Thumbipadi to Namakkal (Km 180+000–248+625)",
                loadCsv(context, "chainage_mvr.csv")));
    }

    private List<Point> loadCsv(Context context, String assetName) {
        List<Point> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets().open(assetName)))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; }
                String[] p = line.split(",");
                if (p.length < 3) continue;
                out.add(new Point(
                        Double.parseDouble(p[0]),
                        Double.parseDouble(p[1]),
                        Double.parseDouble(p[2])));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    public Result detect(double lat, double lon) {
        Result best = null;
        for (Alignment a : alignments) {
            Result r = detectOnAlignment(a, lat, lon);
            if (r != null && (best == null ||
                    r.distanceFromAlignmentMeters < best.distanceFromAlignmentMeters)) {
                best = r;
            }
        }
        return best;
    }

    private Result detectOnAlignment(Alignment a, double lat, double lon) {
        if (a.points.size() < 2) return null;
        double bestDist = Double.MAX_VALUE;
        double bestCh = a.points.get(0).ch;
        double cosLat = Math.cos(Math.toRadians(lat));

        for (int i = 0; i < a.points.size() - 1; i++) {
            Point p1 = a.points.get(i);
            Point p2 = a.points.get(i + 1);

            double x1 = Math.toRadians(p1.lon - lon) * EARTH_R * cosLat;
            double y1 = Math.toRadians(p1.lat - lat) * EARTH_R;
            double x2 = Math.toRadians(p2.lon - lon) * EARTH_R * cosLat;
            double y2 = Math.toRadians(p2.lat - lat) * EARTH_R;

            double dx = x2 - x1;
            double dy = y2 - y1;
            double len2 = dx * dx + dy * dy;
            double t = len2 <= 0.0001 ? 0.0 : -(x1 * dx + y1 * dy) / len2;
            if (t < 0) t = 0;
            else if (t > 1) t = 1;

            double px = x1 + t * dx;
            double py = y1 + t * dy;
            double dist = Math.sqrt(px * px + py * py);

            if (dist < bestDist) {
                bestDist = dist;
                bestCh = p1.ch + t * (p2.ch - p1.ch);
            }
        }
        return new Result(a.code, a.name, bestCh, bestDist);
    }
}
