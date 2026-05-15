package com.example.unihubworkshop.features.workshop.presentation.view;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unihubworkshop.R;
import com.example.unihubworkshop.features.workshop.data.local.entity.RegistrationEntity;

import java.util.ArrayList;
import java.util.List;

public class RegistrationAdapter extends RecyclerView.Adapter<RegistrationAdapter.ViewHolder> {

    private List<RegistrationEntity> registrations = new ArrayList<>();

    public void setRegistrations(List<RegistrationEntity> registrations) {
        this.registrations = registrations;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_registered_student, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RegistrationEntity registration = registrations.get(position);
        holder.tvStudentName.setText(registration.studentName != null ? registration.studentName : "MSSV: " + registration.studentId);
        holder.tvStatus.setText(registration.status);
        
        if ("CHECKED_IN".equalsIgnoreCase(registration.status)) {
            if (registration.isOfflineOnly) {
                holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.orange_sync));
                holder.tvStatus.setText("SYNCING");
            } else {
                holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.green_live));
                holder.tvStatus.setText("CHECKED-IN");
            }
        } else {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.gray_disabled));
            holder.tvStatus.setText("NOT CHECKED-IN");
        }
    }

    @Override
    public int getItemCount() {
        return registrations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName;
        TextView tvStatus;

        ViewHolder(View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
