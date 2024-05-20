package com.example.aqpf.fragments;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Map;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<Map<String, Object>> selectedData = new MutableLiveData<>();

    public void selectData(Map<String, Object> data) {
        selectedData.setValue(data);
    }

    public LiveData<Map<String, Object>> getSelectedData() {
        return selectedData;
    }
}