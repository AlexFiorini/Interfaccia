package com.mycompany.interfaccia;

import javax.swing.*;
import java.awt.*;

class ContactListRenderer extends DefaultListCellRenderer {
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof Contact contact) {
            setText(contact.name() + " <" + contact.email() + ">");
        }
        return this;
    }
}