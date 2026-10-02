package com.sharveshinfra.rfiphotoproof;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.location.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity implements LocationListener {
  static final int PERMS=10,CAM=11;
  EditText project,rfi,activity,from,to; Spinner side; RadioGroup stage; TextView gps,ch; ImageView preview; Button share;
  LocationManager lm; Location loc; ChainageEngine eng; ChainageEngine.Result det; Uri pending,last; String ts,file;

  @Override public void onCreate(Bundle b){super.onCreate(b); eng=new ChainageEngine(this); build(); lm=(LocationManager)getSystemService(LOCATION_SERVICE); ask();}
  TextView tv(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
  EditText ed(String h,String v){EditText e=new EditText(this);e.setHint(h);e.setText(v);e.setSingleLine(true);return e;}
  Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
  int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
  LinearLayout.LayoutParams lp(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(5),0,dp(5));return p;}
  void build(){
    ScrollView sc=new ScrollView(this); LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(18),dp(16),dp(30));sc.addView(root);
    TextView title=tv("SHARVESH FIELD – RFI PHOTO PROOF",21,true);title.setTextColor(Color.rgb(15,54,93));root.addView(title);
    root.addView(tv("NH-44 Maintenance • GPS Chainage • Before / During / After",13,false));
    project=ed("Project / Package","NH-44 Annual Maintenance");rfi=ed("RFI No.","");activity=ed("Activity / BOQ Item","");from=ed("Work From Chainage","");to=ed("Work To Chainage","");
    root.addView(project,lp());root.addView(rfi,lp());root.addView(activity,lp());root.addView(from,lp());root.addView(to,lp());
    side=new Spinner(this);side.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Select Side / Location","LHS","RHS","MEDIAN","MCW","SERVICE ROAD","BOTH SIDES"}));root.addView(side,lp());
    root.addView(tv("Photo Stage",14,true)); stage=new RadioGroup(this);stage.setOrientation(RadioGroup.HORIZONTAL);for(String s:new String[]{"BEFORE","DURING","AFTER"}){RadioButton r=new RadioButton(this);r.setText(s);stage.addView(r);}((RadioButton)stage.getChildAt(0)).setChecked(true);root.addView(stage,lp());
    ch=tv("Current chainage: —",18,true);ch.setTextColor(Color.rgb(15,54,93));gps=tv("Waiting for GPS…",13,false);root.addView(ch,lp());root.addView(gps,lp());
    Button ref=btn("REFRESH GPS");ref.setOnClickListener(v->startGps());root.addView(ref,lp());
    Button cap=btn("CAPTURE & STAMP PHOTO");cap.setTextSize(17);cap.setOnClickListener(v->capture());root.addView(cap,lp());
    preview=new ImageView(this);preview.setAdjustViewBounds(true);preview.setVisibility(View.GONE);root.addView(preview,lp());
    share=btn("SHARE PHOTO");share.setVisibility(View.GONE);share.setOnClickListener(v->share());root.addView(share,lp());setContentView(sc);
  }
  void ask(){if(Build.VERSION.SDK_INT>=23&&(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED||checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)){requestPermissions(new String[]{Manifest.permission.CAMERA,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},PERMS);}else startGps();}
  @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==PERMS)startGps();}
  void startGps(){if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){gps.setText("Location permission required for automatic chainage.");return;}try{Location a=lm.getLastKnownLocation(LocationManager.GPS_PROVIDER),b=lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);loc=a==null?b:(b==null?a:(a.getTime()>b.getTime()?a:b));update();if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER))lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,1500,1,this);if(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,2500,5,this);}catch(Exception e){gps.setText("GPS error: "+e.getMessage());}}
  @Override public void onLocationChanged(Location l){loc=l;update();} @Override public void onProviderEnabled(String p){} @Override public void onProviderDisabled(String p){} @Override public void onStatusChanged(String p,int s,Bundle b){}
  void update(){if(loc==null){gps.setText("Waiting for GPS fix…");return;}det=eng.detect(loc.getLatitude(),loc.getLongitude());gps.setText(String.format(Locale.US,"GPS %.6f, %.6f • Accuracy ±%.0f m",loc.getLatitude(),loc.getLongitude(),loc.getAccuracy()));if(det!=null){ch.setText(String.format(Locale.US,"%s • Km %s • Offset %.1f m",det.packageCode,det.formattedChainage(),det.distanceFromAlignmentMeters));if(val(from).isEmpty()&&det.distanceFromAlignmentMeters<250)from.setText(det.formattedChainage());}}
  String val(EditText e){return e.getText().toString().trim();} String st(){RadioButton r=findViewById(stage.getCheckedRadioButtonId());return r==null?"BEFORE":r.getText().toString();}
  String safe(String s){return s.replaceAll("[^A-Za-z0-9]+","-").replaceAll("^-|-$","");}
  void capture(){if(val(rfi).isEmpty()){rfi.setError("Enter RFI No.");return;}if(val(activity).isEmpty()){activity.setError("Enter activity");return;}if(side.getSelectedItemPosition()==0){Toast.makeText(this,"Select side/location",Toast.LENGTH_LONG).show();return;}if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){ask();return;}ts=new SimpleDateFormat("dd-MM-yyyy HH:mm:ss",Locale.US).format(new Date());file=safe(val(rfi))+"_"+st()+"_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date())+".jpg";ContentValues cv=new ContentValues();cv.put(MediaStore.Images.Media.DISPLAY_NAME,file);cv.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");if(Build.VERSION.SDK_INT>=29){cv.put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/Sharvesh Field RFI");cv.put(MediaStore.Images.Media.IS_PENDING,1);}pending=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,cv);if(pending==null){Toast.makeText(this,"Unable to create photo",Toast.LENGTH_LONG).show();return;}Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);i.putExtra(MediaStore.EXTRA_OUTPUT,pending);i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(i,CAM);}
  @Override protected void onActivityResult(int rq,int rc,Intent d){super.onActivityResult(rq,rc,d);if(rq!=CAM)return;if(rc==RESULT_OK&&pending!=null&&stamp(pending)){if(Build.VERSION.SDK_INT>=29){ContentValues cv=new ContentValues();cv.put(MediaStore.Images.Media.IS_PENDING,0);getContentResolver().update(pending,cv,null,null);}last=pending;preview.setImageURI(last);preview.setVisibility(View.VISIBLE);share.setVisibility(View.VISIBLE);Toast.makeText(this,"Photo saved with RFI proof stamp",Toast.LENGTH_LONG).show();}else if(pending!=null)try{getContentResolver().delete(pending,null,null);}catch(Exception e){}pending=null;}
  boolean stamp(Uri u){try{Bitmap src;try(InputStream in=getContentResolver().openInputStream(u)){src=BitmapFactory.decodeStream(in);}if(src==null)return false;Bitmap bm=src.copy(Bitmap.Config.ARGB_8888,true);Canvas c=new Canvas(bm);float sc=Math.max(1f,bm.getWidth()/1080f),m=18*sc,line=34*sc,h=8*line+20*sc,top=bm.getHeight()-h-m;Paint bg=new Paint();bg.setColor(Color.argb(195,0,0,0));c.drawRect(m,top,bm.getWidth()-m,bm.getHeight()-m,bg);Paint p=new Paint(1);p.setColor(Color.WHITE);p.setTextSize(24*sc);p.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));String[] a={"RFI: "+val(rfi)+"   |   "+st(),"Activity: "+val(activity),"Work CH: Km "+val(from)+(val(to).isEmpty()?"":" – "+val(to))+"   |   "+side.getSelectedItem(),"Current CH: "+(det==null?"—":"Km "+det.formattedChainage()),"Package: "+(det==null?"—":det.packageCode),"Date/Time: "+ts,"GPS: "+(loc==null?"—":String.format(Locale.US,"%.6f, %.6f",loc.getLatitude(),loc.getLongitude())),"Accuracy / Offset: "+(loc==null?"—":String.format(Locale.US,"±%.0f m",loc.getAccuracy()))+(det==null?"":String.format(Locale.US," / %.1f m",det.distanceFromAlignmentMeters))};float y=top+line;for(String s:a){c.drawText(s,m+15*sc,y,p);y+=line;}try(OutputStream out=getContentResolver().openOutputStream(u,"w")){bm.compress(Bitmap.CompressFormat.JPEG,94,out);}return true;}catch(Exception e){Toast.makeText(this,"Stamp error: "+e.getMessage(),Toast.LENGTH_LONG).show();return false;}}
  void share(){if(last==null)return;Intent i=new Intent(Intent.ACTION_SEND);i.setType("image/jpeg");i.putExtra(Intent.EXTRA_STREAM,last);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(i,"Share RFI proof"));}
}
