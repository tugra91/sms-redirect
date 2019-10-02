package com.sms.tcmuer.smsredirect;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


public class MyWakefulReceiver extends WakefulBroadcastReceiver {


    private static final String TAG = "SMSBroadcastReceiver";


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Geldi geldi");

        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs;
        String strMessage = "";

        Object[] pdus = (Object[]) bundle.get("pdus");

        if (pdus != null) {

            msgs = new SmsMessage[pdus.length];

            for (int i = 0; i < msgs.length; i++) {
                msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                strMessage += "\"" + msgs[i].getMessageBody() + "\"" + "\n";



                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                
                Thread mailThread = new Thread(new SendMail(msgs[i].getDisplayOriginatingAddress(), strMessage, context));
                mailThread.start();


                Log.i(TAG, strMessage);



            }
        }
    }



    private class SendMail implements  Runnable {


        private String smsSender;
        private String smsMessage;
        private Context context;

        public SendMail(String smsSender, String smsMessage, Context context) {
            this.smsSender = smsSender;
            this.smsMessage = smsMessage;
            this.context = context;
        }


        public void run() {
            sendMail();
        }

        private void sendMail() {

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            final String username = "fromMail@gmail.com";
            final String password = "sifre";

            Properties prop = new Properties();
            prop.put("mail.smtp.host", "smtp.gmail.com");
            //prop.put("mail.smtp.port", "587");
            prop.put("mail.smtp.connectiontimeout", "10000");
            prop.put("mail.smtp.timeout", "10000");
            prop.put("mail.smtp.port", "465");
            prop.put("mail.smtp.auth", "true");
            prop.put("mail.smtp.socketFactory.port", "465");
            prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            //prop.put("mail.smtp.starttls.enable", "true"); //TLS

            Session session = Session.getInstance(prop,
                    new javax.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username, password);
                        }
                    });

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("SMS Yönlendirme")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);




            try {

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress("fromamail@mail.com","SMS Yönlendirme"));
                message.setRecipients(
                        Message.RecipientType.TO,
                        InternetAddress.parse("toMail@mail.com")
                );
                message.setSubject(smsSender + " SMS Geldi");
                message.setText("Sevgili Tuğra " + smsSender + " ' dan SMS geldi "
                        + "\n\n " + smsMessage);


                Transport.send(message);

                smsMessage += "Mail olarak mesaj Gönderildi " + smsMessage;

                builder.setContentText(smsMessage)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(smsMessage));



            } catch (Exception e) {
                e.printStackTrace();
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage("Redirect SMS Number",null,smsMessage,null,null);
                smsMessage += "Mail Hata Aldı SMS olarak mesaj Gönderildi " + smsMessage;
                builder.setContentText(smsMessage)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(smsMessage));
            } finally {
                Date now = new Date();
                int id = Integer.parseInt(new SimpleDateFormat("ddHHmmss",  Locale.US).format(now));

                notificationManager.notify(id, builder.build());
            }
        }
    }




}
