package com.cd00827.OSSimulator;

import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * Listener preventing a StringProperty from being set to anything other than an integer
 * @author cd00827
 */
public class IntChecker implements ChangeListener<String> {
    private final StringProperty sp;

    /**
     * Constructor
     * @param sp StringProperty to monitor
     */
    public IntChecker(StringProperty sp) {
        super();
        this.sp = sp;
    }

    /**
     * Resets the value of sp to its previous value if the new value is not an integer
     * @param observable ObservableValue
     * @param oldValue Old value
     * @param newValue New value
     */
    @Override
    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        if (!newValue.matches("\\d*")) {
            this.sp.setValue(oldValue);
        }
    }
}