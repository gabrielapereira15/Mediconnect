package com.example.mediconnect_android.fragment;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.mediconnect_android.model.Appointment;

import java.util.List;

/**
 * The visit list that the Visits screen and its three segments share.
 *
 * The segments used to be handed the list through their constructors.
 * Android rebuilds fragments by reflection after a rotation, a font-size or
 * dark-mode change, or the process being killed, and it can only call a
 * constructor that takes nothing, so the tab crashed whenever any of those
 * happened while it was open. Tapping Past or Cancelled before the list had
 * arrived crashed it too, because there was no list to hand over yet.
 *
 * The list lives here instead, owned by MedicalHistoryFragment, and each
 * segment watches it. A segment that is rebuilt, or opened early, starts
 * with whatever is here and fills in when the parent's load lands.
 *
 * Null means "not loaded yet", which the parent is already showing as its
 * loading or error state. That is not the same as an empty list, which the
 * segments show as "nothing here".
 */
public class VisitsViewModel extends ViewModel {

    private final MutableLiveData<List<Appointment>> appointments = new MutableLiveData<>();

    /** The shared list, as seen from one of the segments inside Visits. */
    static VisitsViewModel forSegment(Fragment segment) {
        return new ViewModelProvider(segment.requireParentFragment()).get(VisitsViewModel.class);
    }

    public LiveData<List<Appointment>> getAppointments() {
        return appointments;
    }

    /** Called by MedicalHistoryFragment, on the main thread, when a load lands. */
    void publish(@Nullable List<Appointment> loaded) {
        appointments.setValue(loaded);
    }
}
