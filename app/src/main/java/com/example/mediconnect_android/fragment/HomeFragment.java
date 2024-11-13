package com.example.mediconnect_android.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.adapter.CarouselAdapter;
import com.example.mediconnect_android.adapter.DoctorAdapter;
import com.example.mediconnect_android.client.AppointmentClient;
import com.example.mediconnect_android.client.AppointmentMock;
import com.example.mediconnect_android.databinding.FragmentHomeBinding;
import com.example.mediconnect_android.model.Doctor;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements View.OnClickListener {

    private FragmentHomeBinding binding;
    List<Doctor> doctorList = new ArrayList<Doctor>();
    List<Integer> imageList = new ArrayList<>();
    DoctorAdapter mAdapter;
    CarouselAdapter carouselAdapter;
    private Handler carouselHandler = new Handler(Looper.getMainLooper());
    private Runnable carouselRunnable;
    AppointmentClient appointmentClient;

    public HomeFragment() {
        appointmentClient = new AppointmentMock();
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
        imageList.add(R.drawable.mediconnect_logo);
        imageList.add(R.drawable.doctorimage);

        doctorList.addAll(appointmentClient.getDoctors());

        bindAdapter();
        bindCarouselAdapter();
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
    public void onClick(View view) {}

    private void bindAdapter() {
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.recyclerViewDoctors.setLayoutManager(layoutManager);
        mAdapter = new DoctorAdapter(doctorList, getContext());
        binding.recyclerViewDoctors.setAdapter(mAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        carouselHandler.removeCallbacks(carouselRunnable);
        binding = null;
    }
}