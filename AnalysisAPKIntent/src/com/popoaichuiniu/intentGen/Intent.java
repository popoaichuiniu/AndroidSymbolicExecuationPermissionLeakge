package com.popoaichuiniu.intentGen;

import org.javatuples.Quartet;
import org.javatuples.Triplet;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class Intent {
    public Set<Quartet<String,String,String,String>> extras = new LinkedHashSet<Quartet<String,String,String,String>>();
    public String action;
    public String targetComponent;
    public Set<String> categories = new LinkedHashSet<String>();

    public Intent(Intent intent) {
        if (intent.extras != null) {
            this.extras = new LinkedHashSet<>(intent.extras);
        } else {
            this.extras = null;
        }

        if (intent.action != null) {
            this.action = new String(intent.action);
        } else {
            this.action = null;
        }

        if (intent.targetComponent != null) {
            this.targetComponent = new String(intent.targetComponent);
        }
        else {
            this.targetComponent = null;
        }

        if (this.categories != null) {
            this.categories = new LinkedHashSet<>(intent.categories);
        }
        else {
            this.categories = null;
        }
    }

    public Intent() {
        super();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Intent intent = (Intent) o;

        if (extras != null ? !extras.equals(intent.extras) : intent.extras != null) return false;
        if (action != null ? !action.equals(intent.action) : intent.action != null) return false;
        if (targetComponent != null ? !targetComponent.equals(intent.targetComponent) : intent.targetComponent != null)
            return false;
        return categories != null ? categories.equals(intent.categories) : intent.categories == null;

    }

    @Override
    public int hashCode() {
        int result = extras != null ? extras.hashCode() : 0;
        result = 31 * result + (action != null ? action.hashCode() : 0);
        result = 31 * result + (targetComponent != null ? targetComponent.hashCode() : 0);
        result = 31 * result + (categories != null ? categories.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Intent{" +
                "extras=" + extras +
                ", action='" + action + '\'' +
                ", targetComponent='" + targetComponent + '\'' +
                ", categories=" + categories +
                '}';
    }
}
class IntentExtra {
    String key;
    String type;
    String value;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntentExtra that = (IntentExtra) o;
        return Objects.equals(key, that.key) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {

        return Objects.hash(key, type);
    }

    public IntentExtra(String key, String type, String value) {
        this.key = key;
        this.type = type;
        this.value = value;
    }
}
