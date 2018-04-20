package br.ufrj.caronae.acts;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;

import br.ufrj.caronae.R;
import br.ufrj.caronae.SharedPref;
import br.ufrj.caronae.Util;
import br.ufrj.caronae.frags.FalaeFrag;
import br.ufrj.caronae.httpapis.CaronaeAPI;
import br.ufrj.caronae.models.modelsforjson.FalaeMsgForJson;
import br.ufrj.caronae.models.modelsforjson.RideForJson;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FalaeAct extends AppCompatActivity {

    @BindView(R.id.send_bt)
    TextView send_bt;
    @BindView(R.id.activity_back)
    TextView title;

    FalaeFrag frag;

    public String message;
    public String subject;

    private RideForJson rideOffer;
    private String from, user2;

    boolean requested, fromAnother;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_falae);
        ButterKnife.bind(this);
        boolean fromProfile;
        fromProfile = getIntent().getBooleanExtra("fromProfile", false);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        Fragment fragment = null;
        Class fragmentClass;
        fragmentClass = FalaeFrag.class;
        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.flContent, fragment).commit();
        frag = (FalaeFrag) fragment;
        if(fromProfile)
        {
            String driverName;
            title.setText(getResources().getString(R.string.back));
            user2 = getIntent().getExtras().getString("user");
            from = getIntent().getExtras().getString("from");
            fromAnother = getIntent().getBooleanExtra("fromAnother", false);
            requested = getIntent().getBooleanExtra("requested", false);
            rideOffer = getIntent().getExtras().getParcelable("ride");
            driverName = getIntent().getStringExtra("driver");
            ((FalaeFrag)fragment).reason_txt = getResources().getString(R.string.frag_falae_report_rb);
            ((FalaeFrag)fragment).subject_txt = "Denúncia sobre usuário " + driverName;
        }
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        if(title.getText().equals("Menu")) {
            backToMenu();
        }
        else
        {
            backToProfile();
        }
    }

    @OnClick(R.id.back_bt)
    public void backTouch()
    {
        if(title.getText().equals("Menu")) {
            backToMenu();
        }
        else
        {
            backToProfile();
        }

    }

    private void backToMenu()
    {
        finish();
        Intent mainAct = new Intent(this, MainAct.class);
        SharedPref.NAV_INDICATOR = "Menu";
        startActivity(mainAct);
        this.overridePendingTransition(R.anim.anim_left_slide_in,R.anim.anim_right_slide_out);
    }

    private void backToProfile()
    {
        finish();
        Intent profileAct = new Intent(this, ProfileAct.class);
        profileAct.putExtra("user", user2);
        profileAct.putExtra("from", from);
        profileAct.putExtra("fromAnother", true);
        profileAct.putExtra("requested", requested);
        profileAct.putExtra("ride", rideOffer);
        startActivity(profileAct);
        this.overridePendingTransition(R.anim.anim_left_slide_in,R.anim.anim_right_slide_out);
    }

    @OnClick(R.id.send_bt)
    public void sendBt()
    {
        message = frag.getMessage();
        subject = frag.getSubject();
        if (message.isEmpty()) {
            final android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(FalaeAct.this);
            builder.setCancelable(false);
            builder.setTitle("Ops!");
            builder.setMessage("Parece que você esqueceu de preencher sua mensagem.");
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            android.support.v7.app.AlertDialog dialog = builder.create();
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            WindowManager.LayoutParams wmlp = dialog.getWindow().getAttributes();
            wmlp.gravity = Gravity.CENTER;
            dialog.show();
            return;
        }
        else {
            message = message
                    + "\n\n--------------------------------\n"
                    + "Device: " + android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.RELEASE + ")\n"
                    + "Versão do app: " + Util.getAppVersionName(this);
        }
        final ProgressDialog pd = ProgressDialog.show(FalaeAct.this, "", getString(R.string.wait), true, true);
        CaronaeAPI.service(getBaseContext()).falaeSendMessage(new FalaeMsgForJson(subject, message))
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            pd.dismiss();
                            final android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(FalaeAct.this);
                            builder.setCancelable(false);
                            builder.setTitle("Mensagem enviada!");
                            builder.setMessage("Obrigado por nos mandar uma mensagem. Nossa equipe irá entrar em contato em breve.");
                            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    SharedPref.NAV_INDICATOR = "Menu";
                                    backToMenu();
                                }
                            });
                            android.support.v7.app.AlertDialog dialog = builder.create();
                            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                            WindowManager.LayoutParams wmlp = dialog.getWindow().getAttributes();
                            wmlp.gravity = Gravity.CENTER;
                            dialog.show();
                        } else {
                            Util.treatResponseFromServer(response);
                            pd.dismiss();
                            final android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(FalaeAct.this);
                            builder.setCancelable(false);
                            builder.setTitle("Mensagem não enviada");
                            builder.setMessage("Ocorreu um erro enviando sua mensagem. Verifique sua conexão e tente novamente.");
                            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            });
                            android.support.v7.app.AlertDialog dialog = builder.create();
                            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                            WindowManager.LayoutParams wmlp = dialog.getWindow().getAttributes();
                            wmlp.gravity = Gravity.CENTER;
                            dialog.show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        pd.dismiss();
                        final android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(FalaeAct.this);
                        builder.setCancelable(false);
                        builder.setTitle("Mensagem não enviada");
                        builder.setMessage("Ocorreu um erro enviando sua mensagem. Verifique sua conexão e tente novamente.");
                        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        });
                        android.support.v7.app.AlertDialog dialog = builder.create();
                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        WindowManager.LayoutParams wmlp = dialog.getWindow().getAttributes();
                        wmlp.gravity = Gravity.CENTER;
                        dialog.show();
                    }
                });
    }
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }
}
