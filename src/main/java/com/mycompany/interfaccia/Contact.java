package com.mycompany.interfaccia;

import java.io.Serializable;

record Contact(String name, String email) implements Serializable {

    @Override
    public String toString() {
        return name + " <" + email + ">";
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}