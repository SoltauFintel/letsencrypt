package httpsmanager.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;

public class StringTable {
    public String separator = " | ";
    private final List<List<String>> rows = new ArrayList<>();
    private int maxColumns = 0;

    public void row(String... values) {
        List<String> row = new ArrayList<>();
        for (String value : values) {
            row.add(value);
        }
        rows.add(row);
        if (row.size() > maxColumns) {
            maxColumns = row.size();
        }
    }

    @Override
    public String toString() {
        String ret = "";
        // für jede column: max breite bestimmen
        Map<String, Integer> widths = new HashMap<>();
        for (List<String> row : rows) {
            for (int col = 0; col < maxColumns; col++) {
                String v = "";
                if (col < row.size()) {
                    v = row.get(col);
                }
                String key = "c" + col;
                Integer width = widths.get(key);
                if (width == null || v.length() > width) {
                    widths.put(key, Integer.valueOf(v.length()));
                }
            }
        }
        
        // Ausgabe
        for (List<String> row : rows) {
            if (!ret.isEmpty()) {
                ret += "\n";
            }
            String rowtext = "";
            for (int col = 0; col < maxColumns; col++) {
                if (!rowtext.isEmpty()) {
                    rowtext += separator;
                }
                String v = "";
                if (col < row.size()) {
                    v = row.get(col);
                }
                Integer width = widths.get("c" + col);
                if (width == null) {
                    rowtext += v;
                } else {
                    rowtext += Strings.padEnd(v, width.intValue(), ' ');
                }
            }
            ret += rowtext;
        }
        return ret;
    }
}
