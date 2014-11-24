package juicebox.encode;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableStringConverter;
import java.util.ArrayList;
import java.util.List;

/**
 * //wgEncodeBroadHistoneGm12878H3k4me1StdSig.bigWig
 * // size=346M;
 * // dateSubmitted=2009-01-05;
 * // dataType=ChipSeq;
 * // cell=GM12878;
 * // antibody=H3K4me1;
 * // control=std;
 * // expId=33;
 * // setType=exp;
 * // controlId=GM12878/Input/std;
 * // subId=2804;
 * // dataVersion=ENCODE Jan 2011 Freeze;
 * // dateResubmitted=2010-11-05;
 * // grant=Bernstein;
 * // lab=Broad;
 * // view=Signal;
 * // type=bigWig;
 * // dccAccession=wgEncodeEH000033;
 * // origAssembly=hg18
 *
 * @author jrobinso
 *         Date: 10/31/13
 *         Time: 10:09 PM
 */
public class EncodeTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 7743873079853677860L;
    private String[] columnHeadings;
    private List<EncodeFileRecord> records;
    private final TableRowSorter<EncodeTableModel> sorter;

    public EncodeTableModel(String [] headings, List<EncodeFileRecord> records) {

        this.records = records;

        List<String> tmp = new ArrayList<String>();
        tmp.add("");  // Checkbox heading
        for(String h : headings) {
            String heading = h.trim();
            if(heading.length() > 0 && !"path".equals(heading)) {
                tmp.add(heading);
            }
        }
        //tmp.add("path");
        columnHeadings = tmp.toArray(new String[tmp.size()]);


        sorter = new TableRowSorter<EncodeTableModel>(this);

        sorter.setStringConverter(new TableStringConverter() {
            @Override
            public String toString(TableModel model, int row, int column) {
                final Object value = model.getValueAt(row, column);
                return value == null ? "" : value.toString();
            }
        });
    }

    public TableRowSorter<EncodeTableModel> getSorter() {
        return sorter;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 0 ? Boolean.class : String.class;
    }

    @Override
    public String getColumnName(int column) {
        return columnHeadings[column];
    }

    @Override
    public int getRowCount() {
        return records.size();
    }

    @Override
    public int getColumnCount() {
        return columnHeadings.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {

        if (rowIndex >= records.size() || columnIndex >= columnHeadings.length) {
            return null;
        }

        EncodeFileRecord record = records.get(rowIndex);
        if (columnIndex == 0) {
            return record.isSelected();
        } else {
            String att = columnHeadings[columnIndex];
            return record.getAttributeValue(att);
        }

    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0;
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        if(col == 0) {
            records.get(row).setSelected((Boolean) value);
        }
        fireTableCellUpdated(row, col);
    }

    public List<EncodeFileRecord> getRecords() {
        return records;
    }
}