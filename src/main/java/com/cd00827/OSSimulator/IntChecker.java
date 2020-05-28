package com.cd00827.OSSimulator;

import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class IntChecker implements ChangeListener<String> {
    private final StringProperty sp;

    public IntChecker(StringProperty sp) {
        super();
        this.sp = sp;
    }

    @Override
    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        if (!newValue.matches("\\d*")) {
            this.sp.setValue(oldValue);
        }
    }
}