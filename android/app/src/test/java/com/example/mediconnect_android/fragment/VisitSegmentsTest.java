package com.example.mediconnect_android.fragment;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import com.example.mediconnect_android.R;

import org.junit.Test;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

/**
 * Regression tests for the Visits tab crashing whenever Android rebuilt it:
 * after a rotation, a font-size or dark-mode change, or the process being
 * killed while the tab was open.
 *
 * The FragmentManager rebuilds a fragment by reflection, through a public
 * constructor that takes nothing. The three segments only had one that took
 * the visit list, so the rebuild threw "could not find Fragment constructor"
 * before the screen could draw. These check the same thing it looks for,
 * without building the fragments, which needs a device.
 */
public class VisitSegmentsTest {

    private static final List<Class<?>> REBUILT_BY_ANDROID = Arrays.asList(
            MedicalHistoryFragment.class,
            UpcomingFragment.class,
            CompletedFragment.class,
            CancelledFragment.class,
            // Built by ViewModelProvider's default factory, which has the
            // same need.
            VisitsViewModel.class);

    @Test
    public void everyPartOfVisitsCanBeRebuiltByTheSystem() {
        for (Class<?> type : REBUILT_BY_ANDROID) {
            // A public constructor on a class that is not public is still out
            // of the FragmentManager's reach.
            if (!Modifier.isPublic(type.getModifiers())) {
                fail(type.getSimpleName() + " has to be public");
            }
            try {
                // Finds public constructors only, as the FragmentManager does.
                type.getConstructor();
            } catch (NoSuchMethodException e) {
                fail(type.getSimpleName() + " needs a public constructor that takes nothing");
            }
        }
    }

    @Test
    public void eachButtonOpensItsOwnSegment() {
        assertSame(UpcomingFragment.class, MedicalHistoryFragment.segmentFor(R.id.filter_upcoming));
        assertSame(CompletedFragment.class, MedicalHistoryFragment.segmentFor(R.id.filter_past));
        assertSame(CancelledFragment.class, MedicalHistoryFragment.segmentFor(R.id.filter_cancelled));
    }

    @Test
    public void noButtonCheckedFallsBackToUpcoming() {
        // What getCheckedButtonId() returns when nothing is checked (View.NO_ID).
        assertSame(UpcomingFragment.class, MedicalHistoryFragment.segmentFor(-1));
    }
}
