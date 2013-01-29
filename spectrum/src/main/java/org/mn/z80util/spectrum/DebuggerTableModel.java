package org.mn.z80util.spectrum;

import java.util.*;
import javax.swing.table.*;

import org.mn.z80util.disassembler.*;

public class DebuggerTableModel extends AbstractTableModel {

    private LinkedList<DisasmResult> commandListing;

    public void setCommandListing(LinkedList<DisasmResult> commandListing) {
        this.commandListing = commandListing;
    }

    public LinkedList<DisasmResult> getCommandListing() {
        return commandListing;
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public int getRowCount() {
        return commandListing.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        DisasmResult dar = commandListing.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return Hex.intToHex4(dar.getStartAddr());
            case 1:
                return dar.getHexDigits();
            case 2:
                return dar.getCommand();
            default:
                return null;
        }
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "Address";
            case 1:
                return "Hex digits";
            case 2:
                return "Command";
            default:
                return null;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
}
