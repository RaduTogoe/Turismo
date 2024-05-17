// Generated by view binder compiler. Do not edit!
package com.example.turismo.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.turismo.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class FragmentGroupBinding implements ViewBinding {
  @NonNull
  private final ConstraintLayout rootView;

  @NonNull
  public final TextView emptyGroupText;

  @NonNull
  public final FloatingActionButton floatingActionButton;

  @NonNull
  public final RecyclerView groupsRecyclerView;

  @NonNull
  public final ConstraintLayout main;

  private FragmentGroupBinding(@NonNull ConstraintLayout rootView, @NonNull TextView emptyGroupText,
      @NonNull FloatingActionButton floatingActionButton, @NonNull RecyclerView groupsRecyclerView,
      @NonNull ConstraintLayout main) {
    this.rootView = rootView;
    this.emptyGroupText = emptyGroupText;
    this.floatingActionButton = floatingActionButton;
    this.groupsRecyclerView = groupsRecyclerView;
    this.main = main;
  }

  @Override
  @NonNull
  public ConstraintLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static FragmentGroupBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static FragmentGroupBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.fragment_group, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static FragmentGroupBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.emptyGroupText;
      TextView emptyGroupText = ViewBindings.findChildViewById(rootView, id);
      if (emptyGroupText == null) {
        break missingId;
      }

      id = R.id.floatingActionButton;
      FloatingActionButton floatingActionButton = ViewBindings.findChildViewById(rootView, id);
      if (floatingActionButton == null) {
        break missingId;
      }

      id = R.id.groupsRecyclerView;
      RecyclerView groupsRecyclerView = ViewBindings.findChildViewById(rootView, id);
      if (groupsRecyclerView == null) {
        break missingId;
      }

      ConstraintLayout main = (ConstraintLayout) rootView;

      return new FragmentGroupBinding((ConstraintLayout) rootView, emptyGroupText,
          floatingActionButton, groupsRecyclerView, main);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
