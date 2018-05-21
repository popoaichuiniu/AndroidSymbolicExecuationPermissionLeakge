package com.popoaichuiniu.intentGen;

import soot.toolkits.scalar.ArraySparseSet;

public class MyArraySparseSet<T> extends ArraySparseSet<T> {

    @Override
    public boolean contains(Object obj) {//同一个方法中相同字符串值表示相同value


        for (int i = 0; i < numElements; i++)
        {

            if (elements[i].toString().equals(obj.toString())) {
                return true;
            }
        }


        return false;

    }

    @Override
    public void remove(Object obj) {
        for (int i = 0; i < numElements; i++)
            if (elements[i].toString().equals(obj.toString())) {
                remove(i);
                break;
            }
    }
}
