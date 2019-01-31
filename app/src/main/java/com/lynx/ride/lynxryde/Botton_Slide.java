package com.lynx.ride.lynxryde;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class Botton_Slide extends BottomSheetDialogFragment {
    public  TextView textView;
    Button request;
    ProgressBar progressBar;
    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        View content= View.inflate(getContext(),R.layout.pop_up_window,null);
        textView =content.findViewById(R.id.Distance_popUp);
        progressBar=content.findViewById(R.id.progressBar);
        dialog.setContentView(content);


        progressBar.setVisibility(View.VISIBLE);
       /* MapsActivity mapsActivity=new MapsActivity();
        String distance=mapsActivity.run_distance();*/

        //textView.setText("hello");
       // progressBar.setVisibility(View.GONE);

    }

}
