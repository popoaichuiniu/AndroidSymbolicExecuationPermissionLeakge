package com.popoaichuiniu.intentGen;

import org.javatuples.Quartet;

import java.util.HashSet;
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

        if (!equivExtraTo(extras,intent.extras)) return false;
        if (action != null ? !action.equals(intent.action) : intent.action != null) return false;
        if (targetComponent != null ? !targetComponent.equals(intent.targetComponent) : intent.targetComponent != null)
            return false;
        return equivCategoryTo(categories,intent.categories);

    }

    public boolean equivExtraTo(Set<Quartet<String,String,String,String>> set1, Set<Quartet<String,String,String,String>> set2)
    {
        if(set1==null&&set2!=null || set1!=null&&set2==null)
        {
            return false;
        }

        if(set1==null&&set2==null)
        {
            return true;
        }

        Set<IntentExtraValue> setIntentExtraValue1=new HashSet<>();
        Set<IntentExtraValue> setIntentExtraValue2=new HashSet<>();

        for(Quartet<String,String,String,String> quartet1:set1)
        {
            if(quartet1.getValue0().equals("IntentKey"))
            {
                setIntentExtraValue1.add(new IntentExtraValue(quartet1.getValue2(),quartet1.getValue1(),quartet1.getValue3()));
            }

        }
        for(Quartet<String,String,String,String> quartet2:set2)
        {
            if(quartet2.getValue0().equals("IntentKey"))
            {
                setIntentExtraValue2.add(new IntentExtraValue(quartet2.getValue2(),quartet2.getValue1(),quartet2.getValue3()));
            }

        }

        return setIntentExtraValue1.containsAll(setIntentExtraValue2)&&setIntentExtraValue2.containsAll(setIntentExtraValue1);


    }

    public boolean equivCategoryTo(Set<String> cateSet1,Set<String> cateSet2)
    {
        if(cateSet1==null&&cateSet2!=null ||cateSet1!=null&&cateSet2==null)
        {
            return false;
        }

        if(cateSet1==null&&cateSet2==null)
        {
            return true;
        }

        return cateSet1.containsAll(cateSet2)&&cateSet2.containsAll(cateSet1);

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
class IntentExtraKey {
    String key;
    String type;
    String value;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntentExtraKey that = (IntentExtraKey) o;
        return Objects.equals(key, that.key) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {

        return Objects.hash(key, type);
    }

    public IntentExtraKey(String key, String type, String value) {
        this.key = key;
        this.type = type;
        this.value = value;
    }
}

class IntentExtraValue {
    String key;
    String type;
    String value;

    public IntentExtraValue(String key, String type, String value) {
        this.key = key;
        this.type = type;
        this.value = value;
    }

    public IntentExtraValue(IntentExtraKey intentExtraKey)
    {
        this.key=intentExtraKey.key;
        this.type=intentExtraKey.type;
        this.value=intentExtraKey.value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntentExtraValue that = (IntentExtraValue) o;
        return Objects.equals(key, that.key) &&
                Objects.equals(type, that.type) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {

        return Objects.hash(key, type, value);
    }
}
