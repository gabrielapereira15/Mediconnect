package com.example.mediconnect_android.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.adapter.CarouselAdapter;
import com.example.mediconnect_android.adapter.DoctorHomeScreenAdapter;
import com.example.mediconnect_android.client.AppointmentClient;
import com.example.mediconnect_android.client.AppointmentMock;
import com.example.mediconnect_android.client.DoctorClient;
import com.example.mediconnect_android.client.DoctorClientImpl;
import com.example.mediconnect_android.databinding.FragmentHomeBinding;
import com.example.mediconnect_android.model.Doctor;
import com.example.mediconnect_android.util.FragmentUtils;
import com.example.mediconnect_android.util.KeyboardUtils;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements View.OnClickListener {

    private final Handler carouselHandler = new Handler(Looper.getMainLooper());
    List<Doctor> doctorList = new ArrayList<Doctor>();
    List<Integer> imageList = new ArrayList<>();
    DoctorHomeScreenAdapter mAdapter;
    CarouselAdapter carouselAdapter;
    DoctorClient doctorClient;
    private FragmentHomeBinding binding;
    private Runnable carouselRunnable;

    public HomeFragment() {
        doctorClient = new DoctorClientImpl();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment and initialize ViewBinding
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        init();
        return view;
    }

    private void init() {
        imageList.add(R.drawable.adv1);
        imageList.add(R.drawable.adv2);
        imageList.add(R.drawable.adv3);

        doctorList = doctorClient.getDoctors();

        binding.seeAllCategories.setOnClickListener(this);
        binding.seeAllDoctors.setOnClickListener(this);
        binding.psychiatristIcon.setOnClickListener(this);
        binding.dentistIcon.setOnClickListener(this);
        binding.dermatologistIcon.setOnClickListener(this);
        binding.obgynIcon.setOnClickListener(this);

        bindAdapter();
        bindCarouselAdapter();
        setupKeyboardDismiss();
    }

    private void bindCarouselAdapter() {
        // Initialize the CarouselAdapter with the imageList
        carouselAdapter = new CarouselAdapter(imageList);

        // Set the adapter to ViewPager2
        binding.carouselViewPager.setAdapter(carouselAdapter);

        // Optionally set a page transformer for visual effects (optional)
        binding.carouselViewPager.setPageTransformer((page, position) -> {
            // Customize your page transformer here (e.g., for animations)
            page.setAlpha(1 - Math.abs(position));
        });
        setupAutoScroll();
    }

    private void setupAutoScroll() {
        int delayMillis = 5000;

        carouselRunnable = new Runnable() {
            @Override
            public void run() {
                int currentItem = binding.carouselViewPager.getCurrentItem();
                int nextItem = (currentItem + 1) % imageList.size();
                binding.carouselViewPager.setCurrentItem(nextItem, true);
                carouselHandler.postDelayed(this, delayMillis);
            }
        };

        carouselHandler.postDelayed(carouselRunnable, delayMillis);

        binding.carouselViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                carouselHandler.removeCallbacks(carouselRunnable);
                carouselHandler.postDelayed(carouselRunnable, delayMillis);
            }
        });
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.see_all_categories) {
            FragmentUtils.loadFragment(requireActivity().getSupportFragmentManager(), R.id.flFragment, new SpecialtiesFragment());
        }
        if (view.getId() == R.id.see_all_doctors || view.getId() == R.id.dermatologist_icon
        || view.getId() == R.id.dentist_icon || view.getId() == R.id.obgyn_icon || view.getId() == R.id.psychiatrist_icon) {
            FragmentUtils.loadFragment(requireActivity().getSupportFragmentManager(), R.id.flFragment, new DoctorsFragment());
        }
    }

    private void bindAdapter() {
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.recyclerViewDoctors.setLayoutManager(layoutManager);
        mAdapter = new DoctorHomeScreenAdapter(doctorList, getContext());
        binding.recyclerViewDoctors.setAdapter(mAdapter);
    }

    private void setupKeyboardDismiss() {
        EditText editText = binding.searchBar;
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH) {
                    KeyboardUtils.hideKeyboard(v, getContext());
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        carouselHandler.removeCallbacks(carouselRunnable);
        binding = null;
    }
}