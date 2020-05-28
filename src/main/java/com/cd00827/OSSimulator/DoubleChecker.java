package com.cd00827.OSSimulator;

import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class DoubleChecker implements ChangeListener<String> {
    private final StringProperty sp;

    public DoubleChecker(StringProperty sp) {
        super();
        this.sp = sp;
    }

    @Override
    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        if (!newValue.matches("\\d*([.]\\d*)?")) {
            this.sp.setValue(oldValue);
        }
    }
}
