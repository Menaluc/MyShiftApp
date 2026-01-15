package com.example.myshiftapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class ManagerHomeFragment extends Fragment {

    private static final String ARG_FULL_NAME = "arg_full_name";

    public static ManagerHomeFragment newInstance(String fullName) {
        ManagerHomeFragment f = new ManagerHomeFragment();
        Bundle b = new Bundle();
        b.putString(ARG_FULL_NAME, fullName);
        f.setArguments(b);
        return f;
    }

    public ManagerHomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manager_home, container, false);

        String fullName = (getArguments() == null) ? "" : getArguments().getString(ARG_FULL_NAME, "");

        TextView tvWelcome = view.findViewById(R.id.tvWelcome);
        tvWelcome.setText("Welcome " + fullName);

        return view;
    }
}
