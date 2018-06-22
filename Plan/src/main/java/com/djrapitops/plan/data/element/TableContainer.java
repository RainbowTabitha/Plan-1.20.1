/*
 * Licence is provided in the jar as license.yml also here:
 * https://github.com/Rsl1122/Plan-PlayerAnalytics/blob/master/Plan/src/main/resources/license.yml
 */
package com.djrapitops.plan.data.element;

import com.djrapitops.plan.data.store.mutators.formatting.Formatter;
import com.djrapitops.plan.utilities.FormatUtils;
import com.djrapitops.plan.utilities.html.Html;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Container used for parsing Html tables.
 *
 * @author Rsl1122
 */
public class TableContainer {

    protected final String[] header;
    protected final Formatter[] formatters;
    private List<Serializable[]> values;

    private boolean jqueryDatatable;

    private String color;

    /**
     * Constructor, call with super(...).
     *
     * @param header Required: example {@code new TableContainer("1st", "2nd"} parses into {@code <thead><tr><th>1st</th><th>2nd</th></tr></thead}.
     */
    public TableContainer(String... header) {
        this.header = header;
        this.formatters = new Formatter[this.header.length];
        values = new ArrayList<>();
    }

    public TableContainer(boolean players, String... header) {
        this.header = FormatUtils.mergeArrays(new String[]{Html.FONT_AWESOME_ICON.parse("user") + " Player"}, header);
        this.formatters = new Formatter[this.header.length];
        values = new ArrayList<>();
    }

    public final void addRow(Serializable... values) {
        this.values.add(values);
    }

    public final String parseHtml() {
        return getTableHeader() +
                parseHeader() +
                parseBody() +
                "</table>" + (jqueryDatatable ? "</div>" : "");
    }

    public final String parseBody() {
        StringBuilder body = new StringBuilder();

        if (values.isEmpty()) {
            addRow("No Data");
        }
        for (Serializable[] row : values) {

            int maxIndex = row.length - 1;
            body.append("<tr>");
            for (int i = 0; i < header.length; i++) {
                try {
                    if (i > maxIndex) {
                        body.append("<td>-");
                    } else {
                        Serializable value = row[i];
                        Formatter formatter = formatters[i];
                        body.append("<td").append(formatter != null ? " data-order=\"" + value + "\">" : ">");
                        body.append(formatter != null ? formatter.apply(value) : value);
                    }
                    body.append("</td>");
                } catch (ClassCastException | ArrayIndexOutOfBoundsException e) {
                    throw new IllegalStateException("Invalid formatter given at index " + i + ": " + e.getMessage(), e);
                }
            }
            body.append("</tr>");

        }

        return Html.TABLE_BODY.parse(body.toString());

    }

    public final void setColor(String color) {
        this.color = color;
    }

    public final String parseHeader() {
        StringBuilder header = new StringBuilder("<thead" + (color != null ? " class=\"bg-" + color + "\"" : "") + "><tr>");
        for (String title : this.header) {
            header.append("<th>").append(title).append("</th>");
        }
        header.append("</tr></thead>");
        return header.toString();
    }

    public final void setFormatter(int index, Formatter formatter) {
        if (index < formatters.length) {
            formatters[index] = formatter;
        }
    }

    /**
     * Make use of JQuery Datatables plugin.
     * <p>
     * If this is called, result of {@code parseHtml()} should be wrapped with {@code Html.PANEL.parse(Html.PANEL_BODY.parse(result))}
     */
    public void useJqueryDataTables() {
        this.jqueryDatatable = true;
    }

    private String getTableHeader() {
        if (jqueryDatatable) {
            return "<div class=\"table-responsive\">" + Html.TABLE_JQUERY.parse();
        } else {
            return Html.TABLE_SCROLL.parse();
        }
    }
}