package com.popoaichuiniu.intentGen;


import java.util.*;


public class Intent {

    public Set<IntentExtraKey> myExtras=new HashSet<>();
    public String action;
    public String targetComponent;
    public Set<String> categories = new LinkedHashSet<String>();

    public Intent(Intent intent) {
        if (intent.myExtras != null) {
            this.myExtras = new HashSet<>(intent.myExtras);
        } else {
            this.myExtras = null;
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

        if (!equivExtraTo(myExtras,intent.myExtras)) return false;
        if (action != null ? !action.equals(intent.action) : intent.action != null) return false;
        if (targetComponent != null ? !targetComponent.equals(intent.targetComponent) : intent.targetComponent != null)
            return false;
        return equivCategoryTo(categories,intent.categories);

    }

    public boolean equivExtraTo(Set<IntentExtraKey> set1, Set<IntentExtraKey> set2)
    {
        if(set1==null&&set2!=null || set1!=null&&set2==null)
        {
            return false;
        }

        if(set1==null&&set2==null)
        {
            return true;
        }


        if(set1.size()!=set2.size())
        {
            return false;
        }

       Set<IntentExtraValue> sum=new HashSet<>();

        for(IntentExtraKey intentExtraKey1:set1)
        {
            sum.add(new IntentExtraValue(intentExtraKey1));
        }
        for(IntentExtraKey intentExtraKey2:set2)
        {
            sum.add(new IntentExtraValue(intentExtraKey2));
        }

        if(sum.size()!=set1.size())
        {
            return false;
        }
        else
        {
            return true;
        }






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

        if(cateSet1.size()!=cateSet2.size())
        {
            return false;
        }

        return cateSet1.containsAll(cateSet2);

    }

    @Override
    public int hashCode() {
        int result = myExtras != null ? myExtras.hashCode() : 0;
        result = 31 * result + (action != null ? action.hashCode() : 0);
        result = 31 * result + (targetComponent != null ? targetComponent.hashCode() : 0);
        result = 31 * result + (categories != null ? categories.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Intent{" +
                "extras=" + myExtras +
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

    public  IntentExtraKey(IntentExtraValue intentExtraValue){

        this.key=intentExtraValue.key;
        this.type=intentExtraValue.type;
        this.value=intentExtraValue.value;

    }
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

    @Override
    public String toString() {
        return  "{"+"key='" + key + '\'' +
                ", type='" + type + '\'' +
                ", value='" + value + '\''+"}";
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
