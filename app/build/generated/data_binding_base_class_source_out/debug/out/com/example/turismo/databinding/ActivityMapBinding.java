// Generated by view binder compiler. Do not edit!
package com.example.turismo.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.turismo.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ActivityMapBinding implements ViewBinding {
  @NonNull
  private final ConstraintLayout rootView;

  @NonNull
  public final BottomNavigationView bottomNavigationView;

  @NonNull
  public final ConstraintLayout main;

  @NonNull
  public final FrameLayout mainFrame;

  private ActivityMapBinding(@NonNull ConstraintLayout rootView,
      @NonNull BottomNavigationView bottomNavigationView, @NonNull ConstraintLayout main,
      @NonNull FrameLayout mainFrame) {
    this.rootView = rootView;
    this.bottomNavigationView = bottomNavigationView;
    this.main = main;
    this.mainFrame = mainFrame;
  }

  @Override
  @NonNull
  public ConstraintLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ActivityMapBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ActivityMapBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.activity_map, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ActivityMapBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.bottomNavigationView;
      BottomNavigationView bottomNavigationView = ViewBindings.findChildViewById(rootView, id);
      if (bottomNavigationView == null) {
        break missingId;
      }

      ConstraintLayout main = (ConstraintLayout) rootView;

      id = R.id.main_frame;
      FrameLayout mainFrame = ViewBindings.findChildViewById(rootView, id);
      if (mainFrame == null) {
        break missingId;
      }

      return new ActivityMapBinding((ConstraintLayout) rootView, bottomNavigationView, main,
          mainFrame);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
